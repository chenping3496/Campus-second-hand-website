package com.example.backend.dto;

import com.example.backend.entity.Message;
import com.example.backend.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageDTO {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private Long receiverId;
    private String receiverName;
    private String type;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static MessageDTO fromEntity(Message message, User sender, User receiver) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversationId());
        if (sender != null) {
            dto.setSenderId(sender.getId());
            dto.setSenderName(sender.getNickname() != null ? sender.getNickname() : sender.getUsername());
            dto.setSenderAvatar(sender.getAvatar());
        } else {
            dto.setSenderId(message.getSenderId());
        }
        if (receiver != null) {
            dto.setReceiverId(receiver.getId());
            dto.setReceiverName(receiver.getNickname() != null ? receiver.getNickname() : receiver.getUsername());
        } else {
            dto.setReceiverId(message.getReceiverId());
        }
        dto.setType(message.getType().name());
        dto.setContent(message.getContent());
        dto.setIsRead(message.getIsRead());
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }
}
