package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerificationRequest {
    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @NotNull(message = "身份类型不能为空")
    private String identityType; // STUDENT or TEACHER

    @NotBlank(message = "学号/工号不能为空")
    private String identityNumber;

    private String idCardImage;
}
