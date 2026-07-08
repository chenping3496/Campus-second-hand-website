package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notifications")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    private NotificationType type;

    private Long relatedId;

    private Boolean isRead = false;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public enum NotificationType {
        PRODUCT_APPROVED,
        PRODUCT_REJECTED,
        PRODUCT_OFF_SHELF,
        ORDER_NEW,
        ORDER_SHIPPED,
        ORDER_CANCELLED,
        ORDER_COMPLETED,
        SYSTEM
    }
}
