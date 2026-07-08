package com.example.backend.dto;

import com.example.backend.entity.Complaint;
import com.example.backend.entity.User;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Data
public class ComplaintDTO {
    private Long id;
    private Long userId;
    private String username;
    private String userNickname;
    private Long targetUserId;
    private String targetUsername;
    private String targetNickname;
    private Long orderId;
    private String type;
    private String title;
    private String content;
    private List<String> images;
    private String status;
    private String adminResponse;
    private Long handledBy;
    private String handledByName;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;

    public static ComplaintDTO fromEntity(Complaint c, User user, User target, User handler) {
        ComplaintDTO dto = new ComplaintDTO();
        dto.setId(c.getId());
        dto.setUserId(c.getUserId());
        dto.setTargetUserId(c.getTargetUserId());
        dto.setOrderId(c.getOrderId());
        dto.setType(c.getType());
        dto.setTitle(c.getTitle());
        dto.setContent(c.getContent());
        dto.setStatus(c.getStatus().name());
        dto.setAdminResponse(c.getAdminResponse());
        dto.setHandledBy(c.getHandledBy());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setHandledAt(c.getHandledAt());

        if (user != null) {
            dto.setUsername(user.getUsername());
            dto.setUserNickname(user.getNickname());
        }
        if (target != null) {
            dto.setTargetUsername(target.getUsername());
            dto.setTargetNickname(target.getNickname());
        }
        if (handler != null) {
            dto.setHandledByName(handler.getNickname() != null ? handler.getNickname() : handler.getUsername());
        }
        if (c.getImages() != null && !c.getImages().isEmpty()) {
            dto.setImages(Arrays.asList(c.getImages().split(",")));
        }
        return dto;
    }
}
