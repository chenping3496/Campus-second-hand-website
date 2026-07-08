package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@Tag(name = "用户", description = "用户信息管理接口")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/info")
    @Operation(summary = "获取当前用户信息")
    public Result<UserDTO> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return userService.getUserInfo(userId);
    }

    @GetMapping("/info/{id}")
    @Operation(summary = "获取指定用户信息")
    public Result<UserDTO> getUserInfoById(@PathVariable Long id) {
        return userService.getUserInfo(id);
    }

    @PutMapping("/info")
    @Operation(summary = "更新用户信息")
    public Result<UserDTO> updateUserInfo(HttpServletRequest request, @RequestBody UserDTO dto) {
        Long userId = (Long) request.getAttribute("userId");
        return userService.updateUserInfo(userId, dto);
    }

    @PostMapping("/password")
    @Operation(summary = "修改密码")
    public Result<Void> changePassword(HttpServletRequest request, @RequestBody Map<String, String> params) {
        Long userId = (Long) request.getAttribute("userId");
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        return userService.changePassword(userId, oldPassword, newPassword);
    }
}
