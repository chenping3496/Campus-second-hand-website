package com.example.backend.consumer;

import com.example.backend.config.RabbitMQConfig;
import com.example.backend.entity.Order;
import com.example.backend.entity.Product;
import com.example.backend.mapper.OrderMapper;
import com.example.backend.mapper.ProductMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 消费者 3：订单事件驱动商品状态回滚。由 OrderService.cancelOrder 投递 CANCELLED 事件。
 * <p>
 * acknowledge-mode=auto：正常返回即 ack；抛异常由 Spring 重试，耗尽后进死信队列。
 */
@Component
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderEventConsumer {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductMapper productMapper;

    @RabbitListener(queues = RabbitMQConfig.ORDER_EVENT_QUEUE)
    public void handleOrderEvent(OrderEventMessage event) {
        Order order = orderMapper.selectById(event.getOrderId());
        if (order == null) {
            return;
        }

        switch (event.getEventType()) {
            case "CANCELLED":
                Product product = productMapper.selectById(order.getProductId());
                if (product != null && product.getStatus() == Product.ProductStatus.SOLD) {
                    product.setStatus(Product.ProductStatus.ON_SALE);
                    productMapper.updateById(product);
                }
                break;
            default:
                break;
        }
    }
}
