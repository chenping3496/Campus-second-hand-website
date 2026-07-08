package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import com.example.backend.util.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public Result<Map<String, Object>> login(LoginRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            return Result.error("用户名或密码错误");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return Result.error("用户名或密码错误");
        }

        if (user.getStatus() == User.UserStatus.BANNED) {
            return Result.error("账号已被封禁");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", UserDTO.fromEntity(user));

        return Result.success(data);
    }

    @Transactional
    public Result<UserDTO> register(RegisterRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        if (userMapper.selectCount(wrapper) > 0) {
            return Result.error("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setRole(User.UserRole.USER);
        user.setStatus(User.UserStatus.ACTIVE);

        userMapper.insert(user);

        return Result.success(UserDTO.fromEntity(user));
    }

    public Result<UserDTO> getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(UserDTO.fromEntity(user));
    }

    @Transactional
    public Result<UserDTO> updateUserInfo(Long userId, UserDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        if (dto.getNickname() != null) user.setNickname(dto.getNickname());
        if (dto.getAvatar() != null) user.setAvatar(dto.getAvatar());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getEmail() != null) user.setEmail(dto.getEmail());
        if (dto.getStudentId() != null) user.setStudentId(dto.getStudentId());
        if (dto.getDormitory() != null) user.setDormitory(dto.getDormitory());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.updateById(user);

        return Result.success(UserDTO.fromEntity(user));
    }

    @Transactional
    public Result<Void> changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        return Result.success();
    }

    public PageResult<UserDTO> getUserList(int page, int size) {
        Page<User> mpPage = userMapper.selectPage(new Page<>(page + 1, size), null);
        List<UserDTO> list = mpPage.getRecords().stream().map(UserDTO::fromEntity).toList();
        return PageResult.of(list, mpPage.getTotal(), page, size);
    }

    public PageResult<UserDTO> getUserListForAdmin(String keyword, String status, int page, int size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(User::getStatus, User.UserStatus.valueOf(status));
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getNickname, keyword));
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> mpPage = userMapper.selectPage(new Page<>(page + 1, size), wrapper);
        List<UserDTO> list = mpPage.getRecords().stream().map(UserDTO::fromEntity).toList();
        return PageResult.of(list, mpPage.getTotal(), page, size);
    }

    @Transactional
    public Result<Void> banUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (user.getRole() == User.UserRole.ADMIN) {
            return Result.error("不能封禁管理员");
        }
        user.setStatus(User.UserStatus.BANNED);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return Result.success();
    }

    @Transactional
    public Result<Void> unbanUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setStatus(User.UserStatus.ACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return Result.success();
    }
}
