package com.losgai.ai.config;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String esUris;

    @Bean
    public ElasticsearchClient elasticsearchClient() throws URISyntaxException {
        URI uri = new URI(esUris);
        // 提取主机和端口
        String host = uri.getHost();
        int port = uri.getPort() == -1 ? 9200 : uri.getPort();  // 默认端口为9200
        // TODO 如果在设置里打开了认证，需要配置认证信息
        // 1. 创建 RestClient（无认证、无 SSL）
        RestClient restClient = org.elasticsearch.client.RestClient.builder(
                // 这里换成自己的ES服务器地址，如果是本地部署，直接localhost即可
                new HttpHost(host, port)).build();

        // 2. 使用 Jackson 映射器创建 Transport 层
        RestClientTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());


        // 3. 创建 Elasticsearch Java 客户端
        return new ElasticsearchClient(transport);
    }

    @Bean
    public RestClient restClient() {
        return RestClient.builder(new HttpHost(
                        // 这里换成自己的ES服务器地址，如果是本地部署，直接localhost即可
                        "192.168.200.132",
                        9200,
                        "http"))
                .build();
    }
}