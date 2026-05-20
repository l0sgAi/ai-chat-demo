package com.losgai.ai.tools;

import cn.dev33.satoken.secure.SaSecureUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class EncodingTools {

    @Tool(name = "Base64Encode", description = "Encode a plain text string to Base64")
    String base64Encode(
            @ToolParam(description = "The plain text string to encode") String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    @Tool(name = "Base64Decode", description = "Decode a Base64 encoded string back to plain text")
    String base64Decode(
            @ToolParam(description = "The Base64 encoded string to decode") String encoded) {
        try {
            return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "Error: Invalid Base64 string - " + e.getMessage();
        }
    }

    @Tool(name = "UrlEncode", description = "Encode a string for safe use in URLs (percent-encoding)")
    String urlEncode(
            @ToolParam(description = "The string to URL-encode") String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    @Tool(name = "UrlDecode", description = "Decode a percent-encoded URL string back to plain text")
    String urlDecode(
            @ToolParam(description = "The URL-encoded string to decode") String encoded) {
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }

    @Tool(name = "HashText", description = "Generate a hash of the given text. The 'algorithm' parameter is required — you must confirm with the user which algorithm to use if not specified, or you can NEVER give a output. Supported: md5, sha1, sha256, sha384")
    String hashText(
            @ToolParam(description = "The hash algorithm to use. Required. Supported values: md5, sha1, sha256, sha384. If the user did not specify, ask them to choose.") String algorithm,
            @ToolParam(description = "The text to hash") String text) {
        return switch (algorithm.trim().toLowerCase()) {
            case "md5" -> SaSecureUtil.md5(text);
            case "sha1" -> SaSecureUtil.sha1(text);
            case "sha256" -> SaSecureUtil.sha256(text);
            case "sha384" -> SaSecureUtil.sha384(text);
            default -> "Error: Unsupported algorithm '" + algorithm + "'. Supported: md5, sha1, sha256, sha384";
        };
    }
}
