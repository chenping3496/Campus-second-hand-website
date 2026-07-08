package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("messages")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    private Long senderId;

    private Long receiverId;

    private MessageType type = MessageType.TEXT;

    private String content;

    private Boolean isRead = false;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public enum MessageType {
        TEXT, IMAGE, EMOJI
    }
}
