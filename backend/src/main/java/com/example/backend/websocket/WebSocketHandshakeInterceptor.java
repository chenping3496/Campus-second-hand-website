package com.example.backend.websocket;

import com.example.backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
    
    @Autowired
    private JwtUtil jwtUtil;
    
/**
 * WebSocket握手前的拦截方法，用于验证连接请求
 * @param request 当前的HTTP请求对象
 * @param response 当前的HTTP响应对象
 * @param wsHandler WebSocket处理器
 * @param attributes 用于WebSocket会话的属性映射
 * @return 如果验证通过返回true，否则返回false
 * @throws Exception 可能抛出的异常
 */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
    // 检查请求是否为Servlet类型的HTTP请求
        if (request instanceof ServletServerHttpRequest) {
        // 将请求强制转换为ServletServerHttpRequest以获取Servlet特有的功能
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
        // 从请求参数中获取token
            String token = servletRequest.getServletRequest().getParameter("token");
            
        // 验证token是否有效
            if (token != null && jwtUtil.validateToken(token)) {
            // 从token中提取用户ID并添加到WebSocket属性中
                Long userId = jwtUtil.getUserIdFromToken(token);
                attributes.put("userId", userId);
            // 验证通过，允许握手继续
                return true;
            }
        }
    // 验证失败，拒绝握手
        return false;
    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
