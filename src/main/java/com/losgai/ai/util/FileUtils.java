package com.losgai.ai.util;

public class FileUtils {
    /**
     * 提取文件扩展名
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex == -1 ? "" : fileName.substring(dotIndex + 1);
    }

    /**
     * 去除文件名后缀
     */
    public static String stripExtension(String fileName) {
        if (fileName == null) return "未命名文档";
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex == -1 ? fileName : fileName.substring(0, dotIndex);
    }

    /**
     * 简易内容摘要生成（前200字）
     */
    public static String generateSummary(String content) {
        if (content == null) return "";
        return content.length() > 200 ? content.substring(0, 200) + "..." : content;
    }

    /**
     * 简单语言检测（可接入语言检测库）
     */
    public static String detectLanguage(String content) {
        if (content == null || content.isEmpty()) return "unknown";
        // 简单启发式检测：是否含中文字符
        return content.codePoints().anyMatch(c -> c >= 0x4E00 && c <= 0x9FFF) ? "zh" : "en";
    }
}
