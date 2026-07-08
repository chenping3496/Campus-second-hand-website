package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppealRequest {
    @NotBlank(message = "申诉原因不能为空")
    private String reason;
    private String image;
}
