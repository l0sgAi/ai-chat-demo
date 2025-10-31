package com.losgai.ai.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;

import java.util.List;

@Configuration
public class RabbitMQAiMessageConfig {

    public static final String EXCHANGE_NAME = "ai.exchange";
    public static final String QUEUE_NAME = "ai.message.queue";
    public static final String ROUTING_KEY = "ai.message";

    // 默认的直接交换机
    @Bean
    public Exchange exchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_NAME).durable(true).build();
    }

    // ai消息同步队列
    @Bean
    public Queue queue() {
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    // 绑定队列和交换机
    @Bean
    public Binding binding(Queue queue, Exchange exchange) {
        return BindingBuilder.bind(queue).to((DirectExchange) exchange).with(ROUTING_KEY);
    }

    // 反序列化配置
//    @Bean
//    public SimpleMessageConverter converter() {
//        SimpleMessageConverter converter = new SimpleMessageConverter();
//        converter.setAllowedListPatterns(List.of("com.losgai.ai.entity.*", "java.util.*"));
//        return converter;
//    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);

        // 设置重试拦截器（最多3次）
        factory.setAdviceChain(retryInterceptor());
        return factory;
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    public RetryOperationsInterceptor retryInterceptor() {
        return RetryInterceptorBuilder
                .stateless()
                .maxAttempts(3) // 最多重试3次
                .backOffOptions(1000, 2.0, 10000) // 初始延迟1s，倍数2.0，最大10s
                .recoverer(new RejectAndDontRequeueRecoverer()) // 超过重试次数后丢弃，不重新入队
                .build();
    }

}