package com.losgai.ai;

import com.losgai.ai.entity.sys.MinioProperties;
import org.springframework.ai.vectorstore.elasticsearch.autoconfigure.ElasticsearchVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(exclude = {
        ElasticsearchVectorStoreAutoConfiguration.class  // 排除自动配置
})
@EnableCaching
@EnableConfigurationProperties(MinioProperties.class)
public class AiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }

}
