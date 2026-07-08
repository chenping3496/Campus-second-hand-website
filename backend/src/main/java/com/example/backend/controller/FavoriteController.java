package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
@Tag(name = "收藏", description = "商品收藏相关接口")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @PostMapping("/{productId}")
    @Operation(summary = "添加收藏")
    public Result<Void> addFavorite(@PathVariable Long productId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return favoriteService.addFavorite(userId, productId);
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "取消收藏")
    public Result<Void> removeFavorite(@PathVariable Long productId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return favoriteService.removeFavorite(userId, productId);
    }

    @GetMapping
    @Operation(summary = "获取我的收藏列表")
    public Result<PageResult<ProductDTO>> getFavorites(
            HttpServletRequest request,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(favoriteService.getFavorites(userId, page, size));
    }

    @GetMapping("/check/{productId}")
    @Operation(summary = "检查是否已收藏")
    public Result<Boolean> checkFavorite(@PathVariable Long productId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(favoriteService.isFavorited(userId, productId));
    }
}
