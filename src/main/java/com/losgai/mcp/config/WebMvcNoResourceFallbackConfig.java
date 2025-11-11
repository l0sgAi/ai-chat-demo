package com.losgai.mcp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcNoResourceFallbackConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 明确指定资源目录，避免无限 fallback
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}
