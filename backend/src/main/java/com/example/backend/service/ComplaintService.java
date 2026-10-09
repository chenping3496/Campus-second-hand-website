package com.example.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.dto.*;
import com.example.backend.entity.Complaint;
import com.example.backend.entity.Notification;
import com.example.backend.entity.User;
import com.example.backend.mapper.ComplaintMapper;
import com.example.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ComplaintService {

    @Autowired
    private ComplaintMapper complaintMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NotificationService notificationService;

    // ========= User-side =========

    @Transactional
    public Result<Void> submitComplaint(Long userId, ComplaintRequest request) {
        Complaint c = new Complaint();
        c.setUserId(userId);
        c.setTargetUserId(request.getTargetUserId());
        c.setOrderId(request.getOrderId());
        c.setType(request.getType());
        c.setTitle(request.getTitle());
        c.setContent(request.getContent());
        c.setImages(request.getImages());
        c.setStatus(Complaint.ComplaintStatus.PENDING);
        complaintMapper.insert(c);

        notificationService.sendNotification(
                userId, "投诉已提交",
                "您的投诉「" + c.getTitle() + "」已提交，我们将在 1 个工作日内处理。",
                Notification.NotificationType.SYSTEM, c.getId()
        );
        return Result.success();
    }

    public PageResult<ComplaintDTO> getMyComplaints(Long userId, int page, int size) {
        LambdaQueryWrapper<Complaint> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Complaint::getUserId, userId).orderByDesc(Complaint::getCreatedAt);
        return queryPage(wrapper, page, size);
    }

    // ========= Admin-side =========

    public PageResult<ComplaintDTO> getComplaintList(String status, String type, int page, int size) {
        LambdaQueryWrapper<Complaint> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Complaint::getStatus, Complaint.ComplaintStatus.valueOf(status));
        }
        if (type != null && !type.isEmpty()) {
            wrapper.eq(Complaint::getType, type);
        }
        wrapper.orderByDesc(Complaint::getCreatedAt);
        return queryPage(wrapper, page, size);
    }

    @Transactional
    public Result<Void> handleComplaint(Long complaintId, Long adminId, String adminResponse) {
        Complaint c = complaintMapper.selectById(complaintId);
        if (c == null) return Result.error("投诉不存在");

        c.setStatus(Complaint.ComplaintStatus.RESOLVED);
        c.setAdminResponse(adminResponse);
        c.setHandledBy(adminId);
        c.setHandledAt(LocalDateTime.now());
        complaintMapper.updateById(c);

        notificationService.sendNotification(
                c.getUserId(), "投诉处理完成",
                "您的投诉「" + c.getTitle() + "」已处理完成。回复：" + adminResponse,
                Notification.NotificationType.SYSTEM, c.getId()
        );
        return Result.success();
    }

    @Transactional
    public Result<Void> markProcessing(Long complaintId, Long adminId) {
        Complaint c = complaintMapper.selectById(complaintId);
        if (c == null) return Result.error("投诉不存在");
        c.setStatus(Complaint.ComplaintStatus.PROCESSING);
        c.setHandledBy(adminId);
        complaintMapper.updateById(c);

        notificationService.sendNotification(
                c.getUserId(), "投诉处理中",
                "您的投诉「" + c.getTitle() + "」正在处理中。",
                Notification.NotificationType.SYSTEM, c.getId()
        );
        return Result.success();
    }

/**
 * 分页查询投诉信息，并转换为DTO对象
 * @param wrapper 查询条件封装对象，用于构建查询条件
 * @param page 当前页码
 * @param size 每页记录数
 * @return PageResult<ComplaintDTO> 包含投诉信息列表和分页信息的分页结果对象
 */
    private PageResult<ComplaintDTO> queryPage(LambdaQueryWrapper<Complaint> wrapper, int page, int size) {
    // 使用MyBatis-Plus的分页查询功能查询投诉记录
        Page<Complaint> mpPage = complaintMapper.selectPage(new Page<>(page + 1, size), wrapper);
    // 获取查询结果中的记录列表
        List<Complaint> complaints = mpPage.getRecords();
    // 如果记录为空，返回空结果
        if (complaints.isEmpty()) return PageResult.of(List.of(), 0, page, size);

    // 收集所有相关的用户ID
        Set<Long> userIds = new HashSet<>();
        complaints.forEach(c -> {  // 遍历每条投诉记录
            userIds.add(c.getUserId());  // 添加投诉人ID
            if (c.getTargetUserId() != null) userIds.add(c.getTargetUserId());  // 添加被投诉人ID（如果存在）
            if (c.getHandledBy() != null) userIds.add(c.getHandledBy());  // 添加处理人ID（如果存在）
        });
    // 批量查询所有相关用户信息，并转换为Map以便快速查找
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

    // 将投诉实体列表转换为DTO列表，并填充相关用户信息
        List<ComplaintDTO> list = complaints.stream()
                .map(c -> ComplaintDTO.fromEntity(c,
                        userMap.get(c.getUserId()),      // 投诉人信息
                        userMap.get(c.getTargetUserId()), // 被投诉人信息
                        userMap.get(c.getHandledBy())))   // 处理人信息
                .toList();
    // 返回分页结果
        return PageResult.of(list, mpPage.getTotal(), page, size);
    }
}
