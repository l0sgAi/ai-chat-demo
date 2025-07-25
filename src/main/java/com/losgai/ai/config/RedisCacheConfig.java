package com.losgai.ai.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching // 开启Spring Cache
public class RedisCacheConfig {

    // 定义 userInfo 缓存的名称
    private static final String USER_INFO_CACHE_NAME = "userInfo";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        // 1. 配置默认缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                // 设置key的序列化方式为String
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // 设置value的序列化方式为JSON
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer()))
                // 默认缓存过期时间（例如，1天）
                .entryTtl(Duration.ofDays(1));

        // 2. 针对 userInfo 缓存进行特定配置
        RedisCacheConfiguration userInfoCacheConfig = defaultConfig
                // 设置 userInfo 缓存的过期时间为3小时
                .entryTtl(Duration.ofHours(3));

        // 3. 将特定配置应用到对应的缓存名称
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(USER_INFO_CACHE_NAME, userInfoCacheConfig);
        // 可以为其他缓存添加更多特定配置
        // cacheConfigurations.put("otherCache", otherConfig);

        // 4. 构建并返回 RedisCacheManager
        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig) // 应用默认配置
                .withInitialCacheConfigurations(cacheConfigurations) // 应用特定配置
                .build();
    }
    
    /**
     * 配置Jackson2JsonRedisSerializer序列化器，用于将对象序列化为JSON格式存储在Redis中
     */
    private GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
