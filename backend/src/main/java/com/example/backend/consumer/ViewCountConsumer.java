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

/**
 * 使用RabbitMQ监听视图计数队列的方法
 * 当有商品浏览事件发生时，会触发此方法进行处理
 *
 * @param message 包含商品ID和浏览信息的消息对象
 */
    @RabbitListener(queues = RabbitMQConfig.VIEW_COUNT_QUEUE, concurrency = "1")
    public void handleViewEvent(ViewCountMessage message) {
    // 检查消息或商品ID是否为空，如果是则直接返回不做处理
        if (message == null || message.getProductId() == null) {
            return;
        }
    // 使用computeIfAbsent方法将商品ID作为键，对应的AtomicLong作为值
    // 如果商品ID不存在，则创建一个新的AtomicLong(0)
    // 然后调用incrementAndGet方法增加计数
        pending.computeIfAbsent(message.getProductId(), k -> new AtomicLong(0))
                .incrementAndGet();
    }

    /**
     * 每 5 秒把累计 delta 批量回写 DB。
     * getAndSet(0) 原子取出并清零：消费线程在本轮回写期间新增的 delta 留到下一轮，不丢。
     */
    @Scheduled(fixedDelay = 5000)  // 使用 Spring 的定时任务注解，每 5 秒执行一次
    public void flushToDb() {  // 方法名：批量刷新数据到数据库
        if (pending.isEmpty()) {  // 检查是否有待处理的数据
            return;  // 如果没有待处理数据，直接返回
        }
        // 快照 key 集合，避免长时间持有迭代器
        var entries = new ArrayList<Map.Entry<Long, AtomicLong>>(pending.size());  // 创建一个 ArrayList 来存储待处理的条目
        pending.entrySet().iterator().forEachRemaining(entries::add);  // 将 pending 中的所有条目添加到 entries 列表中
        for (Map.Entry<Long, AtomicLong> e : entries) {  // 遍历所有条目
            long delta = e.getValue().getAndSet(0);  // 原子性地获取当前值并设置为 0
            if (delta <= 0) {  // 如果 delta 小于等于 0，跳过处理
                continue;
            }
            int rows = productMapper.incrementViewCount(e.getKey(), delta);  // 调用 productMapper 更新数据库中的商品浏览次数
            if (rows == 0) {  // 如果影响的行数为 0，说明商品可能已被删除或不存在
                // 商品可能已逻辑删除或不存在，丢弃该 delta
                log.debug("incrementViewCount affected 0 rows, productId={}, delta={}", e.getKey(), delta);  // 记录调试日志
            }
        }
    }
}
