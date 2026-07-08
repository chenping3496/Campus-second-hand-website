package com.example.backend.consumer;

import com.example.backend.config.RabbitMQConfig;
import com.example.backend.entity.Notification;
import com.example.backend.mapper.NotificationMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class NotificationConsumer {

    @Autowired
    private NotificationMapper notificationMapper;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotification(NotificationMessage message, Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            Notification notification = new Notification();
            notification.setUserId(message.getUserId());
            notification.setTitle(message.getTitle());
            notification.setContent(message.getContent());
            notification.setType(message.getType());
            notification.setRelatedId(message.getRelatedId());
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
