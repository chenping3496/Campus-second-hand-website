package com.example.backend.dto;

import com.example.backend.entity.User;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private String studentId;
    private String dormitory;
    private String role;
    private String status;
    private LocalDateTime createdAt;
    
    public static UserDTO fromEntity(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setPhone(user.getPhone());
        dto.setEmail(user.getEmail());
        dto.setStudentId(user.getStudentId());
        dto.setDormitory(user.getDormitory());
        dto.setRole(user.getRole().name());
        dto.setStatus(user.getStatus().name());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
