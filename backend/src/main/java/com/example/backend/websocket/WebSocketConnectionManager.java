package com.example.backend.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebSocket连接管理器，用于监控和管理连接状态
 * 提供连接数控制、心跳检查、性能监控等功能
 */
@Component
public class WebSocketConnectionManager {

    private final AtomicInteger activeConnections = new AtomicInteger(0);
    private final AtomicLong totalMessagesProcessed = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);

    // 用户连接统计
    private final ConcurrentMap<Long, UserConnectionStats> userStats = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, WebSocketSession> sessionById = new ConcurrentHashMap<>();

    // 配置参数
    private final int maxConnectionsPerInstance = 5000; // 单实例最大连接数
    private final int maxConnectionsPerUser = 10;      // 单用户最大连接数
    private final long connectionRateLimitMs = 1000;   // 连接频率限制（1秒）

    /**
     * 检查是否可以接受新连接
     */
    public synchronized boolean canAcceptConnection(Long userId, String sessionId) {
        // 1. 实例级连接数控制
        if (activeConnections.get() >= maxConnectionsPerInstance) {
            return false;
        }

        // 2. 用户级连接数控制
        UserConnectionStats stats = userStats.computeIfAbsent(userId,
            k -> new UserConnectionStats());

        if (stats.getActiveConnections() >= maxConnectionsPerUser) {
            return false;
        }

        // 3. 连接频率控制（防止连接风暴）
        long currentTime = System.currentTimeMillis();
        if (currentTime - stats.getLastConnectionTime() < connectionRateLimitMs) {
            if (stats.getConnectionCountLastSecond() > 3) {
                return false;
            }
        }

        return true;
    }

    /**
     * 注册新连接
     */
    public synchronized void registerConnection(Long userId, String sessionId,
                                              WebSocketSession session) {
        activeConnections.incrementAndGet();

        UserConnectionStats stats = userStats.computeIfAbsent(userId,
            k -> new UserConnectionStats());
        stats.addConnection(sessionId);
        sessionById.put(sessionId, session);
    }

    /**
     * 注销连接
     */
    public synchronized void unregisterConnection(Long userId, String sessionId) {
        activeConnections.decrementAndGet();

        UserConnectionStats stats = userStats.get(userId);
        if (stats != null) {
            stats.removeConnection(sessionId);
            if (stats.getActiveConnections() == 0) {
                userStats.remove(userId);
            }
        }
        sessionById.remove(sessionId);
    }

    /**
     * 记录消息处理
     */
    public void recordMessageProcessed() {
        totalMessagesProcessed.incrementAndGet();
    }

    /**
     * 记录错误
     */
    public void recordError() {
        totalErrors.incrementAndGet();
    }

    /**
     * 获取监控指标
     */
    public ConnectionMetrics getMetrics() {
        ConnectionMetrics metrics = new ConnectionMetrics();
        metrics.setActiveConnections(activeConnections.get());
        metrics.setTotalMessagesProcessed(totalMessagesProcessed.get());
        metrics.setTotalErrors(totalErrors.get());
        metrics.setUniqueUsers(userStats.size());

        // 计算错误率
        long totalMessages = totalMessagesProcessed.get();
        if (totalMessages > 0) {
            double errorRate = (double) totalErrors.get() / totalMessages * 100;
            metrics.setErrorRate(errorRate);
        } else {
            metrics.setErrorRate(0.0);
        }

        return metrics;
    }

    /**
     * 用户连接统计内部类
     */
    private static class UserConnectionStats {
        private final ConcurrentMap<String, Long> connections = new ConcurrentHashMap<>();
        private long lastConnectionTime = 0;

        public void addConnection(String sessionId) {
            connections.put(sessionId, System.currentTimeMillis());
            lastConnectionTime = System.currentTimeMillis();
        }

        public void removeConnection(String sessionId) {
            connections.remove(sessionId);
        }

        public int getActiveConnections() {
            return connections.size();
        }

        public long getLastConnectionTime() {
            return lastConnectionTime;
        }

        public int getConnectionCountLastSecond() {
            long now = System.currentTimeMillis();
            long oneSecondAgo = now - 1000;

            return (int) connections.values().stream()
                .filter(timestamp -> timestamp > oneSecondAgo)
                .count();
        }
    }

    /**
     * 连接监控指标类
     */
    public static class ConnectionMetrics {
        private int activeConnections;
        private long totalMessagesProcessed;
        private long totalErrors;
        private int uniqueUsers;
        private double errorRate;

        // Getters and setters
        public int getActiveConnections() { return activeConnections; }
        public void setActiveConnections(int activeConnections) { this.activeConnections = activeConnections; }

        public long getTotalMessagesProcessed() { return totalMessagesProcessed; }
        public void setTotalMessagesProcessed(long totalMessagesProcessed) { this.totalMessagesProcessed = totalMessagesProcessed; }

        public long getTotalErrors() { return totalErrors; }
        public void setTotalErrors(long totalErrors) { this.totalErrors = totalErrors; }

        public int getUniqueUsers() { return uniqueUsers; }
        public void setUniqueUsers(int uniqueUsers) { this.uniqueUsers = uniqueUsers; }

        public double getErrorRate() { return errorRate; }
        public void setErrorRate(double errorRate) { this.errorRate = errorRate; }

        @Override
        public String toString() {
            return String.format(
                "ConnectionMetrics{active=%d, messages=%d, errors=%d, users=%d, errorRate=%.2f%%}",
                activeConnections, totalMessagesProcessed, totalErrors, uniqueUsers, errorRate
            );
        }
    }
}