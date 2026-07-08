package com.example.backend.websocket;

import com.example.backend.dto.MessageDTO;
import com.example.backend.dto.Result;
import com.example.backend.service.ChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    // 在线用户列表：key=userId，value=连接会话
    private static final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Autowired
    private ChatService chatService;// 聊天业务逻辑（发送消息、已读、创建会话）
    
    private final ObjectMapper objectMapper = new ObjectMapper();// JSON 转换工具
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.put(userId, session);// 用户上线 → 存入在线列表
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            return;
        }
        
        try {
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());// 把前端发来的 JSON 字符串转成对象
            String action = jsonNode.get("action").asText();// 获取动作类型

            // 根据动作执行不同逻辑
            switch (action) {
                case "send":
                    handleSendMessage(userId, jsonNode, session);
                    break;
                case "read":
                    handleMarkAsRead(userId, jsonNode);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            sendError(session, "消息格式错误");
        }
    }
    
    private void handleSendMessage(Long senderId, JsonNode jsonNode, WebSocketSession session) throws IOException {
        Long conversationId = jsonNode.get("conversationId").asLong();
        String content = jsonNode.get("content").asText();
        String type = jsonNode.has("type") ? jsonNode.get("type").asText() : "TEXT";

        // 2. 调用 Service 保存消息到数据库
        Result<MessageDTO> result = chatService.sendMessage(conversationId, senderId, content, type);
        
        if (result.getCode() == 200) {
            MessageDTO messageDTO = result.getData();

            // 3. 把消息转成 JSON 格式
            String messageJson = objectMapper.writeValueAsString(Map.of(
                    "action", "message",
                    "data", messageDTO
            ));

            // 4. 发给发送者自己
            session.sendMessage(new TextMessage(messageJson));

            // 5. 发给接收者（如果在线）
            Long receiverId = messageDTO.getReceiverId();
            WebSocketSession receiverSession = userSessions.get(receiverId);
            if (receiverSession != null && receiverSession.isOpen()) {
                receiverSession.sendMessage(new TextMessage(messageJson));
            }
        } else {
            sendError(session, result.getMessage());
        }
    }
    
    private void handleMarkAsRead(Long userId, JsonNode jsonNode) {
        Long conversationId = jsonNode.get("conversationId").asLong();
        chatService.markMessagesAsRead(conversationId, userId);
    }
    
    private void sendError(WebSocketSession session, String message) throws IOException {
        String errorJson = objectMapper.writeValueAsString(Map.of(
                "action", "error",
                "message", message
        ));
        session.sendMessage(new TextMessage(errorJson));
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.remove(userId);
        }
    }
    
    public void sendToUser(Long userId, Object message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String messageJson = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(messageJson));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
