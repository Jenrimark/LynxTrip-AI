package com.lynxtrip.api;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lynxtrip.service.AiService;

/**
 * AI 对话：前端从线路/资讯库检索命中项作为上下文，后端拼入 system prompt，由大模型整理、润色后回答用户。
 */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);

    private final AiService aiService;

    public AiChatController(AiService aiService) {
        this.aiService = aiService;
    }

    public record ChatRequest(
            String message,
            List<Map<String, Object>> routeHits,
            List<Map<String, Object>> newsHits,
            String filesText
    ) {}

    public record ChatResponse(
            boolean success,
            String content,
            String source
    ) {}

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String userMessage = request.message();
        if (userMessage == null || userMessage.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse(false, "Message is required", "error"));
        }

        log.info("AI chat request: message length={}", userMessage.length());

        if (!aiService.isConfigured()) {
            return ResponseEntity.ok(new ChatResponse(false,
                    "未配置 OPENAI_API_KEY，请在仓库根目录 .env 中填写 AI 接口密钥。", "error"));
        }

        String systemPrompt = buildSystemPrompt(request);
        String aiResponse = aiService.chat(systemPrompt, userMessage);

        if (aiResponse != null) {
            return ResponseEntity.ok(new ChatResponse(true, aiResponse, "ai"));
        }

        return ResponseEntity.ok(new ChatResponse(false,
                "AI 接口调用失败，请检查网络、密钥与 OPENAI_BASE_URL 是否正确。", "error"));
    }

    private String buildSystemPrompt(ChatRequest request) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are Lingxi (灵犀), the AI travel assistant for the LynxTrip platform.\n\n");

        sb.append("## How LynxTrip uses you\n");
        sb.append("- The client first searches LynxTrip's route and news tables (and optional user uploads) for items relevant to the user's question.\n");
        sb.append("- Those search hits are pasted below as raw context. They are NOT the final answer—your job is to read them, merge with the user's question, ");
        sb.append("structure the reply, polish wording, and produce the answer the user sees.\n");
        sb.append("- Ground specific facts (names, prices, places) in the provided rows when applicable; add general travel knowledge only to clarify or fill gaps.\n");
        sb.append("- Do not invent concrete prices or route names that contradict the given rows.\n");
        sb.append("- Respond in Chinese (Simplified) unless the user asks for another language.\n\n");

        sb.append("## Task types you may handle\n");
        sb.append("Route suggestions, news summaries, packing lists, budget tips, and related travel Q&A.\n\n");

        List<Map<String, Object>> routes = request.routeHits();
        if (routes != null && !routes.isEmpty()) {
            sb.append("## Retrieved routes (from LynxTrip database)\n");
            sb.append("Use these rows when relevant to the user's question:\n");
            for (int i = 0; i < routes.size(); i++) {
                Map<String, Object> r = routes.get(i);
                sb.append(String.format("%d. %s\n", i + 1,
                        formatRoute(r)));
            }
            sb.append("\n");
        }

        List<Map<String, Object>> news = request.newsHits();
        if (news != null && !news.isEmpty()) {
            sb.append("## Retrieved news (from LynxTrip database)\n");
            sb.append("Use these articles when relevant:\n");
            for (int i = 0; i < news.size(); i++) {
                Map<String, Object> n = news.get(i);
                sb.append(String.format("%d. %s\n", i + 1,
                        formatNews(n)));
            }
            sb.append("\n");
        }

        String filesText = request.filesText();
        if (filesText != null && !filesText.isBlank()) {
            sb.append("## User-uploaded text (reference)\n");
            sb.append(filesText.substring(0, Math.min(filesText.length(), 1000)));
            sb.append("\n\n");
        }

        sb.append("## Final instruction\n");
        sb.append("Answer the user's latest message in a clear, friendly way. ");
        sb.append("Synthesize and polish—do not dump raw lists unless the user explicitly wants a table-like listing.\n");

        return sb.toString();
    }

    private String formatRoute(Map<String, Object> r) {
        String name = String.valueOf(r.getOrDefault("xianlumingcheng", "Unknown"));
        String category = String.valueOf(r.getOrDefault("xianlufenlei", ""));
        String price = String.valueOf(r.getOrDefault("price", "N/A"));
        String from = String.valueOf(r.getOrDefault("chufadi", ""));
        String to = String.valueOf(r.getOrDefault("mudedi", ""));
        String transport = String.valueOf(r.getOrDefault("jiaotongfangshi", ""));

        return String.format("%s [%s] ¥%s - %s -> %s (%s)",
                name, category, price, from, to, transport);
    }

    private String formatNews(Map<String, Object> n) {
        String title = String.valueOf(n.getOrDefault("title", "Unknown"));
        String intro = String.valueOf(n.getOrDefault("introduction", ""));
        if (intro.length() > 100) {
            intro = intro.substring(0, 100) + "...";
        }
        return String.format("%s: %s", title, intro);
    }
}
