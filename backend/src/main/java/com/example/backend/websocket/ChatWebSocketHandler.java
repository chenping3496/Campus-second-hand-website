package com.example.backend.websocket;

import com.example.backend.config.RabbitMQConfig;
import com.example.backend.consumer.ChatOfflineMessage;
import com.example.backend.dto.MessageDTO;
import com.example.backend.dto.Result;
import com.example.backend.service.ChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天 WebSocket 处理器。
 * <p>
 * 跨实例推送：sendToUser 不再直接查本地 userSessions，而是把 {userId, messageJson}
 * publish 到 Redis 的 ws:push 频道；每个实例都订阅该频道（见 RedisPubSubConfig），
 * 收到后只对本地持有 session 的 userId 真正 sendMessage，其余实例丢弃。
 * 在线状态用 Redis Set ws:online 做全局登记，判断目标用户是否在任意实例在线。
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler implements MessageListener {

    /** 跨实例推送频道：sendToUser publish 到这里，所有实例订阅 */
    public static final String WS_PUSH_CHANNEL = "ws:push";

    /** 全局在线用户集合（Redis Set），跨实例共享 */
    public static final String WS_ONLINE_KEY = "ws:online";

    // 本实例在线用户列表：key=userId，value=连接会话
    private static final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Autowired
    private ChatService chatService;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${spring.rabbitmq.enabled:true}")
    private boolean rabbitEnabled;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            userSessions.put(userId, session);
            // 全局在线登记：任意实例都能查到该用户在线
            stringRedisTemplate.opsForSet().add(WS_ONLINE_KEY, userId.toString());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            return;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());
            String action = jsonNode.get("action").asText();

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

        // 1. 调用 Service 保存消息到数据库（持久化，离线也能看到）
        Result<MessageDTO> result = chatService.sendMessage(conversationId, senderId, content, type);

        if (result.getCode() == 200) {
            MessageDTO messageDTO = result.getData();

            // 2. 把消息转成 JSON 格式
            Map<String, Object> payload = Map.of("action", "message", "data", messageDTO);
            String messageJson = objectMapper.writeValueAsString(payload);

            // 3. 发给发送者自己（本实例，直接推）
            session.sendMessage(new TextMessage(messageJson));

            // 4. 发给接收者：跨实例，经 Redis Pub/Sub 由持有 session 的实例送达
            Long receiverId = messageDTO.getReceiverId();
            if (isOnline(receiverId)) {
                sendToUser(receiverId, payload);
            } else if (rabbitEnabled && rabbitTemplate != null) {
                // 全局离线：异步落库一条"新消息"通知
                ChatOfflineMessage offline = new ChatOfflineMessage();
                offline.setReceiverId(receiverId);
                offline.setSenderId(senderId);
                offline.setConversationId(conversationId);
                offline.setContent(content);
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME,
                        RabbitMQConfig.CHAT_OFFLINE_ROUTING_KEY, offline);
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
            // 注销全局在线：单设备假设；多设备需按 session 计数，这里简化
            stringRedisTemplate.opsForSet().remove(WS_ONLINE_KEY, userId.toString());
        }
    }

    /**
     * 跨实例推送入口：把消息 publish 到 ws:push 频道，由持有目标 session 的实例本地投递。
     * 不再直接查本地 Map —— 否则接收者在别的实例上就收不到。
     */
    public void sendToUser(Long userId, Object message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);
            WsPushEnvelope envelope = new WsPushEnvelope();
            envelope.setUserId(userId);
            envelope.setMessageJson(messageJson);
            stringRedisTemplate.convertAndSend(WS_PUSH_CHANNEL,
                    objectMapper.writeValueAsString(envelope));
        } catch (Exception e) {
            // 序列化失败属于程序 bug，打日志即可
            e.printStackTrace();
        }
    }

    /**
     * Redis Pub/Sub 回调：所有实例都会收到 publish，只对本实例持有的 userId 投递。
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            WsPushEnvelope envelope = objectMapper.readValue(body, WsPushEnvelope.class);
            if (envelope == null || envelope.getUserId() == null) {
                return;
            }
            deliverLocally(envelope.getUserId(), envelope.getMessageJson());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deliverLocally(Long userId, String messageJson) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(messageJson));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 本实例不持有该 session：说明用户在别的实例或已下线，静默丢弃即可
    }

    private boolean isOnline(Long userId) {
        if (userId == null) {
            return false;
        }
        Boolean member = stringRedisTemplate.opsForSet().isMember(WS_ONLINE_KEY, userId.toString());
        return Boolean.TRUE.equals(member);
    }
}
