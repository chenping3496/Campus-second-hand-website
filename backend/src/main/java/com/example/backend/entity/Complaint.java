package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("complaints")
public class Complaint {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;          // complainant
    private Long targetUserId;    // user being complained about (optional)
    private Long orderId;         // related order (optional)
    private String type;          // USER, PRODUCT, ORDER, OTHER
    private String title;
    private String content;
    private String images;        // comma-separated image URLs
    private ComplaintStatus status = ComplaintStatus.PENDING;
    private String adminResponse; // resolution result
    private Long handledBy;       // admin who handled it

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime handledAt;

    public enum ComplaintStatus { PENDING, PROCESSING, RESOLVED }
}
