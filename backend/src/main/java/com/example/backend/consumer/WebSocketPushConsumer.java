package com.example.backend.consumer;

import com.example.backend.config.RabbitMQConfig;
import com.example.backend.websocket.ChatWebSocketHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 消费者 2：通知实时推送到在线用户的 WebSocket 会话。离线用户由通知列表（消费者 1 落库）兜底。
 * <p>
 * acknowledge-mode=auto：正常返回即 ack；抛异常由 Spring 重试，耗尽后进死信队列。
 */
@Component
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebSocketPushConsumer {

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_PUSH_QUEUE)
    public void handlePush(NotificationMessage message) {
        chatWebSocketHandler.sendToUser(message.getUserId(), Map.of(
                "action", "notification",
                "data", Map.of(
                        "title", message.getTitle() == null ? "" : message.getTitle(),
                        "content", message.getContent() == null ? "" : message.getContent(),
                        "type", message.getType() == null ? "" : message.getType().name(),
                        "relatedId", message.getRelatedId() == null ? 0 : message.getRelatedId()
                )
        ));
    }
}
