package com.example.backend.consumer;

import lombok.Data;

/**
 * 聊天离线通知消息体：接收方不在线时，由 ChatWebSocketHandler 投递，
 * ChatOfflineNotifyConsumer 落库为 SYSTEM 通知，用户下次可见。
 */
@Data
public class ChatOfflineMessage {
    private Long receiverId;
    private Long senderId;
    private Long conversationId;
    private String content;
}
