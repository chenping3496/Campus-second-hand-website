package com.example.backend.config;

import com.example.backend.websocket.enhanced.EnhancedChatWebSocketHandler;
import com.example.backend.websocket.WebSocketHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 * 使用增强版的WebSocket处理器
 */
@Configuration // 标记为配置类，让Spring容器管理
@EnableWebSocket // 开启WebSocket支持
public class WebSocketConfig implements WebSocketConfigurer { // 实现WebSocketConfigurer接口，用于配置WebSocket

    /**
     * 自动注入增强版聊天WebSocket处理器
     * 增加心跳、ACK、连接管理等功能
     */
    @Autowired
    private EnhancedChatWebSocketHandler enhancedChatWebSocketHandler;

    /**
     * 自动注入WebSocket握手拦截器
     * 用于在握手前进行一些验证或设置
     */
    @Autowired
    private WebSocketHandshakeInterceptor handshakeInterceptor;

    /**
     * 注册WebSocket处理器
     * @param registry WebSocket处理器注册器
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册增强版聊天WebSocket处理器，并设置访问路径为"/ws/chat"
        // 添加握手拦截器
        // 设置允许所有来源的跨域请求（生产环境应该限制允许的域名）
        registry.addHandler(enhancedChatWebSocketHandler, "/ws/chat")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
