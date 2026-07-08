package com.example.backend.service;

import com.example.backend.dto.*;
import com.example.backend.entity.*;
import com.example.backend.mapper.MessageMapper;
import com.example.backend.mapper.NotificationMapper;
import com.example.backend.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private UserMapper userMapper;

    @Transactional
    public void sendNotification(Long userId, String title, String content,
                                  Notification.NotificationType type, Long relatedId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setRelatedId(relatedId);

        notificationMapper.insert(notification);
    }

    public PageResult<NotificationDTO> getNotifications(Long userId, int page, int size) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return PageResult.of(List.of(), 0, page, size);
        }

        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId).orderByDesc(Notification::getCreatedAt);
        Page<Notification> mpPage = notificationMapper.selectPage(new Page<>(page + 1, size), wrapper);
        List<NotificationDTO> list = mpPage.getRecords().stream()
                .map(NotificationDTO::fromEntity)
                .toList();

        return PageResult.of(list, mpPage.getTotal(), page, size);
    }

    public long getUnreadCount(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return 0;
        }
        return notificationMapper.countUnreadByUserId(userId);
    }

    @Transactional
    public Result<Void> markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            return Result.error("通知不存在");
        }

        if (!notification.getUserId().equals(userId)) {
            return Result.error("无权操作");
        }

        notification.setIsRead(true);
        notificationMapper.updateById(notification);

        return Result.success();
    }

    @Transactional
    public Result<Void> markAllAsRead(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        notificationMapper.markAllAsRead(userId);

        return Result.success();
    }
}
