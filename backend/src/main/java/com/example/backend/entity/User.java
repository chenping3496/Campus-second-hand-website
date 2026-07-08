package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String avatar;

    private String phone;

    private String email;

    private String studentId;

    private String dormitory;

    private UserRole role = UserRole.USER;

    private UserStatus status = UserStatus.ACTIVE;

    // === Identity verification fields ===
    private String realName;
    private IdentityType identityType;
    private String identityNumber;
    private String idCardImage;
    private VerificationStatus verificationStatus = VerificationStatus.UNVERIFIED;
    private String verificationRemark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedAt;

    public enum UserRole { USER, ADMIN }
    public enum UserStatus { ACTIVE, BANNED }
    public enum IdentityType { STUDENT, TEACHER }
    public enum VerificationStatus { UNVERIFIED, PENDING, APPROVED, REJECTED, FROZEN }
}
