package com.example.backend.consumer;

import com.example.backend.config.RabbitMQConfig;
import com.example.backend.entity.Notification;
import com.example.backend.entity.User;
import com.example.backend.mapper.NotificationMapper;
import com.example.backend.mapper.UserMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 消费者 4：聊天接收方离线时，把“新消息”落库为系统通知，避免消息只活在 WebSocket 内存里被丢。
 */
@Component
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ChatOfflineNotifyConsumer {

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private UserMapper userMapper;

    @RabbitListener(queues = RabbitMQConfig.CHAT_OFFLINE_QUEUE)
    public void handleOffline(ChatOfflineMessage message, Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            User receiver = userMapper.selectById(message.getReceiverId());
            if (receiver == null) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            String preview = message.getContent();
            if (preview != null && preview.length() > 50) {
                preview = preview.substring(0, 50) + "...";
            }
            Notification notification = new Notification();
            notification.setUserId(message.getReceiverId());
            notification.setTitle("您有一条新消息");
            notification.setContent("会话中收到新消息：" + (preview == null ? "" : preview));
            notification.setType(Notification.NotificationType.SYSTEM);
            notification.setRelatedId(message.getConversationId());
            notificationMapper.insert(notification);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
