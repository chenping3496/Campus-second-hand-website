package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")//对应@valid中的条件
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
}
