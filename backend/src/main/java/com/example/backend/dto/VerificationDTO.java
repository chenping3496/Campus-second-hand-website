package com.example.backend.dto;

import com.example.backend.entity.User;
import lombok.Data;

@Data
public class VerificationDTO {
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String realName;
    private String identityType;
    private String identityNumber;
    private String idCardImage;
    private String verificationStatus;
    private String verificationRemark;

    public static VerificationDTO fromEntity(User user) {
        VerificationDTO dto = new VerificationDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setRealName(user.getRealName());
        dto.setIdentityType(user.getIdentityType() != null ? user.getIdentityType().name() : null);
        dto.setIdentityNumber(user.getIdentityNumber());
        dto.setIdCardImage(user.getIdCardImage());
        dto.setVerificationStatus(user.getVerificationStatus().name());
        dto.setVerificationRemark(user.getVerificationRemark());
        return dto;
    }
}
