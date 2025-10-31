package com.losgai.ai.config;

import com.losgai.ai.entity.ai.AiConfig;
import com.losgai.ai.mapper.AiConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * 动态加载数据库中的文本嵌入模型配置
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiEmbeddingConfig {

    private final AiConfigMapper aiConfigMapper;

    private static final Integer id = 2;

    /**
     * 注册一个 EmbeddingModel Bean，用于 SpringAI VectorStore
     */
    @Bean
    @Lazy // 延迟加载，避免启动时数据库尚未准备好
    public EmbeddingModel embeddingModel() throws IllegalStateException {
        // 从数据库读取 id=2 的配置（文本嵌入模型）
        AiConfig config = aiConfigMapper.selectByPrimaryKey(id);
        if (config == null) {
            log.error("未找到ID={}的嵌入模型配置", id);
        }
        if (config != null && config.getIsEnabled() != null && config.getIsEnabled() == 1) {
            log.error("ID={}的嵌入模型嵌入模型已禁用", id);
        }

        // 根据 apiDomain 判定使用哪家服务
        String domain = null;
        String apiKey = null;
        String modelName = null;
        if (config != null) {
            domain = config.getApiDomain();
            apiKey = config.getApiKey();
            modelName = config.getModelId();
        }

        //构造 EmbeddingOptions
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(modelName)
                .dimensions(1536)
                //.user("模型对用户的称呼")
                .build();

        OpenAiApi openAiApi = OpenAiApi.builder()
                // 填入自己的API KEY
                .apiKey(apiKey)
                // 填入自己的API域名，如果是百炼，即为https://dashscope.aliyuncs.com/compatible-mode
                // 注意：这里与langchain4j的配置不同，不需要在后面加/v1
                .baseUrl(domain)
                .completionsPath("/chat/completions")
                .embeddingsPath("/embeddings")
                .build();

        return new OpenAiEmbeddingModel(
                openAiApi,
                MetadataMode.EMBED,
                options);
    }
}