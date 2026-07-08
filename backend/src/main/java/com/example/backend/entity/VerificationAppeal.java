package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("verification_appeals")
public class VerificationAppeal {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String reason;

    private String image;

    private AppealStatus status = AppealStatus.PENDING;

    private String adminResponse;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private LocalDateTime handledAt;

    public enum AppealStatus { PENDING, APPROVED, REJECTED }
}
