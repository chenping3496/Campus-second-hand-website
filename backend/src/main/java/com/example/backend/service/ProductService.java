package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.entity.*;
import com.example.backend.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private NotificationService notificationService;

    public PageResult<ProductDTO> getProductList(int page, int size, String sortBy, String sortDir) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, Product.ProductStatus.ON_SALE);
        applySort(wrapper, sortBy, sortDir);

        Page<Product> mpPage = productMapper.selectPage(new Page<>(page + 1, size), wrapper);
        return toPageResult(mpPage, page, size, null);
    }

    public PageResult<ProductDTO> searchProducts(String keyword, Long categoryId,
                                                  BigDecimal minPrice, BigDecimal maxPrice,
                                                  String productTag,
                                                  int page, int size, String sortBy, String sortDir) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, Product.ProductStatus.ON_SALE);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Product::getTitle, keyword).or().like(Product::getDescription, keyword));
        }
        if (categoryId != null) wrapper.eq(Product::getCategoryId, categoryId);
        if (minPrice != null) wrapper.ge(Product::getPrice, minPrice);
        if (maxPrice != null) wrapper.le(Product::getPrice, maxPrice);
        if (productTag != null && !productTag.isEmpty()) {
            wrapper.eq(Product::getProductTag, Product.ProductTag.valueOf(productTag));
        }
        applySort(wrapper, sortBy, sortDir);

        Page<Product> mpPage = productMapper.selectPage(new Page<>(page + 1, size), wrapper);
        return toPageResult(mpPage, page, size, null);
    }

    public Result<ProductDTO> getProductDetail(Long id, Long currentUserId) {
        Product product = productMapper.selectById(id);
        if (product == null || product.getDeleted()) {
            return Result.error("商品不存在");
        }

        product.setViewCount(product.getViewCount() + 1);
        productMapper.updateById(product);

        Category category = categoryMapper.selectById(product.getCategoryId());
        User seller = userMapper.selectById(product.getSellerId());
        ProductDTO dto = ProductDTO.fromEntity(product, category, seller);

        LambdaQueryWrapper<Favorite> favWrapper = new LambdaQueryWrapper<>();
        favWrapper.eq(Favorite::getProductId, id);
        dto.setFavoriteCount(favoriteMapper.selectCount(favWrapper));

        if (currentUserId != null) {
            favWrapper.eq(Favorite::getUserId, currentUserId);
            dto.setIsFavorited(favoriteMapper.selectCount(favWrapper) > 0);
        }

        return Result.success(dto);
    }

    @Transactional
    public Result<ProductDTO> createProduct(Long sellerId, ProductRequest request) {
        User seller = userMapper.selectById(sellerId);
        if (seller == null) {
            return Result.error("用户不存在");
        }

        Category category = categoryMapper.selectById(request.getCategoryId());
        if (category == null) {
            return Result.error("分类不存在");
        }

        Product product = new Product();
        product.setTitle(request.getTitle());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setCategoryId(category.getId());
        product.setSellerId(sellerId);
        product.setStatus(Product.ProductStatus.PENDING);
        if (request.getProductTag() != null && !request.getProductTag().isEmpty()) {
            try {
                product.setProductTag(Product.ProductTag.valueOf(request.getProductTag()));
            } catch (IllegalArgumentException e) {
                product.setProductTag(Product.ProductTag.NORMAL);
            }
        }
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            product.setImages(String.join(",", request.getImages()));
        }

        productMapper.insert(product);

        return Result.success(ProductDTO.fromEntity(product, category, seller));
    }

    @Transactional
    public Result<ProductDTO> updateProduct(Long productId, Long userId, ProductRequest request) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted()) {
            return Result.error("商品不存在");
        }

        if (!product.getSellerId().equals(userId)) {
            return Result.error("无权操作此商品");
        }

        if (request.getTitle() != null) product.setTitle(request.getTitle());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getOriginalPrice() != null) product.setOriginalPrice(request.getOriginalPrice());
        if (request.getCategoryId() != null) {
            Category category = categoryMapper.selectById(request.getCategoryId());
            if (category != null) {
                product.setCategoryId(category.getId());
            }
        }
        if (request.getImages() != null) {
            product.setImages(String.join(",", request.getImages()));
        }

        if (product.getStatus() == Product.ProductStatus.ON_SALE ||
            product.getStatus() == Product.ProductStatus.REJECTED) {
            product.setStatus(Product.ProductStatus.PENDING);
        }

        product.setRejectReason(null);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);

        Category category = categoryMapper.selectById(product.getCategoryId());
        User seller = userMapper.selectById(product.getSellerId());
        return Result.success(ProductDTO.fromEntity(product, category, seller));
    }

    @Transactional
    public Result<Void> deleteProduct(Long productId, Long userId) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted()) {
            return Result.error("商品不存在");
        }

        if (!product.getSellerId().equals(userId)) {
            return Result.error("无权操作此商品");
        }

        if (product.getStatus() == Product.ProductStatus.ON_SALE) {
            List<Order.OrderStatus> activeStatuses = Arrays.asList(
                    Order.OrderStatus.PENDING, Order.OrderStatus.SHIPPED);
            LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.eq(Order::getProductId, productId)
                    .in(Order::getStatus, activeStatuses);
            if (orderMapper.selectCount(orderWrapper) > 0) {
                return Result.error("存在进行中订单，暂不可删除");
            }
        }

        product.setDeleted(true);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);

        return Result.success();
    }

    @Transactional
    public Result<Void> offShelfProduct(Long productId, Long userId) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted()) {
            return Result.error("商品不存在");
        }

        if (!product.getSellerId().equals(userId)) {
            return Result.error("无权操作此商品");
        }

        if (product.getStatus() != Product.ProductStatus.ON_SALE) {
            return Result.error("商品状态不正确");
        }

        product.setStatus(Product.ProductStatus.OFF_SHELF);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);

        return Result.success();
    }

    @Transactional
    public Result<Void> relistProduct(Long productId, Long userId) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted()) {
            return Result.error("商品不存在");
        }

        if (!product.getSellerId().equals(userId)) {
            return Result.error("无权操作此商品");
        }

        if (product.getStatus() != Product.ProductStatus.OFF_SHELF) {
            return Result.error("商品状态不正确");
        }

        product.setStatus(Product.ProductStatus.PENDING);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);

        return Result.success();
    }

    public PageResult<ProductDTO> getMyProducts(Long userId, String status, int page, int size) {
        User seller = userMapper.selectById(userId);
        if (seller == null) {
            return PageResult.of(List.of(), 0, page, size);
        }

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getSellerId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Product::getStatus, Product.ProductStatus.valueOf(status));
        }
        wrapper.orderByDesc(Product::getCreatedAt);

        Page<Product> mpPage = productMapper.selectPage(new Page<>(page + 1, size), wrapper);
        return toPageResult(mpPage, page, size, userId);
    }

    @Transactional
    public Result<Void> approveProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted()) {
            return Result.error("商品不存在");
        }

        if (product.getStatus() != Product.ProductStatus.PENDING) {
            return Result.error("商品状态不正确");
        }

        product.setStatus(Product.ProductStatus.ON_SALE);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);

        notificationService.sendNotification(
                product.getSellerId(),
                "商品审核通过",
                "您的商品「" + product.getTitle() + "」已通过审核，现已上架",
                Notification.NotificationType.PRODUCT_APPROVED,
                product.getId()
        );

        return Result.success();
    }

    @Transactional
    public Result<Void> rejectProduct(Long productId, String reason) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted()) {
            return Result.error("商品不存在");
        }

        if (product.getStatus() != Product.ProductStatus.PENDING) {
            return Result.error("商品状态不正确");
        }

        product.setStatus(Product.ProductStatus.REJECTED);
        product.setRejectReason(reason);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);

        notificationService.sendNotification(
                product.getSellerId(),
                "商品审核拒绝",
                "您的商品「" + product.getTitle() + "」审核未通过，原因：" + reason,
                Notification.NotificationType.PRODUCT_REJECTED,
                product.getId()
        );

        return Result.success();
    }

    @Transactional
    public Result<Void> forceOffShelf(Long productId, String reason) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted()) {
            return Result.error("商品不存在");
        }

        if (product.getStatus() != Product.ProductStatus.ON_SALE) {
            return Result.error("商品状态不正确");
        }

        product.setStatus(Product.ProductStatus.OFF_SHELF);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);

        notificationService.sendNotification(
                product.getSellerId(),
                "商品被强制下架",
                "您的商品「" + product.getTitle() + "」已被管理员强制下架，原因：" + reason,
                Notification.NotificationType.PRODUCT_OFF_SHELF,
                product.getId()
        );

        return Result.success();
    }

    public PageResult<ProductDTO> getProductListForAdmin(String status, String keyword, int page, int size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Product::getStatus, Product.ProductStatus.valueOf(status));
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Product::getTitle, keyword);
        }
        wrapper.orderByDesc(Product::getCreatedAt);

        Page<Product> mpPage = productMapper.selectPage(new Page<>(page + 1, size), wrapper);
        return toPageResult(mpPage, page, size, null);
    }

    private void applySort(LambdaQueryWrapper<Product> wrapper, String sortBy, String sortDir) {
        boolean asc = "asc".equalsIgnoreCase(sortDir);
        if ("price".equals(sortBy)) {
            if (asc) wrapper.orderByAsc(Product::getPrice);
            else wrapper.orderByDesc(Product::getPrice);
        } else if ("viewCount".equals(sortBy)) {
            if (asc) wrapper.orderByAsc(Product::getViewCount);
            else wrapper.orderByDesc(Product::getViewCount);
        } else {
            if (asc) wrapper.orderByAsc(Product::getCreatedAt);
            else wrapper.orderByDesc(Product::getCreatedAt);
        }
    }

    private PageResult<ProductDTO> toPageResult(Page<Product> mpPage, int page, int size, Long currentUserId) {
        List<Product> products = mpPage.getRecords();
        if (products.isEmpty()) {
            return PageResult.of(List.of(), mpPage.getTotal(), page, size);
        }

        // Batch load categories
        Set<Long> categoryIds = products.stream().map(Product::getCategoryId).collect(Collectors.toSet());
        Map<Long, Category> categoryMap = categoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        // Batch load sellers
        Set<Long> sellerIds = products.stream().map(Product::getSellerId).collect(Collectors.toSet());
        Map<Long, User> sellerMap = userMapper.selectBatchIds(sellerIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<ProductDTO> list = products.stream().map(product -> {
            ProductDTO dto = ProductDTO.fromEntity(product,
                    categoryMap.get(product.getCategoryId()),
                    sellerMap.get(product.getSellerId()));

            // Favorite count
            LambdaQueryWrapper<Favorite> favWrapper = new LambdaQueryWrapper<>();
            favWrapper.eq(Favorite::getProductId, product.getId());
            dto.setFavoriteCount(favoriteMapper.selectCount(favWrapper));

            // Is favorited
            if (currentUserId != null) {
                favWrapper.eq(Favorite::getUserId, currentUserId);
                dto.setIsFavorited(favoriteMapper.selectCount(favWrapper) > 0);
            }
            return dto;
        }).toList();

        return PageResult.of(list, mpPage.getTotal(), page, size);
    }
}
