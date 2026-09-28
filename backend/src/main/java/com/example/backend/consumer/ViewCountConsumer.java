package com.example.backend.consumer;

import com.example.backend.config.RabbitMQConfig;
import com.example.backend.mapper.ProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 消费者 5：浏览计数 MQ 合并。
 * <p>
 * 每次商品详情访问投递一条 ViewCountMessage(productId) 到 view.count.queue。
 * 单消费者串行消费（concurrency=1），把 delta 累加进内存 Map；
 * 由 @Scheduled 定时把每个商品的累计 delta 批量回写 DB（一条 UPDATE 一个商品），
 * 避免每次浏览都同步写库。
 * <p>
 * 一致性：Redis 的 product:view:{id} 仍是展示用的实时计数（getProductDetail 直接读），
 * DB 的 view_count 列由本消费者定时追赶，滞后但最终一致；崩溃窗口内的 delta 会丢，
 * 对浏览数这种弱一致性指标可接受。重试耗尽的消息进死信队列（view.count.dlq）。
 */
@Component
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ViewCountConsumer {

    private static final Logger log = LoggerFactory.getLogger(ViewCountConsumer.class);

    /** 内存累加器：productId → 待回写的 delta。多线程安全。 */
    private final Map<Long, AtomicLong> pending = new ConcurrentHashMap<>();

    @Autowired
    private ProductMapper productMapper;

    @RabbitListener(queues = RabbitMQConfig.VIEW_COUNT_QUEUE, concurrency = "1")
    public void handleViewEvent(ViewCountMessage message) {
        if (message == null || message.getProductId() == null) {
            return;
        }
        pending.computeIfAbsent(message.getProductId(), k -> new AtomicLong(0))
                .incrementAndGet();
    }

    /**
     * 每 5 秒把累计 delta 批量回写 DB。
     * getAndSet(0) 原子取出并清零：消费线程在本轮回写期间新增的 delta 留到下一轮，不丢。
     */
    @Scheduled(fixedDelay = 5000)
    public void flushToDb() {
        if (pending.isEmpty()) {
            return;
        }
        // 快照 key 集合，避免长时间持有迭代器
        var entries = new ArrayList<Map.Entry<Long, AtomicLong>>(pending.size());
        pending.entrySet().iterator().forEachRemaining(entries::add);
        for (Map.Entry<Long, AtomicLong> e : entries) {
            long delta = e.getValue().getAndSet(0);
            if (delta <= 0) {
                continue;
            }
            int rows = productMapper.incrementViewCount(e.getKey(), delta);
            if (rows == 0) {
                // 商品可能已逻辑删除或不存在，丢弃该 delta
                log.debug("incrementViewCount affected 0 rows, productId={}, delta={}", e.getKey(), delta);
            }
        }
    }
}
