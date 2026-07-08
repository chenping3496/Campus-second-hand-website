package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "分类", description = "商品分类相关接口")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/list")
    @Operation(summary = "获取已启用的分类列表")
    public Result<List<CategoryDTO>> getCategories() {
        return Result.success(categoryService.getEnabledCategories());
    }
}
