package com.example.backend.dto;

import com.example.backend.entity.Conversation;
import com.example.backend.entity.Product;
import com.example.backend.entity.User;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConversationDTO {
    private Long id;
    private Long otherUserId;
    private String otherUserName;
    private String otherUserAvatar;
    private Long productId;
    private String productTitle;
    private String productImage;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;

    public static ConversationDTO fromEntity(Conversation conversation, User currentUser,
                                              User otherUser, Product product) {
        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversation.getId());

        if (otherUser != null) {
            dto.setOtherUserId(otherUser.getId());
            dto.setOtherUserName(otherUser.getNickname() != null ?
                    otherUser.getNickname() : otherUser.getUsername());
            dto.setOtherUserAvatar(otherUser.getAvatar());
        }

        if (product != null) {
            dto.setProductId(product.getId());
            dto.setProductTitle(product.getTitle());
            String images = product.getImages();
            if (images != null && !images.isEmpty()) {
                dto.setProductImage(images.split(",")[0]);
            }
        } else if (conversation.getProductId() != null) {
            dto.setProductId(conversation.getProductId());
        }

        dto.setLastMessage(conversation.getLastMessage());
        dto.setLastMessageTime(conversation.getLastMessageTime());

        if (currentUser != null) {
            int unread = conversation.getUser1Id().equals(currentUser.getId())
                    ? conversation.getUser1Unread() : conversation.getUser2Unread();
            dto.setUnreadCount(unread);
        }

        return dto;
    }
}
