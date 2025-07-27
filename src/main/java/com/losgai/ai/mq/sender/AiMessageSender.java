package com.losgai.ai.mq.sender;

import com.losgai.ai.entity.ai.AiMessagePair;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiMessageSender {

    private final RabbitTemplate rabbitTemplate;

    // 发送方法，可以自定义 routingKey 和 exchange
    public void sendMessage(String exchange, String routingKey, AiMessagePair message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}