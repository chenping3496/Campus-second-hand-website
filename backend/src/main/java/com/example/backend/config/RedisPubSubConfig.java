package com.example.backend.config;

import com.example.backend.websocket.ChatWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.ChannelTopic;

/**
 * Redis Pub/Sub 配置：每个实例订阅 ws:push 频道，由 ChatWebSocketHandler.onMessage
 * 做本地 session 投递，实现 WebSocket 跨实例推送。
 */
@Configuration
public class RedisPubSubConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory, ChatWebSocketHandler chatWebSocketHandler) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // 订阅 ws:push；ChatWebSocketHandler 实现 MessageListener，收到 publish 后查本地 Map 投递
        container.addMessageListener(chatWebSocketHandler, new ChannelTopic(ChatWebSocketHandler.WS_PUSH_CHANNEL));
        return container;
    }
}
