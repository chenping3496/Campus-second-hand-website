package com.example.backend.controller;

import com.example.backend.dto.*;
import com.example.backend.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "聊天", description = "即时通讯相关接口")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @GetMapping("/conversations")
    @Operation(summary = "获取会话列表")
    public Result<PageResult<ConversationDTO>> getConversations(
            HttpServletRequest request,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatService.getConversations(userId, page, size));
    }

    @PostMapping("/conversations")
    @Operation(summary = "进入或创建会话")
    public Result<ConversationDTO> getOrCreateConversation(
            HttpServletRequest request,
            @RequestBody Map<String, Long> params) {
        Long userId = (Long) request.getAttribute("userId");
        Long otherUserId = params.get("otherUserId");
        Long productId = params.get("productId");
        return chatService.getOrCreateConversation(userId, otherUserId, productId);
    }

    @GetMapping("/conversations/{id}")
    @Operation(summary = "获取会话详情")
    public Result<ConversationDTO> getConversation(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return chatService.getConversation(id, userId);
    }

    @GetMapping("/conversations/{id}/messages")
    @Operation(summary = "获取会话消息列表")
    public Result<PageResult<MessageDTO>> getMessages(
            @PathVariable Long id,
            HttpServletRequest request,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "50") int size) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatService.getMessages(id, userId, page, size));
    }

    @PostMapping("/conversations/{id}/messages")
    @Operation(summary = "发送消息")
    public Result<MessageDTO> sendMessage(
            @PathVariable Long id,
            HttpServletRequest request,
            @RequestBody Map<String, String> params) {
        Long userId = (Long) request.getAttribute("userId");
        String content = params.get("content");
        String type = params.get("type");
        return chatService.sendMessage(id, userId, content, type);
    }

    @PostMapping("/conversations/{id}/read")
    @Operation(summary = "标记消息已读")
    public Result<Void> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return chatService.markMessagesAsRead(id, userId);
    }

    @GetMapping("/unread")
    @Operation(summary = "获取未读消息总数")
    public Result<Integer> getUnreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatService.getUnreadCount(userId));
    }
}
