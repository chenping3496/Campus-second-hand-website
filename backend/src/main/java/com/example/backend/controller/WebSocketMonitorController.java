package com.example.backend.controller;

import com.example.backend.websocket.WebSocketConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket监控控制器
 * 提供WebSocket连接状态的监控端点
 */
@RestController
@RequestMapping("/admin/websocket")
public class WebSocketMonitorController {

    @Autowired
    private WebSocketConnectionManager connectionManager;

    /**
     * 获取当前WebSocket连接统计
     */
    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics() {
        try {
            WebSocketConnectionManager.ConnectionMetrics metrics = connectionManager.getMetrics();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "healthy");
            response.put("data", Map.of(
                "activeConnections", metrics.getActiveConnections(),
                "totalMessagesProcessed", metrics.getTotalMessagesProcessed(),
                "totalErrors", metrics.getTotalErrors(),
                "uniqueUsers", metrics.getUniqueUsers(),
                "errorRate", String.format("%.2f%%", metrics.getErrorRate())
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            WebSocketConnectionManager.ConnectionMetrics metrics = connectionManager.getMetrics();

            boolean isHealthy = true;
            String status = "healthy";

            // 检查连接数是否过高
            if (metrics.getActiveConnections() > 4000) { // 超过80%限制
                isHealthy = false;
                status = "warning-high-connections";
            }

            // 检查错误率是否过高
            if (metrics.getErrorRate() > 5.0) { // 超过5%错误率
                isHealthy = false;
                status = "warning-high-error-rate";
            }

            health.put("status", isHealthy ? "healthy" : "warning");
            health.put("details", status);
            health.put("timestamp", System.currentTimeMillis());
            health.put("metrics", Map.of(
                "activeConnections", metrics.getActiveConnections(),
                "errorRate", metrics.getErrorRate()
            ));

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            health.put("status", "unhealthy");
            health.put("details", "exception: " + e.getMessage());
            health.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(503).body(health);
        }
    }

    /**
     * 模拟连接测试（仅用于开发环境）
     */
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection(
            @RequestParam(defaultValue = "5") int count) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 模拟一些消息处理
            for (int i = 0; i < count; i++) {
                connectionManager.recordMessageProcessed();
            }

            result.put("status", "success");
            result.put("message", "Simulated " + count + " message deliveries");
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", "Test failed: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}