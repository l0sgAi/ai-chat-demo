package com.losgai.ai.entity.sys;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai-chat-demo.minio")
public class MinioProperties { //读取minio相关配置项
    private String endpointUrl;
    private String accessKey;
    private String secretKey;
    private String bucketName;
}