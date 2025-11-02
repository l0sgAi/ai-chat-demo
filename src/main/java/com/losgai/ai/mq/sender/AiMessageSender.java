package com.losgai.ai.mq.sender;

import com.losgai.ai.dto.RagStoreDto;
import com.losgai.ai.entity.ai.AiMessagePair;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AiMessageSender {

    private final RabbitTemplate rabbitTemplate;

    // 发送方法，可以自定义 routingKey 和 exchange
    public void sendMessage(
            String exchange,
            String routingKey,
            AiMessagePair message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }

    // 发送向量嵌入请求
    public void sendEmbeddingMessage(
            String exchange,
            String routingKey,
            List<Long> message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }

    // 发送向量嵌入请求:单个
    public void sendEmbeddingMessageSingle(
            String exchange,
            String routingKey,
            RagStoreDto message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}