package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long productId;

    private Long buyerId;

    private Long sellerId;

    private BigDecimal price;

    private OrderStatus status = OrderStatus.PENDING;

    private String paymentMethod;

    private LocalDateTime paymentTime;

    private LocalDateTime shipTime;

    private LocalDateTime completeTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;

    public enum OrderStatus {
        PENDING, SHIPPED, COMPLETED, CANCELLED
    }
}
