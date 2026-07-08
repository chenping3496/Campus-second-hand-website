package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@Tag(name = "商品", description = "商品浏览、发布、管理接口")
public class ProductController {

    @GetMapping("/tags/periods")
    @Operation(summary = "获取商品标签开放时间段")
    public Result<Map<String, Object>> getTagPeriods() {
        return Result.success(Map.of(
            "URGENT_SCHOOL", Map.of("name", "开学急用", "startMonth", 8, "endMonth", 10, "active", isTagActive(8, 10)),
            "URGENT_GRADUATION", Map.of("name", "毕业急出", "startMonth", 5, "endMonth", 7, "active", isTagActive(5, 7))
        ));
    }

    private boolean isTagActive(int startMonth, int endMonth) {
        int currentMonth = java.time.LocalDate.now().getMonthValue();
        return currentMonth >= startMonth && currentMonth <= endMonth;
    }

    @Autowired
    private ProductService productService;

    @GetMapping("/list")
    @Operation(summary = "获取在售商品列表")
    public Result<PageResult<ProductDTO>> getProductList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDir) {
        return Result.success(productService.getProductList(page, size, sortBy, sortDir));
    }

    @GetMapping("/search")
    @Operation(summary = "搜索商品")
    public Result<PageResult<ProductDTO>> searchProducts(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "最低价格") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "最高价格") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "商品标签") @RequestParam(required = false) String productTag,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "desc") String sortDir) {
        return Result.success(productService.searchProducts(keyword, categoryId, minPrice, maxPrice, productTag, page, size, sortBy, sortDir));
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "获取商品详情")
    public Result<ProductDTO> getProductDetail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return productService.getProductDetail(id, userId);
    }

    @PostMapping
    @Operation(summary = "发布商品")
    public Result<ProductDTO> createProduct(HttpServletRequest request,
                                            @Valid @RequestBody ProductRequest productRequest) {
        Long userId = (Long) request.getAttribute("userId");
        return productService.createProduct(userId, productRequest);
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改商品信息")
    public Result<ProductDTO> updateProduct(@PathVariable Long id,
                                            HttpServletRequest request,
                                            @Valid @RequestBody ProductRequest productRequest) {
        Long userId = (Long) request.getAttribute("userId");
        return productService.updateProduct(id, userId, productRequest);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除商品")
    public Result<Void> deleteProduct(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return productService.deleteProduct(id, userId);
    }

    @PostMapping("/{id}/off-shelf")
    @Operation(summary = "下架商品")
    public Result<Void> offShelfProduct(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return productService.offShelfProduct(id, userId);
    }

    @PostMapping("/{id}/relist")
    @Operation(summary = "重新上架商品")
    public Result<Void> relistProduct(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return productService.relistProduct(id, userId);
    }

    @GetMapping("/my")
    @Operation(summary = "获取我发布的商品")
    public Result<PageResult<ProductDTO>> getMyProducts(
            HttpServletRequest request,
            @Parameter(description = "商品状态筛选") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(productService.getMyProducts(userId, status, page, size));
    }
}
