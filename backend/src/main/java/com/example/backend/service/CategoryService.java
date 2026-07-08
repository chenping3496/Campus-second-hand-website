package com.example.backend.service;

import com.example.backend.dto.CategoryDTO;
import com.example.backend.dto.Result;
import com.example.backend.entity.Category;
import com.example.backend.mapper.CategoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Cacheable(value = "categories", key = "'enabled'")
    public List<CategoryDTO> getEnabledCategories() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getEnabled, true).orderByAsc(Category::getSortOrder);
        return categoryMapper.selectList(wrapper)
                .stream()
                .map(CategoryDTO::fromEntity)
                .toList();
    }

    public List<CategoryDTO> getAllCategories() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getSortOrder);
        return categoryMapper.selectList(wrapper)
                .stream()
                .map(CategoryDTO::fromEntity)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "categories", key = "'enabled'")
    public Result<CategoryDTO> createCategory(CategoryDTO dto) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getName, dto.getName());
        if (categoryMapper.selectCount(wrapper) > 0) {
            return Result.error("分类名称已存在");
        }

        Category category = new Category();
        category.setName(dto.getName());
        category.setIcon(dto.getIcon());
        category.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        category.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);

        categoryMapper.insert(category);

        return Result.success(CategoryDTO.fromEntity(category));
    }

    @Transactional
    @CacheEvict(value = "categories", key = "'enabled'")
    public Result<CategoryDTO> updateCategory(Long id, CategoryDTO dto) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            return Result.error("分类不存在");
        }

        if (dto.getName() != null && !dto.getName().equals(category.getName())) {
            LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Category::getName, dto.getName());
            if (categoryMapper.selectCount(wrapper) > 0) {
                return Result.error("分类名称已存在");
            }
            category.setName(dto.getName());
        }
        if (dto.getIcon() != null) category.setIcon(dto.getIcon());
        if (dto.getSortOrder() != null) category.setSortOrder(dto.getSortOrder());
        if (dto.getEnabled() != null) category.setEnabled(dto.getEnabled());
        category.setUpdatedAt(LocalDateTime.now());

        categoryMapper.updateById(category);

        return Result.success(CategoryDTO.fromEntity(category));
    }

    @Transactional
    @CacheEvict(value = "categories", key = "'enabled'")
    public Result<Void> deleteCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            return Result.error("分类不存在");
        }
        categoryMapper.deleteById(id);
        return Result.success();
    }
}
