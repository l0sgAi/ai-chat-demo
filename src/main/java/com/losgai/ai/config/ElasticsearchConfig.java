package com.losgai.ai.config;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String esUris;

    @Bean
    public ElasticsearchClient elasticsearchClient() throws IOException {
        // TODO 如果在设置里打开了认证，需要配置认证信息
        // 1. 创建 RestClient（无认证、无 SSL）
        RestClient restClient = org.elasticsearch.client.RestClient.builder(
                // 这里换成自己的ES服务器地址，如果是本地部署，直接localhost即可
                new HttpHost(esUris)).build();

        // 2. 使用 Jackson 映射器创建 Transport 层
        RestClientTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());


        // 3. 创建 Elasticsearch Java 客户端
        ElasticsearchClient elasticsearchClient = new ElasticsearchClient(transport);
        transport.close();
        return elasticsearchClient;
    }
}