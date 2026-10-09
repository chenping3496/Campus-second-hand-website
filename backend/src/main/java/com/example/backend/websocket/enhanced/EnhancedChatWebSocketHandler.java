package com.example.backend.websocket.enhanced;

import com.example.backend.config.RabbitMQConfig;
import com.example.backend.consumer.ChatOfflineMessage;
import com.example.backend.dto.MessageDTO;
import com.example.backend.dto.Result;
import com.example.backend.service.ChatService;
import com.example.backend.websocket.WebSocketConnectionManager;
import com.example.backend.websocket.WsPushEnvelope;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 增强版聊天 WebSocket 处理器。
 * 增加心跳机制、消息ACK、连接管理等功能，适合生产环境使用
 * 可以直接替换原有的ChatWebSocketHandler
 */
@Component
public class EnhancedChatWebSocketHandler extends TextWebSocketHandler implements MessageListener {

    /** 跨实例推送频道：sendToUser publish 到这里，所有实例订阅 */
    public static final String WS_PUSH_CHANNEL = "ws:push";

    /** 全局在线用户集合（Redis Set），跨实例共享 */
    public static final String WS_ONLINE_KEY = "ws:online";

    /** 心跳间隔（毫秒） */
    private static final long HEARTBEAT_INTERVAL_MS = 30000; // 30秒

    /** 心跳超时（毫秒） */
    private static final long HEARTBEAT_TIMEOUT_MS = 90000; // 90秒（3个心跳周期）

    /** 消息ACK超时（毫秒） */
    private static final long MESSAGE_ACK_TIMEOUT_MS = 30000; // 30秒

    /** 最大重试次数 */
    private static final int MAX_RETRY_COUNT = 3;

    // 本实例在线用户列表：key=userId，value=连接会话
    private static final ConcurrentMap<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    /** 会话ID到用户ID的映射 */
    private static final ConcurrentMap<String, Long> sessionToUserMap = new ConcurrentHashMap<>();

    /** 心跳跟踪：记录最后一次收到消息的时间 */
    private static final ConcurrentMap<String, Long> sessionHeartbeats = new ConcurrentHashMap<>();

    @Autowired
    private ChatService chatService;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${spring.rabbitmq.enabled:true}")
    private boolean rabbitEnabled;

    @Autowired
    private WebSocketConnectionManager connectionManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 心跳线程池 */
    private final ScheduledExecutorService heartbeatScheduler;

    /** 消息ACK跟踪：serverMessageId -> 投递状态 */
    private final ConcurrentMap<String, MessageDeliveryState> pendingMessages = new ConcurrentHashMap<>();

