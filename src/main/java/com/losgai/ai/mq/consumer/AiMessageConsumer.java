package com.losgai.ai.mq.consumer;

import com.losgai.ai.config.RabbitMQAiMessageConfig;
import com.losgai.ai.entity.ai.AiMessagePair;
import com.losgai.ai.global.EsConstants;
import com.losgai.ai.service.ai.AiMessagePairService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiMessageConsumer {

    private final AiMessagePairService aiMessagePairEsService;

    @RabbitListener(queues = RabbitMQAiMessageConfig.QUEUE_NAME)
    public void receiveMessage(AiMessagePair message) {
        log.info("[MQ]消费者收到消息：{}", message);
        boolean result = false;
        try {
            result = aiMessagePairEsService.insertAiMessagePairDoc(EsConstants.INDEX_NAME_AI_MSG, message);
            if (result) {
                log.info("[MQ]成功插入 ES，消息ID: {}", message.getId());
            } else {
                log.warn("[MQ]插入 ES 失败，消息ID: {}", message.getId());
            }
        } catch (IOException e) {
            log.error("[MQ]插入 ES 异常，消息ID: {}", message.getId(), e);
        }

    }
}