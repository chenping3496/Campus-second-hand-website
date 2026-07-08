package com.example.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.dto.*;
import com.example.backend.entity.User;
import com.example.backend.entity.VerificationAppeal;
import com.example.backend.mapper.UserMapper;
import com.example.backend.mapper.VerificationAppealMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class VerificationService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private VerificationAppealMapper appealMapper;

    // ============ User-side ============

    public Result<Map<String, Object>> getStatus(Long userId) {
        User user = userMapper.selectById(userId);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("verificationStatus", user.getVerificationStatus().name());
        result.put("verificationRemark", user.getVerificationRemark());
        result.put("realName", user.getRealName());
        result.put("identityType", user.getIdentityType() != null ? user.getIdentityType().name() : null);
        result.put("identityNumber", user.getIdentityNumber());

        // Check if there's a pending appeal
        LambdaQueryWrapper<VerificationAppeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VerificationAppeal::getUserId, userId)
               .eq(VerificationAppeal::getStatus, VerificationAppeal.AppealStatus.PENDING)
               .orderByDesc(VerificationAppeal::getCreatedAt)
               .last("LIMIT 1");
        VerificationAppeal appeal = appealMapper.selectOne(wrapper);
        result.put("hasPendingAppeal", appeal != null);

        return Result.success(result);
    }

    @Transactional
    public Result<Void> submitVerification(Long userId, VerificationRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.error("用户不存在");

        if (user.getVerificationStatus() == User.VerificationStatus.PENDING) {
            return Result.error("认证申请正在审核中，请耐心等待");
        }
        if (user.getVerificationStatus() == User.VerificationStatus.APPROVED) {
            return Result.error("您已完成认证，无需重复提交");
        }
        if (user.getVerificationStatus() == User.VerificationStatus.FROZEN) {
            return Result.error("您的认证已被冻结，无法重新提交");
        }

        User.IdentityType identityType;
        try {
            identityType = User.IdentityType.valueOf(request.getIdentityType());
        } catch (IllegalArgumentException e) {
            return Result.error("身份类型无效，请选择 STUDENT 或 TEACHER");
        }

        user.setRealName(request.getRealName());
        user.setIdentityType(identityType);
        user.setIdentityNumber(request.getIdentityNumber());
        user.setIdCardImage(request.getIdCardImage());
        user.setVerificationStatus(User.VerificationStatus.PENDING);
        user.setVerificationRemark(null);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        return Result.success();
    }

    @Transactional
    public Result<Void> submitAppeal(Long userId, AppealRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.error("用户不存在");

        if (user.getVerificationStatus() != User.VerificationStatus.REJECTED) {
            return Result.error("只有认证被驳回后才能提交申诉");
        }

        // Check if already has a pending appeal
        LambdaQueryWrapper<VerificationAppeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VerificationAppeal::getUserId, userId)
               .eq(VerificationAppeal::getStatus, VerificationAppeal.AppealStatus.PENDING);
        if (appealMapper.selectCount(wrapper) > 0) {
            return Result.error("已有申诉正在处理中，请耐心等待");
        }

        VerificationAppeal appeal = new VerificationAppeal();
        appeal.setUserId(userId);
        appeal.setReason(request.getReason());
        appeal.setImage(request.getImage());
        appeal.setStatus(VerificationAppeal.AppealStatus.PENDING);
        appealMapper.insert(appeal);

        // Mark user as under review (set to PENDING for re-review)
        user.setVerificationStatus(User.VerificationStatus.PENDING);
        user.setVerificationRemark("申诉处理中：" + request.getReason());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        return Result.success();
    }

    // ============ Admin-side ============

    public PageResult<VerificationDTO> getVerificationList(String status, int page, int size) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(User::getVerificationStatus, User.VerificationStatus.valueOf(status));
        } else {
            // Default: show non-UNVERIFIED users
            wrapper.ne(User::getVerificationStatus, User.VerificationStatus.UNVERIFIED);
        }
        wrapper.orderByDesc(User::getUpdatedAt);

        Page<User> mpPage = userMapper.selectPage(new Page<>(page + 1, size), wrapper);
        List<VerificationDTO> list = mpPage.getRecords().stream()
                .map(VerificationDTO::fromEntity)
                .toList();
        return PageResult.of(list, mpPage.getTotal(), page, size);
    }

    @Transactional
    public Result<Void> approve(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.error("用户不存在");
        if (user.getVerificationStatus() == User.VerificationStatus.APPROVED) {
            return Result.error("该用户已经通过认证");
        }

        user.setVerificationStatus(User.VerificationStatus.APPROVED);
        user.setVerificationRemark(null);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return Result.success();
    }

    @Transactional
    public Result<Void> reject(Long userId, String reason) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.error("用户不存在");

        user.setVerificationStatus(User.VerificationStatus.REJECTED);
        user.setVerificationRemark(reason);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return Result.success();
    }

    @Transactional
    public Result<Void> freeze(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.error("用户不存在");
        if (user.getVerificationStatus() == User.VerificationStatus.FROZEN) {
            return Result.error("该用户已被冻结");
        }

        user.setVerificationStatus(User.VerificationStatus.FROZEN);
        user.setVerificationRemark("账号认证已被冻结");
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return Result.success();
    }

    @Transactional
    public Result<Void> unfreeze(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.error("用户不存在");
        if (user.getVerificationStatus() != User.VerificationStatus.FROZEN) {
            return Result.error("该用户未被冻结");
        }

        user.setVerificationStatus(User.VerificationStatus.APPROVED);
        user.setVerificationRemark(null);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return Result.success();
    }

    @Transactional
    public Result<Void> review(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return Result.error("用户不存在");

        user.setVerificationStatus(User.VerificationStatus.PENDING);
        user.setVerificationRemark("管理员复核中");
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return Result.success();
    }

    public PageResult<AppealDTO> getAppeals(String status, int page, int size) {
        LambdaQueryWrapper<VerificationAppeal> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(VerificationAppeal::getStatus, VerificationAppeal.AppealStatus.valueOf(status));
        }
        wrapper.orderByDesc(VerificationAppeal::getCreatedAt);

        Page<VerificationAppeal> mpPage = appealMapper.selectPage(new Page<>(page + 1, size), wrapper);
        List<AppealDTO> list = mpPage.getRecords().stream()
                .map(a -> {
                    User user = userMapper.selectById(a.getUserId());
                    return AppealDTO.fromEntity(a, user);
                })
                .toList();
        return PageResult.of(list, mpPage.getTotal(), page, size);
    }

    @Transactional
    public Result<Void> handleAppeal(Long appealId, boolean approved, String response) {
        VerificationAppeal appeal = appealMapper.selectById(appealId);
        if (appeal == null) return Result.error("申诉不存在");
        if (appeal.getStatus() != VerificationAppeal.AppealStatus.PENDING) {
            return Result.error("该申诉已处理");
        }

        appeal.setStatus(approved ? VerificationAppeal.AppealStatus.APPROVED : VerificationAppeal.AppealStatus.REJECTED);
        appeal.setAdminResponse(response);
        appeal.setHandledAt(LocalDateTime.now());
        appealMapper.updateById(appeal);

        User user = userMapper.selectById(appeal.getUserId());
        if (user != null) {
            if (approved) {
                user.setVerificationStatus(User.VerificationStatus.APPROVED);
                user.setVerificationRemark(null);
            } else {
                user.setVerificationStatus(User.VerificationStatus.REJECTED);
                user.setVerificationRemark("申诉被驳回：" + (response != null ? response : ""));
            }
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
        }

        return Result.success();
    }
}
