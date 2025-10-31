package com.losgai.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RagBodyDto {

    @Schema(description = "文档ID（业务唯一标识）", example = "doc_20231101_001")
    @NotBlank(message = "文档ID不能为空")
    private String docId;

    @Schema(description = "文档标题",example = "产品使用手册")
    @NotBlank(message = "文档标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255字符")
    private String title;

    @Schema(description = "文档内容")
    @NotBlank(message = "文档内容不能为空")
    private String content;

    @Schema(description = "文档类型", example = "pdf", allowableValues = {"txt", "pdf", "docx", "md", "html"})
    private String docType;

    @Schema(description = "文档来源", example = "https://example.com/doc.pdf")
    private String docSource;

    @Schema(description = "文档语言", example = "zh", defaultValue = "zh")
    private String language = "zh";

    @Schema(description = "是否需要分块", example = "true", defaultValue = "true")
    private Boolean needChunk = true;

    @Schema(description = "分块大小（字符数）", example = "500", defaultValue = "500")
    @Min(value = 100, message = "分块大小不能小于100")
    @Max(value = 2000, message = "分块大小不能大于2000")
    private Integer chunkSize = 500;

    @Schema(description = "分块重叠大小", example = "50", defaultValue = "50")
    @Min(value = 0, message = "重叠大小不能小于0")
    private Integer chunkOverlap = 50;

    @Schema(description = "用户ID")
    private Long userId;
}
