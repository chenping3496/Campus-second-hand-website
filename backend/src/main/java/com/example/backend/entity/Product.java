package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("products")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Long categoryId;

    private Long sellerId;

    private ProductStatus status = ProductStatus.PENDING;

    private ProductTag productTag = ProductTag.NORMAL;

    private String rejectReason;

    private String images;

    private Integer viewCount = 0;

    @TableLogic
    private Boolean deleted = false;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;

    public enum ProductStatus {
        PENDING, ON_SALE, SOLD, OFF_SHELF, REJECTED
    }

    public enum ProductTag {
        NORMAL, URGENT_SCHOOL, URGENT_GRADUATION
    }
}
