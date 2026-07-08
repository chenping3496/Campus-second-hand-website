package com.example.backend.dto;

import com.example.backend.entity.User;
import com.example.backend.entity.VerificationAppeal;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AppealDTO {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String reason;
    private String image;
    private String status;
    private String adminResponse;
    private LocalDateTime createdAt;
    private LocalDateTime handledAt;

    public static AppealDTO fromEntity(VerificationAppeal appeal, User user) {
        AppealDTO dto = new AppealDTO();
        dto.setId(appeal.getId());
        dto.setUserId(appeal.getUserId());
        dto.setReason(appeal.getReason());
        dto.setImage(appeal.getImage());
        dto.setStatus(appeal.getStatus().name());
        dto.setAdminResponse(appeal.getAdminResponse());
        dto.setCreatedAt(appeal.getCreatedAt());
        dto.setHandledAt(appeal.getHandledAt());
        if (user != null) {
            dto.setUsername(user.getUsername());
            dto.setNickname(user.getNickname());
        }
        return dto;
    }
}
