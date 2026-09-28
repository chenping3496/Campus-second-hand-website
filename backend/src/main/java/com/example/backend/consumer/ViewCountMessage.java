package com.example.backend.consumer;

import lombok.Data;

/**
 * 浏览计数事件：一次商品详情访问投递一条消息到 view.count.queue，
 * 由 ViewCountConsumer 单消费者串行消费、按 productId 合并 delta，定时批量回写 DB。
 */
@Data
public class ViewCountMessage {
    private Long productId;

    public ViewCountMessage() {}

    public ViewCountMessage(Long productId) {
        this.productId = productId;
    }
}
