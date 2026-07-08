package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.entity.*;
import com.example.backend.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NotificationService notificationService;

    @Transactional
    public Result<OrderDTO> createOrder(Long buyerId, Long productId, String paymentMethod) {
        User buyer = userMapper.selectById(buyerId);
        if (buyer == null) {
            return Result.error("用户不存在");
        }

        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted()) {
            return Result.error("商品不存在");
        }

        if (product.getStatus() != Product.ProductStatus.ON_SALE) {
            return Result.error("商品不在售");
        }

        if (product.getSellerId().equals(buyerId)) {
            return Result.error("不能购买自己的商品");
        }

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setProductId(productId);
        order.setBuyerId(buyerId);
        order.setSellerId(product.getSellerId());
        order.setPrice(product.getPrice());
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentMethod(paymentMethod);
        order.setPaymentTime(LocalDateTime.now());

        orderMapper.insert(order);

        product.setStatus(Product.ProductStatus.SOLD);
        product.setUpdatedAt(LocalDateTime.now());
        productMapper.updateById(product);

        notificationService.sendNotification(
                product.getSellerId(),
                "新订单通知",
                "您的商品「" + product.getTitle() + "」已被购买，请尽快发货",
                Notification.NotificationType.ORDER_NEW,
                order.getId()
        );

        User seller = userMapper.selectById(product.getSellerId());
        return Result.success(OrderDTO.fromEntity(order, product, buyer, seller));
    }

    public PageResult<OrderDTO> getBuyerOrders(Long buyerId, String status, int page, int size) {
        User buyer = userMapper.selectById(buyerId);
        if (buyer == null) {
            return PageResult.of(List.of(), 0, page, size);
        }

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getBuyerId, buyerId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, Order.OrderStatus.valueOf(status));
        }
        wrapper.orderByDesc(Order::getCreatedAt);

        Page<Order> mpPage = orderMapper.selectPage(new Page<>(page + 1, size), wrapper);
        return toPageResult(mpPage, page, size);
    }

    public PageResult<OrderDTO> getSellerOrders(Long sellerId, String status, int page, int size) {
        User seller = userMapper.selectById(sellerId);
        if (seller == null) {
            return PageResult.of(List.of(), 0, page, size);
        }

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getSellerId, sellerId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, Order.OrderStatus.valueOf(status));
        }
        wrapper.orderByDesc(Order::getCreatedAt);

        Page<Order> mpPage = orderMapper.selectPage(new Page<>(page + 1, size), wrapper);
        return toPageResult(mpPage, page, size);
    }

    public Result<OrderDTO> getOrderDetail(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }

        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            return Result.error("无权查看此订单");
        }

        Product product = productMapper.selectById(order.getProductId());
        User buyer = userMapper.selectById(order.getBuyerId());
        User seller = userMapper.selectById(order.getSellerId());
        return Result.success(OrderDTO.fromEntity(order, product, buyer, seller));
    }

    @Transactional
    public Result<Void> shipOrder(Long orderId, Long sellerId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }

        if (!order.getSellerId().equals(sellerId)) {
            return Result.error("无权操作此订单");
        }

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            return Result.error("订单状态不正确");
        }

        order.setStatus(Order.OrderStatus.SHIPPED);
        order.setShipTime(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        Product product = productMapper.selectById(order.getProductId());
        String productTitle = product != null ? product.getTitle() : "未知商品";
        notificationService.sendNotification(
                order.getBuyerId(),
                "卖家已发货",
                "您购买的商品「" + productTitle + "」已发货，请注意查收",
                Notification.NotificationType.ORDER_SHIPPED,
                order.getId()
        );

        return Result.success();
    }

    @Transactional
    public Result<Void> confirmReceive(Long orderId, Long buyerId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }

        if (!order.getBuyerId().equals(buyerId)) {
            return Result.error("无权操作此订单");
        }

        if (order.getStatus() != Order.OrderStatus.SHIPPED) {
            return Result.error("订单状态不正确");
        }

        order.setStatus(Order.OrderStatus.COMPLETED);
        order.setCompleteTime(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        Product product = productMapper.selectById(order.getProductId());
        String productTitle = product != null ? product.getTitle() : "未知商品";
        notificationService.sendNotification(
                order.getSellerId(),
                "交易完成",
                "买家已确认收货，商品「" + productTitle + "」交易完成",
                Notification.NotificationType.ORDER_COMPLETED,
                order.getId()
        );

        return Result.success();
    }

    @Transactional
    public Result<Void> cancelOrder(Long orderId, Long userId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }

        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            return Result.error("无权操作此订单");
        }

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            return Result.error("只能取消待发货的订单");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        Product product = productMapper.selectById(order.getProductId());
        if (product != null) {
            product.setStatus(Product.ProductStatus.ON_SALE);
            product.setUpdatedAt(LocalDateTime.now());
            productMapper.updateById(product);
        }

        String productTitle = product != null ? product.getTitle() : "未知商品";
        Long notifyUserId = order.getBuyerId().equals(userId)
                ? order.getSellerId() : order.getBuyerId();
        notificationService.sendNotification(
                notifyUserId,
                "订单已取消",
                "商品「" + productTitle + "」的订单已被取消",
                Notification.NotificationType.ORDER_CANCELLED,
                order.getId()
        );

        return Result.success();
    }

    public PageResult<OrderDTO> getOrderListForAdmin(String status, String orderNo, int page, int size) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, Order.OrderStatus.valueOf(status));
        }
        if (orderNo != null && !orderNo.isEmpty()) {
            wrapper.like(Order::getOrderNo, orderNo);
        }
        wrapper.orderByDesc(Order::getCreatedAt);

        Page<Order> mpPage = orderMapper.selectPage(new Page<>(page + 1, size), wrapper);
        return toPageResult(mpPage, page, size);
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return timestamp + random;
    }

    private PageResult<OrderDTO> toPageResult(Page<Order> mpPage, int page, int size) {
        List<Order> orders = mpPage.getRecords();
        if (orders.isEmpty()) {
            return PageResult.of(List.of(), mpPage.getTotal(), page, size);
        }

        // Batch load products
        Set<Long> productIds = orders.stream().map(Order::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // Batch load users (both buyers and sellers)
        Set<Long> userIds = new HashSet<>();
        orders.forEach(o -> {
            userIds.add(o.getBuyerId());
            userIds.add(o.getSellerId());
        });
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<OrderDTO> list = orders.stream().map(order -> {
            Product product = productMap.get(order.getProductId());
            User buyer = userMap.get(order.getBuyerId());
            User seller = userMap.get(order.getSellerId());
            return OrderDTO.fromEntity(order, product, buyer, seller);
        }).toList();

        return PageResult.of(list, mpPage.getTotal(), page, size);
    }
}
