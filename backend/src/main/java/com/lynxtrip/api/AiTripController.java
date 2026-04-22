package com.lynxtrip.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.lynxtrip.service.AiTripPlannerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiTripController {

    private final AiTripPlannerService aiTripPlannerService;

    public AiTripController(AiTripPlannerService aiTripPlannerService) {
        this.aiTripPlannerService = aiTripPlannerService;
    }

    public record TripGenerateResponse(
            boolean ok,
            JsonNode plan,
            String error
    ) {
    }

    /**
     * 行程生成（提示词工程核心接口）。
     *
     * 接收前端“创建行程/多城市创建”的原始数据，后端统一组装成规范 Input Context：
     * - route_type
     * - destinations
     * - total_days
     * - preferences
     * - custom_description
     *
     * 然后调用 OpenAI/兼容大模型接口，并对返回结果做 JSON 反序列化与校验。
     */
    @PostMapping("/trip/generate")
    public ResponseEntity<TripGenerateResponse> generate(@RequestBody Map<String, Object> body) {
        try {
            JsonNode plan = aiTripPlannerService.generateTripPlan(body);
            return ResponseEntity.ok(new TripGenerateResponse(true, plan, null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new TripGenerateResponse(false, null, e.getMessage()));
        } catch (IllegalStateException e) {
            // e.g. OPENAI_API_KEY missing / AI not configured
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new TripGenerateResponse(false, null, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new TripGenerateResponse(false, null, "行程生成失败，请稍后重试"));
        }
    }
}

