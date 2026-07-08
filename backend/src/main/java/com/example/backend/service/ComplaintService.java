package com.example.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.dto.*;
import com.example.backend.entity.Complaint;
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
        return Result.success();
    }

    @Transactional
    public Result<Void> markProcessing(Long complaintId, Long adminId) {
        Complaint c = complaintMapper.selectById(complaintId);
        if (c == null) return Result.error("投诉不存在");
        c.setStatus(Complaint.ComplaintStatus.PROCESSING);
        c.setHandledBy(adminId);
        complaintMapper.updateById(c);
        return Result.success();
    }

    private PageResult<ComplaintDTO> queryPage(LambdaQueryWrapper<Complaint> wrapper, int page, int size) {
        Page<Complaint> mpPage = complaintMapper.selectPage(new Page<>(page + 1, size), wrapper);
        List<Complaint> complaints = mpPage.getRecords();
        if (complaints.isEmpty()) return PageResult.of(List.of(), 0, page, size);

        Set<Long> userIds = new HashSet<>();
        complaints.forEach(c -> {
            userIds.add(c.getUserId());
            if (c.getTargetUserId() != null) userIds.add(c.getTargetUserId());
            if (c.getHandledBy() != null) userIds.add(c.getHandledBy());
        });
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<ComplaintDTO> list = complaints.stream()
                .map(c -> ComplaintDTO.fromEntity(c,
                        userMap.get(c.getUserId()),
                        userMap.get(c.getTargetUserId()),
                        userMap.get(c.getHandledBy())))
                .toList();
        return PageResult.of(list, mpPage.getTotal(), page, size);
    }
}
