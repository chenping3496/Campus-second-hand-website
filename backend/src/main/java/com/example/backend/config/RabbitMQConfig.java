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

    // 死信交换机：消费重试耗尽（default-requeue-rejected=false）后，消息按各队列的
    // x-dead-letter-routing-key 路由到对应死信队列，避免毒消息无限 requeue。
    public static final String DLX_NAME = "ershou.dlx";

    // 1. 通知落库
    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "notification";
    public static final String NOTIFICATION_DLQ = "notification.dlq";

    // 2. 通知实时推送（在线 WebSocket）
    public static final String NOTIFICATION_PUSH_QUEUE = "notification.push.queue";
    public static final String NOTIFICATION_PUSH_ROUTING_KEY = "notification.push";
    public static final String NOTIFICATION_PUSH_DLQ = "notification.push.dlq";

    // 3. 订单事件（商品状态回滚等）
    public static final String ORDER_EVENT_QUEUE = "order.event.queue";
    public static final String ORDER_EVENT_ROUTING_KEY = "order.event";
    public static final String ORDER_EVENT_DLQ = "order.event.dlq";

    // 4. 聊天离线通知
    public static final String CHAT_OFFLINE_QUEUE = "chat.offline.queue";
    public static final String CHAT_OFFLINE_ROUTING_KEY = "chat.offline";
    public static final String CHAT_OFFLINE_DLQ = "chat.offline.dlq";

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME);
    }

    // 主队列：绑定死信交换机，消费失败到上限后被 reject（不 requeue）→ 进死信队列
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ)
                .build();
    }

    @Bean
    public Queue notificationPushQueue() {
        return QueueBuilder.durable(NOTIFICATION_PUSH_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_PUSH_DLQ)
                .build();
    }

    @Bean
    public Queue orderEventQueue() {
        return QueueBuilder.durable(ORDER_EVENT_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", ORDER_EVENT_DLQ)
                .build();
    }

    @Bean
    public Queue chatOfflineQueue() {
        return QueueBuilder.durable(CHAT_OFFLINE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", CHAT_OFFLINE_DLQ)
                .build();
    }

    // 死信队列：存放重试耗尽的消息，可接专门消费者做告警 / 人工排查
    @Bean
    public Queue notificationDeadQueue() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    @Bean
    public Queue notificationPushDeadQueue() {
        return QueueBuilder.durable(NOTIFICATION_PUSH_DLQ).build();
    }

    @Bean
    public Queue orderEventDeadQueue() {
        return QueueBuilder.durable(ORDER_EVENT_DLQ).build();
    }

    @Bean
    public Queue chatOfflineDeadQueue() {
        return QueueBuilder.durable(CHAT_OFFLINE_DLQ).build();
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
    public Binding notificationDeadBinding() {
        return BindingBuilder.bind(notificationDeadQueue()).to(deadLetterExchange()).with(NOTIFICATION_DLQ);
    }

    @Bean
    public Binding notificationPushDeadBinding() {
        return BindingBuilder.bind(notificationPushDeadQueue()).to(deadLetterExchange()).with(NOTIFICATION_PUSH_DLQ);
    }

    @Bean
    public Binding orderEventDeadBinding() {
        return BindingBuilder.bind(orderEventDeadQueue()).to(deadLetterExchange()).with(ORDER_EVENT_DLQ);
    }

    @Bean
    public Binding chatOfflineDeadBinding() {
        return BindingBuilder.bind(chatOfflineDeadQueue()).to(deadLetterExchange()).with(CHAT_OFFLINE_DLQ);
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
