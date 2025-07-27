package com.losgai.ai.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RabbitMQAiMessageConfig {

    public static final String EXCHANGE_NAME = "ai.exchange";
    public static final String QUEUE_NAME = "ai.message.queue";
    public static final String ROUTING_KEY = "ai.message";

    @Bean
    public Exchange exchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_NAME).durable(true).build();
    }

    @Bean
    public Queue queue() {
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    @Bean
    public Binding binding(Queue queue, Exchange exchange) {
        return BindingBuilder.bind(queue).to((DirectExchange) exchange).with(ROUTING_KEY);
    }

    // 反序列化配置
    @Bean
    public SimpleMessageConverter converter() {
        SimpleMessageConverter converter = new SimpleMessageConverter();
        converter.setAllowedListPatterns(List.of("com.losgai.ai.entity.*", "java.util.*"));
        return converter;
    }

}