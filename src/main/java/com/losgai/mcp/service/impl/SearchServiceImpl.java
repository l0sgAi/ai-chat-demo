package com.losgai.mcp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.losgai.mcp.global.SseEmitterManager;
import com.losgai.mcp.service.SearchService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    private final WebClient webClient;

//    private final SseEmitterManager sseEmitterManager;

    private static final String baseUrl = "https://api.tavily.com";

    private static final String bearerToken = "tvly-dev-***";

    public SearchServiceImpl(
            SseEmitterManager sseEmitterManager) {
//        this.sseEmitterManager = sseEmitterManager;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    @Tool(name = "webSearch", description = "Searching information from the internet")
    public String search(
            @ToolParam(description = "搜索关键词")
            String query) {

        if (StrUtil.isBlank(query)) {
            return "nothing";
        }

        // 构建请求体
        SearchRequest req = new SearchRequest();
        req.setQuery(query);
        req.setTopic("general");
        req.setSearchDepth("basic");
        req.setChunksPerSource(3);
        req.setMaxResults(5);
        req.setTimeRange(null);
        req.setDays(3);
        req.setIncludeAnswer(true);
        req.setIncludeRawContent(false);
        req.setIncludeImages(false);
        req.setIncludeImageDescriptions(false);
        req.setIncludeDomains(Collections.emptyList());
        req.setExcludeDomains(Collections.emptyList());

        try {
            // 发送请求并等待响应（同步风格）
            SearchResponse resp = webClient.post()
                    .uri("/search")
                    .header("Authorization", "Bearer " + bearerToken)
                    .header("Content-Type", "application/json")
                    .bodyValue(req)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            ClientResponse::createException)
                    .bodyToMono(SearchResponse.class)
                    .block(Duration.ofSeconds(15)); // 超时时间可调整

            if (resp == null) {
                return "No results returned.";
            }

            // 构建输出文本
            StringBuilder out = new StringBuilder();
            out.append("Query: ").append(resp.getQuery()).append("\n\n");

            if (resp.getAnswer() != null && !resp.getAnswer().isBlank()) {
                out.append("Answer (summary):\n").append(resp.getAnswer()).append("\n\n");
            }

            List<SearchResult> results = resp.getResults();
            if (results == null || results.isEmpty()) {
                out.append("No results returned.");
            } else {
                out.append("Top results:\n");
                int idx = 1;
                for (SearchResult r : results.stream().limit(5).toList()) {
                    out.append(idx++).append(". ").append(safe(r.getTitle())).append("\n");
                    out.append(" URL: ").append(safe(r.getUrl())).append("\n");
                    if (r.getContent() != null && !r.getContent().isBlank()) {
                        // 截断content到合理长度以免返回过长文本
                        String snip = r.getContent().length() > 400 ? r.getContent().substring(0, 400) + "..." : r.getContent();
                        out.append(" Snippet: ").append(snip).append("\n");
                    }
                    out.append(" Score: ").append(r.getScore()).append("\n\n");
                }
            }

            out.append("API response_time: ").append(safe(resp.getResponseTime())).append("s");
            // 发送结果
//            sseEmitterManager.getEmitter(DEFAULT_EMITTER_ID).send(out.toString(), MediaType.TEXT_PLAIN);
            return out.toString();
        } catch (Exception ex) {
            log.error("搜索服务错误: ", ex);
            return "Error: " + ex.getMessage();
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    @Setter
    @Getter
    public static class SearchRequest {
        // getters / setters
        private String query;
        private String topic;
        @JsonProperty("search_depth")
        private String searchDepth;
        @JsonProperty("chunks_per_source")
        private Integer chunksPerSource;
        @JsonProperty("max_results")
        private Integer maxResults;
        @JsonProperty("time_range")
        private Object timeRange;
        private Integer days;
        @JsonProperty("include_answer")
        private Boolean includeAnswer;
        @JsonProperty("include_raw_content")
        private Boolean includeRawContent;
        @JsonProperty("include_images")
        private Boolean includeImages;
        @JsonProperty("include_image_descriptions")
        private Boolean includeImageDescriptions;
        @JsonProperty("include_domains")
        private List<String> includeDomains;
        @JsonProperty("exclude_domains")
        private List<String> excludeDomains;

    }

    @Setter
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchResponse {
        // getters / setters
        private String query;
        private String answer;
        private List<String> images;
        private List<SearchResult> results;
        @JsonProperty("response_time")
        private String responseTime;

    }

    @Setter
    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchResult {
        // getters / setters
        private String title;
        private String url;
        private String content;
        private Double score;
        @JsonProperty("raw_content")
        private String rawContent;
    }

}