    public EnhancedChatWebSocketHandler() {
        // 初始化心跳检查线程池
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "WebSocket-Heartbeat-Checker");
            t.setDaemon(true);
            return t;
        });

        // 启动心跳检查任务（每30秒检查一次）
        heartbeatScheduler.scheduleAtFixedRate(this::checkHeartbeats,
            HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);

        // 启动ACK超时检查任务（每10秒检查一次）
        heartbeatScheduler.scheduleAtFixedRate(this::checkAckTimeouts,
            10000, 10000, TimeUnit.MILLISECONDS);
    }

    @Override //用户成功建立连接时完成会话初始化
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从WebSocket会话属性中获取用户ID
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Authentication failed"));
            return;
        }

        // 获取会话ID
        String sessionId = session.getId();

        // 检查连接数限制
        if (!connectionManager.canAcceptConnection(userId, sessionId)) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Connection limit reached"));
            return;
        }

        // 注册连接管理器
        connectionManager.registerConnection(userId, sessionId, session);

        // 保存会话映射
        userSessions.put(userId, session);
        sessionToUserMap.put(sessionId, userId);
        sessionHeartbeats.put(sessionId, System.currentTimeMillis());

        // 全局在线登记：任意实例都能查到该用户在线
        stringRedisTemplate.opsForSet().add(WS_ONLINE_KEY, userId.toString());

        // 发送连接成功的ACK
        sendConnectionAck(session);

        System.out.println("WebSocket connection established: userId=" + userId + ", sessionId=" + sessionId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            return;
        }

        String sessionId = session.getId();

        // 更新心跳时间
        sessionHeartbeats.put(sessionId, System.currentTimeMillis());

        try {
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());
            String action = jsonNode.get("action").asText();

            switch (action) {
                case "send":
                    handleSendMessageWithAck(userId, jsonNode, session);
                    break;
                case "read":
                    handleMarkAsRead(userId, jsonNode);
                    break;
                case "heartbeat":
                    handleHeartbeat(session);
                    break;
                case "ack":
                    handleMessageAck(userId, jsonNode);
                    break;
                default:
                    sendError(session, "Unknown action type: " + action);
                    break;
            }

            // 记录消息处理
            connectionManager.recordMessageProcessed();

        } catch (Exception e) {
            connectionManager.recordError();
            sendError(session, "Message format error: " + e.getMessage());
        }
    }

    /**
     * 增强的消息发送：带ACK机制
     */
    private void handleSendMessageWithAck(Long senderId, JsonNode jsonNode, WebSocketSession session)
            throws IOException {
        String sessionId = session.getId();
        String clientMessageId = jsonNode.has("messageId") ?
            jsonNode.get("messageId").asText() : UUID.randomUUID().toString();

        Long conversationId = jsonNode.get("conversationId").asLong();
        String content = jsonNode.get("content").asText();
        String type = jsonNode.has("type") ? jsonNode.get("type").asText() : "TEXT";

        // 1. 调用 Service 保存消息到数据库（持久化，离线也能看到）
        Result<MessageDTO> result = chatService.sendMessage(conversationId, senderId, content, type);

        if (result.getCode() == 200) {
            MessageDTO messageDTO = result.getData();

            // 生成服务端消息ID（用于ACK跟踪）
            String serverMessageId = "msg-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString()
                .substring(0, 8);

            // 2. 发送ACK给发送者
            Map<String, Object> ackPayload = Map.of(
                "action", "ack",
                "clientMessageId", clientMessageId,
                "serverMessageId", serverMessageId,
                "data", messageDTO,
                "timestamp", System.currentTimeMillis()
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ackPayload)));

            // 3. 构建消息负载
            Map<String, Object> payload = Map.of(
                "action", "message",
                "data", messageDTO,
                "serverMessageId", serverMessageId
            );

            // 4. 发给发送者自己（本实例，直接推）- 包含serverMessageId
            Map<String, Object> selfPayload = Map.of(
                "action", "message",
                "data", messageDTO,
                "serverMessageId", serverMessageId,
                "isSelf", true
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(selfPayload)));

            // 5. 发给接收者：跨实例，经 Redis Pub/Sub 由持有 session 的实例送达
            Long receiverId = messageDTO.getReceiverId();
            if (isOnline(receiverId)) {
                // 记录消息投递状态（等待ACK）
                pendingMessages.put(serverMessageId,
                    new MessageDeliveryState(serverMessageId, senderId, receiverId, payload));

                sendToUser(receiverId, payload);
            } else if (rabbitEnabled && rabbitTemplate != null) {
                // 全局离线：异步落库一条"新消息"通知
                ChatOfflineMessage offline = new ChatOfflineMessage();
                offline.setReceiverId(receiverId);
                offline.setSenderId(senderId);
                offline.setConversationId(conversationId);
                offline.setContent(content);
                offline.setServerMessageId(serverMessageId);
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME,
                        RabbitMQConfig.CHAT_OFFLINE_ROUTING_KEY, offline);

                // 离线消息不需要等待ACK
                pendingMessages.remove(serverMessageId);
            }
        } else {
            // 发送失败，返回错误ACK
            Map<String, Object> errorAck = Map.of(
                "action", "ack",
                "clientMessageId", clientMessageId,
                "error", result.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorAck)));
        }
    }

    /**
     * 处理消息ACK
     */
    private void handleMessageAck(Long userId, JsonNode jsonNode) {
        if (!jsonNode.has("serverMessageId")) {
            return;
        }

        String serverMessageId = jsonNode.get("serverMessageId").asText();
        boolean success = jsonNode.has("success") && jsonNode.get("success").asBoolean();

        // 从待处理消息中移除
        MessageDeliveryState state = pendingMessages.remove(serverMessageId);
        if (state != null) {
            System.out.println("Message ACK received: messageId=" + serverMessageId +
                ", success=" + success + ", receiver=" + userId);
        }
    }

    /**
     * 处理心跳
     */
    private void handleHeartbeat(WebSocketSession session) throws IOException {
        String sessionId = session.getId();
        sessionHeartbeats.put(sessionId, System.currentTimeMillis());

        // 返回心跳响应
        Map<String, Object> heartbeatResponse = Map.of(
            "action", "heartbeat",
            "timestamp", System.currentTimeMillis()
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(heartbeatResponse)));
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
        if (userId == null) {
            return;
        }

        String sessionId = session.getId();

        // 从各种映射中移除
        userSessions.remove(userId);
        sessionToUserMap.remove(sessionId);
        sessionHeartbeats.remove(sessionId);

        // 注销连接管理器
        connectionManager.unregisterConnection(userId, sessionId);

        // 注销全局在线：检查是否还有其他会话
        boolean hasOtherSessions = sessionToUserMap.values().stream()
            .anyMatch(id -> id.equals(userId));

        if (!hasOtherSessions) {
            stringRedisTemplate.opsForSet().remove(WS_ONLINE_KEY, userId.toString());
        }

        System.out.println("WebSocket connection closed: userId=" + userId +
            ", sessionId=" + sessionId + ", status=" + status);
    }

    /**
     * 发送连接成功ACK
     */
    private void sendConnectionAck(WebSocketSession session) throws IOException {
        Map<String, Object> ackPayload = Map.of(
            "action", "connected",
            "sessionId", session.getId(),
            "heartbeatInterval", HEARTBEAT_INTERVAL_MS,
            "timestamp", System.currentTimeMillis()
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ackPayload)));
    }

    /**
     * 检查心跳，关闭超时连接
     */
    private void checkHeartbeats() {
        long now = System.currentTimeMillis();
        long timeoutThreshold = now - HEARTBEAT_TIMEOUT_MS;

        sessionHeartbeats.entrySet().removeIf(entry -> {
            String sessionId = entry.getKey();
            long lastHeartbeat = entry.getValue();

            if (lastHeartbeat < timeoutThreshold) {
                // 心跳超时，关闭连接
                Long userId = sessionToUserMap.get(sessionId);
                if (userId != null) {
                    WebSocketSession session = userSessions.get(userId);
                    if (session != null && session.isOpen()) {
                        try {
                            session.close(CloseStatus.SESSION_NOT_RELIABLE.withReason("Heartbeat timeout"));
                            System.out.println("Heartbeat timeout, closing session: " + sessionId);
                        } catch (IOException e) {
                            System.err.println("Error closing session: " + e.getMessage());
                        }
                    }
                }
                return true; // 从map中移除
            }
            return false;
        });
    }

    /**
     * 检查ACK超时，重发消息
     */
    private void checkAckTimeouts() {
        long now = System.currentTimeMillis();
        long timeoutThreshold = now - MESSAGE_ACK_TIMEOUT_MS;

        pendingMessages.entrySet().removeIf(entry -> {
            MessageDeliveryState state = entry.getValue();

            if (state.getCreatedTime() < timeoutThreshold && state.getRetryCount() < MAX_RETRY_COUNT) {
                // 重发消息
                state.incrementRetryCount();
                sendToUser(state.getReceiverId(), state.getPayload());
                System.out.println("Resending message (retry " + state.getRetryCount() + "): " + state.getMessageId());
                return false; // 不移除，等待下次ACK
            } else if (state.getCreatedTime() < timeoutThreshold) {
                // 超过最大重试次数，放弃
                System.out.println("Message delivery failed after max retries: " + state.getMessageId());
                return true; // 从map中移除
            }
            return false;
        });
    }

    /**
     * 跨实例推送入口：把消息 publish 到 ws:push 频道，由持有目标 session 的实例本地投递。
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
            System.err.println("Error sending message to user " + userId + ": " + e.getMessage());
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
            System.err.println("Error processing Redis message: " + e.getMessage());
        }
    }

    private void deliverLocally(Long userId, String messageJson) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(messageJson));
            } catch (IOException e) {
                System.err.println("Error delivering message to user " + userId + ": " + e.getMessage());
            }
        }
    }

    private boolean isOnline(Long userId) {
        if (userId == null) {
            return false;
        }
        Boolean member = stringRedisTemplate.opsForSet().isMember(WS_ONLINE_KEY, userId.toString());
        return Boolean.TRUE.equals(member);
    }

    /**
     * 消息投递状态跟踪
     */
    private static class MessageDeliveryState {
        private final String messageId;
        private final Long senderId;
        private final Long receiverId;
        private final Map<String, Object> payload;
        private final long createdTime;
        private int retryCount;

        public MessageDeliveryState(String messageId, Long senderId, Long receiverId, Map<String, Object> payload) {
            this.messageId = messageId;
            this.senderId = senderId;
            this.receiverId = receiverId;
            this.payload = payload;
            this.createdTime = System.currentTimeMillis();
            this.retryCount = 0;
        }

        public String getMessageId() { return messageId; }
        public Long getSenderId() { return senderId; }
        public Long getReceiverId() { return receiverId; }
        public Map<String, Object> getPayload() { return payload; }
        public long getCreatedTime() { return createdTime; }
        public int getRetryCount() { return retryCount; }
        public void incrementRetryCount() { this.retryCount++; }
    }
}