package com.losgai.ai;

import com.losgai.ai.entity.AiConfig;
import com.losgai.ai.mapper.AiConfigMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
class AiApplicationTests {

    @Autowired
    private AiConfigMapper aiConfigMapper;

    /** 通过数据库获取配置，初始化模型进行输出
     * 使用langchain4j*/
    @Test
    void testLangChain4j() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);

        AiConfig aiConfig = aiConfigMapper.selectByPrimaryKey(1);

        // 构建模型对象，使用百炼 OpenAI 兼容模式
        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
                .apiKey(aiConfig.getApiKey()) // 这里也可以直接填你的apiKey
                .baseUrl(aiConfig.getApiDomain()) // 百炼兼容地址
                .modelName(aiConfig.getModelId()) // qwen-turbo比较便宜，测试用
                // qwen-plus、qwen-max、qwen-turbo 等
                .temperature(aiConfig.getSimilarityTopK()) // 温度，与输出的随机度有关
                .topP(aiConfig.getSimilarityTopP()) // 限制采样时选择的概率质量范围
                .maxTokens(aiConfig.getMaxContextMsgs()) // 最大输出token数量
                .build();

        // 创建一个流式响应处理器
        StreamingResponseHandler<AiMessage> handler = new StreamingResponseHandler<AiMessage>() {
            @Override
            public void onNext(String s) {
                log.info("***AI对话 onNext: {}", s);
            }

            @Override
            public void onComplete(Response response) {
                log.info("***AI对话 onComplete: {}", response.toString());
                // 停止倒计时
                latch.countDown();
            }

            @Override
            public void onError(Throwable error) {
                log.error("[❌ 错误] : {}", error.getMessage());
            }
        };

        // 这里可以换成其它任何问题
        model.generate("你是谁？", handler);

        // 等待响应完成，最多等待60秒
        latch.await(60, TimeUnit.SECONDS);
    }

}
