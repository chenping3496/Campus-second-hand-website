package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ComplaintRequest {
    private Long targetUserId;
    private Long orderId;
    @NotBlank(message = "投诉类型不能为空")
    private String type;  // USER, PRODUCT, ORDER, OTHER
    @NotBlank(message = "投诉标题不能为空")
    private String title;
    @NotBlank(message = "投诉内容不能为空")
    private String content;
    private String images;
}
