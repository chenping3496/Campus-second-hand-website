package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.entity.Favorite;
import com.example.backend.entity.Product;
import com.example.backend.entity.User;
import com.example.backend.mapper.FavoriteMapper;
import com.example.backend.mapper.ProductMapper;
import com.example.backend.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserMapper userMapper;

    @Transactional
    public Result<Void> addFavorite(Long userId, Long productId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        Product product = productMapper.selectById(productId);
        if (product == null || product.getDeleted()) {
            return Result.error("商品不存在");
        }

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getProductId, productId);
        if (favoriteMapper.selectCount(wrapper) > 0) {
            return Result.error("已收藏该商品");
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        favoriteMapper.insert(favorite);

        return Result.success();
    }

    @Transactional
    public Result<Void> removeFavorite(Long userId, Long productId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        Product product = productMapper.selectById(productId);
        if (product == null) {
            return Result.error("商品不存在");
        }

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getProductId, productId);
        favoriteMapper.delete(wrapper);

        return Result.success();
    }

    public PageResult<ProductDTO> getFavorites(Long userId, int page, int size) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return PageResult.of(List.of(), 0, page, size);
        }

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).orderByDesc(Favorite::getCreatedAt);
        Page<Favorite> mpPage = favoriteMapper.selectPage(new Page<>(page + 1, size), wrapper);

        List<ProductDTO> list = mpPage.getRecords().stream()
                .filter(f -> {
                    Product p = productMapper.selectById(f.getProductId());
                    return p != null && !p.getDeleted();
                })
                .map(f -> {
                    Product p = productMapper.selectById(f.getProductId());
                    ProductDTO dto = ProductDTO.fromEntity(p, null, null);
                    dto.setIsFavorited(true);
                    LambdaQueryWrapper<Favorite> countWrapper = new LambdaQueryWrapper<>();
                    countWrapper.eq(Favorite::getProductId, f.getProductId());
                    dto.setFavoriteCount(favoriteMapper.selectCount(countWrapper));
                    return dto;
                })
                .toList();

        return PageResult.of(list, mpPage.getTotal(), page, size);
    }

    public boolean isFavorited(Long userId, Long productId) {
        User user = userMapper.selectById(userId);
        Product product = productMapper.selectById(productId);
        if (user == null || product == null) {
            return false;
        }
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId).eq(Favorite::getProductId, productId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }
}
