package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.service.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/verification")
@Tag(name = "身份认证", description = "用户身份认证相关接口")
public class VerificationController {

    @Autowired
    private VerificationService verificationService;

    @GetMapping("/status")
    @Operation(summary = "获取当前用户认证状态")
    public Result<Map<String, Object>> getStatus(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return verificationService.getStatus(userId);
    }

    @PostMapping("/submit")
    @Operation(summary = "提交身份认证")
    public Result<Void> submitVerification(HttpServletRequest request,
                                           @Valid @RequestBody VerificationRequest verificationRequest) {
        Long userId = (Long) request.getAttribute("userId");
        return verificationService.submitVerification(userId, verificationRequest);
    }

    @PostMapping("/appeal")
    @Operation(summary = "提交申诉")
    public Result<Void> submitAppeal(HttpServletRequest request,
                                     @Valid @RequestBody AppealRequest appealRequest) {
        Long userId = (Long) request.getAttribute("userId");
        return verificationService.submitAppeal(userId, appealRequest);
    }
}
