package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "通知", description = "系统通知相关接口")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    @Operation(summary = "获取通知列表")
    public Result<PageResult<NotificationDTO>> getNotifications(
            HttpServletRequest request,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(notificationService.getNotifications(userId, page, size));
    }

    @GetMapping("/unread")
    @Operation(summary = "获取未读通知数量")
    public Result<Long> getUnreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(notificationService.getUnreadCount(userId));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记单条通知为已读")
    public Result<Void> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return notificationService.markAsRead(id, userId);
    }

    @PostMapping("/read-all")
    @Operation(summary = "全部标记为已读")
    public Result<Void> markAllAsRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return notificationService.markAllAsRead(userId);
    }
}
