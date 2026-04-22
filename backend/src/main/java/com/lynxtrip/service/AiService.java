package com.lynxtrip.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 调用 OpenAI 兼容的 AI API（chat/completions）。
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final int timeoutMs;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AiService() {
        String k = System.getProperty("OPENAI_API_KEY");
        this.apiKey = k != null ? k.trim() : "";
        String u = System.getProperty("OPENAI_BASE_URL");
        this.baseUrl = (u != null && !u.isBlank()) ? u.trim() : "https://api.openai.com/v1";
        String m = System.getProperty("OPENAI_MODEL");
        this.model = (m != null && !m.isBlank()) ? m.trim() : "gpt-4o-mini";
        this.timeoutMs = parseTimeoutMs();

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();

        log.info("AiService initialized: baseUrl={}, model={}, timeoutMs={}, configured={}",
                baseUrl, model, timeoutMs, isConfigured());
    }

    private static int parseTimeoutMs() {
        String raw = System.getProperty("AI_TIMEOUT_MS");
        if (raw == null || raw.isBlank()) {
            return 60000;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return 60000;
        }
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    /**
     * @param systemPrompt 系统提示（含本地上下文）
     * @param userMessage 用户问题
     * @return 模型返回的正文；未配置密钥或调用失败时返回 null
     */
    public String chat(String systemPrompt, String userMessage) {
        return chat(systemPrompt, userMessage, 0.7, 2000);
    }

    /**
     * @param systemPrompt 系统提示（含本地上下文）
     * @param userMessage 用户输入（可为结构化 JSON 字符串）
     * @param temperature 采样温度（越低越稳定）
     * @param maxTokens 最大输出 token
     * @return 模型返回的 message.content；未配置密钥或调用失败时返回 null
     */
    public String chat(String systemPrompt, String userMessage, double temperature, int maxTokens) {
        if (!isConfigured()) {
            log.warn("OPENAI_API_KEY is empty");
            return null;
        }

        int safeMaxTokens = Math.max(256, Math.min(8192, maxTokens));
        double safeTemp = Double.isFinite(temperature) ? Math.max(0.0, Math.min(2.0, temperature)) : 0.7;

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage)
                    ),
                    "temperature", safeTemp,
                    "max_tokens", safeMaxTokens
            );

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .build();

            log.debug("Sending AI request to {}", baseUrl + "/chat/completions");
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("AI API error: status={}, body={}", response.statusCode(), response.body());
                return null;
            }

            String responseBody = response.body();
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode errNode = root.get("error");
            if (errNode != null && !errNode.isNull()) {
                log.error("AI API error in body: {}", responseBody);
                return null;
            }
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                String content = choices.get(0).path("message").path("content").asText("");
                if (content != null && !content.isBlank()) {
                    return content;
                }
                log.error("AI API empty message.content, body={}", responseBody);
                return null;
            }

            log.error("AI API response missing choices: {}", responseBody);
            return null;

        } catch (Exception e) {
            log.error("AI API call failed", e);
            return null;
        }
    }
}
