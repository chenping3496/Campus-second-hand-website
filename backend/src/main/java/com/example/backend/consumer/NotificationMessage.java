package com.example.backend.consumer;

import lombok.Data;

@Data
public class NotificationMessage {
    private Long userId;
    private String title;
    private String content;
    private com.example.backend.entity.Notification.NotificationType type;
    private Long relatedId;
}
