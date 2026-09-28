package com.example.backend.consumer;

import com.example.backend.config.RabbitMQConfig;
import com.example.backend.entity.Notification;
import com.example.backend.entity.User;
import com.example.backend.mapper.NotificationMapper;
import com.example.backend.mapper.UserMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 消费者 1：通知异步落库。由 NotificationService.sendNotification 在开启 MQ 时投递到 notification 路由键。
 * <p>
 * acknowledge-mode=auto：方法正常返回即 ack；抛异常则由 Spring 重试（max-attempts=3），
 * 重试耗尽后 default-requeue-rejected=false → 消息经 x-dead-letter-exchange 进入死信队列，
 * 不再无限 requeue。
 */
@Component
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationConsumer {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private UserMapper userMapper;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotification(NotificationMessage message) {
        User user = userMapper.selectById(message.getUserId());
        if (user == null) {
            // 用户不存在视为业务可丢弃，正常返回 → ack
            return;
        }
        Notification notification = new Notification();
        notification.setUserId(message.getUserId());
        notification.setTitle(message.getTitle());
        notification.setContent(message.getContent());
        notification.setType(message.getType());
        notification.setRelatedId(message.getRelatedId());
        // DB 异常会向上抛出 → Spring 重试 → 耗尽后进死信队列
        notificationMapper.insert(notification);
    }
}
