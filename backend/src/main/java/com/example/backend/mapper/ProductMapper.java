package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Select("SELECT category_id, COUNT(*) as cnt FROM products WHERE deleted = 0 GROUP BY category_id")
    List<Map<String, Object>> countByCategory();

    @Select("SELECT COUNT(*) FROM products WHERE deleted = 0")
    long countAllProducts();

    @Select("SELECT COUNT(*) FROM products WHERE status = 'ON_SALE' AND deleted = 0")
    long countOnSaleProducts();

    @Select("SELECT COUNT(*) FROM products WHERE status = 'PENDING' AND deleted = 0")
    long countPendingProducts();

    /**
     * 原子累加浏览数：UPDATE products SET view_count = view_count + delta WHERE id = ?
     * 仅更新计数值，避免整行回写（updateById 写所有列）。
     */
    @Update("UPDATE products SET view_count = view_count + #{delta} WHERE id = #{productId}")
    int incrementViewCount(@Param("productId") Long productId, @Param("delta") long delta);
}
