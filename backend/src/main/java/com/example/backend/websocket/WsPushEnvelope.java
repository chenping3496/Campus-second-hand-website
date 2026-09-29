package com.example.backend.websocket;

import lombok.Data;

/**
 * WebSocket 跨实例推送信封。
 * <p>
 * sendToUser 不再直接查本地 Map，而是把 {userId, messageJson} publish 到 Redis 的 ws:push 频道；
 * 每个实例都订阅该频道，收到后只对本地持有 session 的 userId 真正 sendMessage，其余丢弃。
 */
@Data
public class WsPushEnvelope {
    /** 目标用户 ID */
    private Long userId;
    /** 已序列化好的、可直接发给 WebSocket 的消息 JSON */
    private String messageJson;
}
