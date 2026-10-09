package com.example.backend.config;

import com.example.backend.websocket.enhanced.EnhancedChatWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.ChannelTopic;

/**
 * Redis Pub/Sub 配置：每个实例订阅 ws:push 频道，由 EnhancedChatWebSocketHandler.onMessage
 * 做本地 session 投递，实现 WebSocket 跨实例推送。
 */
@Configuration
public class RedisPubSubConfig {

    /**
     * 配置Redis消息监听器容器
     * 该容器用于监听Redis频道消息，并将消息传递给相应的处理器
     *
     * @param connectionFactory Redis连接工厂，用于创建Redis连接
     * @param enhancedChatWebSocketHandler 增强版WebSocket消息处理器，实现了MessageListener接口
     * @return 配置好的Redis消息监听器容器实例
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory, EnhancedChatWebSocketHandler enhancedChatWebSocketHandler) {
        // 创建Redis消息监听器容器实例
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        // 设置Redis连接工厂
        container.setConnectionFactory(connectionFactory);
        // 订阅 ws:push；EnhancedChatWebSocketHandler 实现 MessageListener，收到 publish 后查本地 Map 投递
        container.addMessageListener(enhancedChatWebSocketHandler, new ChannelTopic(EnhancedChatWebSocketHandler.WS_PUSH_CHANNEL));
        return container;
    }
}
