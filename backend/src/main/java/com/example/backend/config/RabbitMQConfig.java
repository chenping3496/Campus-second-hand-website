package com.example.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "ershou.direct";

    // 1. 通知落库
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "notification";

    // 2. 通知实时推送（在线 WebSocket）
    public static final String NOTIFICATION_PUSH_QUEUE = "notification.push.queue";
    public static final String NOTIFICATION_PUSH_ROUTING_KEY = "notification.push";

    // 3. 订单事件（商品状态回滚等）
    public static final String ORDER_EVENT_QUEUE = "order.event.queue";
    public static final String ORDER_EVENT_ROUTING_KEY = "order.event";

    // 4. 聊天离线通知
    public static final String CHAT_OFFLINE_QUEUE = "chat.offline.queue";
    public static final String CHAT_OFFLINE_ROUTING_KEY = "chat.offline";

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Queue notificationPushQueue() {
        return QueueBuilder.durable(NOTIFICATION_PUSH_QUEUE).build();
    }

    @Bean
    public Queue orderEventQueue() {
        return QueueBuilder.durable(ORDER_EVENT_QUEUE).build();
    }

    @Bean
    public Queue chatOfflineQueue() {
        return QueueBuilder.durable(CHAT_OFFLINE_QUEUE).build();
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue()).to(directExchange()).with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding notificationPushBinding() {
        return BindingBuilder.bind(notificationPushQueue()).to(directExchange()).with(NOTIFICATION_PUSH_ROUTING_KEY);
    }

    @Bean
    public Binding orderEventBinding() {
        return BindingBuilder.bind(orderEventQueue()).to(directExchange()).with(ORDER_EVENT_ROUTING_KEY);
    }

    @Bean
    public Binding chatOfflineBinding() {
        return BindingBuilder.bind(chatOfflineQueue()).to(directExchange()).with(CHAT_OFFLINE_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
