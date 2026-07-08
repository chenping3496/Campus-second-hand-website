package com.example.backend.consumer;

import lombok.Data;

@Data
public class OrderEventMessage {
    private Long orderId;
    private String eventType; // CANCELLED, COMPLETED, etc.
}
