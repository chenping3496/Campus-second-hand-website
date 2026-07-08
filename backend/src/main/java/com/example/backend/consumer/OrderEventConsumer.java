package com.example.backend.consumer;

import com.example.backend.config.RabbitMQConfig;
import com.example.backend.entity.Order;
import com.example.backend.entity.Product;
import com.example.backend.mapper.OrderMapper;
import com.example.backend.mapper.ProductMapper;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OrderEventConsumer {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductMapper productMapper;

    @RabbitListener(queues = RabbitMQConfig.ORDER_EVENT_QUEUE)
    public void handleOrderEvent(OrderEventMessage event, Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            Order order = orderMapper.selectById(event.getOrderId());
            if (order == null) {
                channel.basicAck(deliveryTag, false);
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

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
