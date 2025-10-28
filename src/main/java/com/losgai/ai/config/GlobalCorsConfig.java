package com.losgai.ai.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 跨域配置
 * */
@Configuration
public class GlobalCorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NotNull CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(
                                "http://127.0.0.1:5173/ai-chat-demo/",
                                "http://127.0.0.1:5173",
                                "http://localhost:5173/ai-chat-demo/",
                                "http://localhost:5173",
                                "https://l0sgai.github.io",
                                "https://l0sgai.github.io/ai-chat-demo/"
                        )
                        .allowedMethods("*")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}
