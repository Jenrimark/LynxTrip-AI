package com.lynxtrip.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiTripPlannerService {

    private static final Logger log = LoggerFactory.getLogger(AiTripPlannerService.class);

    private final AiService aiService;
    private final ObjectMapper objectMapper;

    public AiTripPlannerService(AiService aiService, ObjectMapper objectMapper) {
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    public JsonNode generateTripPlan(Map<String, Object> frontendBody) {
        if (frontendBody == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (!aiService.isConfigured()) {
            throw new IllegalStateException("未配置 OPENAI_API_KEY，请在仓库根目录 .env 中填写 AI 接口密钥。");
        }

        Map<String, Object> inputCtx = buildInputContext(frontendBody);
        String inputJson = writeJson(inputCtx);

        String systemPrompt = buildSystemPrompt();

        // 行程生成必须严格 JSON；温度低一点更稳定
        String raw = aiService.chat(systemPrompt, inputJson, 0.2, 5000);
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("AI 接口调用失败，请检查网络、密钥与 OPENAI_BASE_URL 是否正确。");
        }

        String cleaned = extractJsonObjectText(raw);
        JsonNode node;
        try {
            node = objectMapper.readTree(cleaned);
        } catch (Exception e) {
            log.warn("AI trip plan is not valid JSON. rawHead={}", raw.substring(0, Math.min(200, raw.length())));
            throw new IllegalArgumentException("AI 返回内容不是合法 JSON（请稍后重试）");
        }

        validateTripPlan(node);
        return node;
    }

    private Map<String, Object> buildInputContext(Map<String, Object> body) {
        // 已规范化输入（优先）
        if (body.containsKey("route_type") || body.containsKey("destinations") || body.containsKey("total_days")) {
            String routeType = reqTrim(body.get("route_type"), "route_type");
            List<String> destinations = reqStringList(body.get("destinations"), "destinations");
            int totalDays = reqInt(body.get("total_days"), "total_days");
            List<String> preferences = optStringList(body.get("preferences"));
            String custom = String.valueOf(body.getOrDefault("custom_description", "")).trim();
            return Map.of(
                    "route_type", routeType,
                    "destinations", destinations,
                    "total_days", totalDays,
                    "preferences", preferences,
                    "custom_description", custom
            );
        }

        // 多城市创建（AppShell multicityForm）
        if (body.containsKey("stops")) {
            List<Map<String, Object>> stops = reqMapList(body.get("stops"), "stops");
            List<String> cities = new ArrayList<>();
            int totalDays = 0;
            for (Map<String, Object> s : stops) {
                String city = String.valueOf(s.getOrDefault("city", "")).trim();
                if (city.isEmpty()) continue;
                cities.add(city);
                totalDays += Math.max(1, parseIntSafe(s.get("days"), 1));
            }
            if (cities.isEmpty()) {
                throw new IllegalArgumentException("至少需要 1 个城市（stops.city）");
            }
            String title = String.valueOf(body.getOrDefault("title", "")).trim();
            String desc = String.valueOf(body.getOrDefault("description", "")).trim();
            String custom = joinNonBlank("；", title, desc);
            List<String> preferences = new ArrayList<>();
            preferences.addAll(optStringList(body.get("interests")));
            // 兼容：若前端没传 interests，也允许用 preference/plannerInput 补充
            String preferenceText = String.valueOf(body.getOrDefault("preference", "")).trim();
            if (!preferenceText.isEmpty()) preferences.add(preferenceText);
            String plannerInput = String.valueOf(body.getOrDefault("plannerInput", "")).trim();
            if (!plannerInput.isEmpty()) custom = joinNonBlank("；", custom, plannerInput);

            return Map.of(
                    "route_type", cities.size() > 1 ? "多城市连线" : "单城市",
                    "destinations", cities,
                    "total_days", Math.max(1, totalDays),
                    "preferences", preferences,
                    "custom_description", custom
            );
        }

        // 单城市创建（MyItineraryView / AiTripView 表单）
        String destination = String.valueOf(body.getOrDefault("destination", "")).trim();
        if (destination.isEmpty()) {
            // 有些页面只传 cities/days query，不走 body；此处给出明确错误
            throw new IllegalArgumentException("缺少目的地（destination）或 stops");
        }
        int days = Math.max(1, parseIntSafe(body.getOrDefault("days", 1), 1));
        List<String> preferences = new ArrayList<>();
        Object prefObj = body.get("preferences");
        if (prefObj != null) preferences.addAll(optStringList(prefObj));
        String preferenceText = String.valueOf(body.getOrDefault("preference", "")).trim();
        if (!preferenceText.isEmpty()) preferences.add(preferenceText);
        String plannerInput = String.valueOf(body.getOrDefault("plannerInput", "")).trim();

        return Map.of(
                "route_type", "单城市",
                "destinations", List.of(destination),
                "total_days", days,
                "preferences", preferences,
                "custom_description", plannerInput
        );
    }

    private String buildSystemPrompt() {
        // 直接把你给的提示词工程“绝对规则 + 输出 schema + HTML 规范”写进 system prompt
        return """
你现在是“灵犀AI旅游网站”的首席行程规划引擎。你的任务是接收用户输入的结构化行程数据，并严格按照预设格式，规划出逻辑顺畅、符合用户偏好的日程安排。

绝对规则：
- 你必须只返回合法的纯 JSON 格式数据。
- 不要包含任何 Markdown 格式（如 ```json 等包裹符）。
- 不要有任何解释性文本或前言后语。
- 所有的富文本描述必须包裹在指定的 HTML 标签中（<section> 和 .card）。

输入数据结构（用户消息中会直接提供一个 JSON 对象）：
- route_type: 路线类型（单城市 / 多城市连线）。
- destinations: 目的地数组（若是多城市，严格按照数组顺序规划）。
- total_days: 总天数。
- preferences: 倾向性偏好标签数组。
- custom_description: 用户自行填写的具体行程描述或愿望清单。

输出数据结构（必须严格符合）：
{
  "trip_title": "行程主标题（结合目的地和偏好生成）",
  "trip_summary": "一段引人入胜的整体行程摘要",
  "total_days": 整数,
  "itinerary": [
    {
      "day_number": 整数,
      "city": "当天所在城市",
      "day_theme": "当天的核心主题（如：古城漫步）",
      "html_content": "严格按照下面 HTML 规范生成的字符串",
      "activities": [
        {
          "order": 整数,
          "time_period": "上午 / 下午 / 晚上",
          "activity_name": "活动或景点名称",
          "activity_description": "具体的游玩建议或亮点，需融合用户的 preferences"
        }
      ]
    }
  ]
}

HTML 结构生成规范（html_content 字段必须严格输出，且仅允许这些标签结构，不允许引入其他标签或样式）：
<section class="day-section">
  <div class="day-header">Day {day_number}: {city} - {day_theme}</div>
  <div class="activities-container">
    <div class="card activity-card">
      <h3 class="card-title">{time_period} | {activity_name}</h3>
      <p class="card-desc">{activity_description}</p>
    </div>
  </div>
</section>

补充要求：
- itinerary 必须完整覆盖 total_days 天；day_number 从 1 连续递增。
- 多城市连线：按 destinations 顺序分配天数（可自行均衡，但需保证顺序与过渡合理）。
- 活动时间段只能使用：上午 / 下午 / 晚上（不要输出“中午/傍晚/夜里”等其他值）。
- html_content 中每个活动都必须生成一个 <div class="card activity-card"> 块，顺序与 activities 一致。
""";
    }

    private static String extractJsonObjectText(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.isEmpty()) return s;

        // 去掉常见代码围栏
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline > 0) {
                s = s.substring(firstNewline + 1);
            }
            int lastFence = s.lastIndexOf("```");
            if (lastFence >= 0) {
                s = s.substring(0, lastFence);
            }
            s = s.trim();
        }

        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1).trim();
        }
        return s;
    }

    private void validateTripPlan(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("AI 返回的根节点不是 JSON Object");
        }
        reqText(root, "trip_title");
        reqText(root, "trip_summary");
        int totalDays = reqIntNode(root, "total_days");

        JsonNode itinerary = root.get("itinerary");
        if (itinerary == null || !itinerary.isArray()) {
            throw new IllegalArgumentException("AI 返回缺少 itinerary 数组");
        }
        if (itinerary.size() != totalDays) {
            throw new IllegalArgumentException("AI 返回 itinerary 天数与 total_days 不一致");
        }
        for (int i = 0; i < itinerary.size(); i++) {
            JsonNode day = itinerary.get(i);
            if (day == null || !day.isObject()) {
                throw new IllegalArgumentException("itinerary[" + i + "] 不是对象");
            }
            int dayNumber = reqIntNode(day, "day_number");
            if (dayNumber != i + 1) {
                throw new IllegalArgumentException("day_number 必须从 1 连续递增（期望 " + (i + 1) + "）");
            }
            reqText(day, "city");
            reqText(day, "day_theme");
            String html = reqText(day, "html_content");
            if (!html.contains("<section class=\"day-section\">")) {
                throw new IllegalArgumentException("html_content 不符合 day-section 规范");
            }
            JsonNode acts = day.get("activities");
            if (acts == null || !acts.isArray() || acts.isEmpty()) {
                throw new IllegalArgumentException("activities 必须为非空数组");
            }
            for (int j = 0; j < acts.size(); j++) {
                JsonNode a = acts.get(j);
                if (a == null || !a.isObject()) throw new IllegalArgumentException("activities[" + j + "] 不是对象");
                int order = reqIntNode(a, "order");
                if (order != j + 1) {
                    throw new IllegalArgumentException("activities.order 必须从 1 连续递增（day " + dayNumber + "）");
                }
                String tp = reqText(a, "time_period");
                if (!("上午".equals(tp) || "下午".equals(tp) || "晚上".equals(tp))) {
                    throw new IllegalArgumentException("time_period 只能是 上午/下午/晚上（day " + dayNumber + "）");
                }
                reqText(a, "activity_name");
                reqText(a, "activity_description");
            }
        }
    }

    private static String reqText(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) throw new IllegalArgumentException("缺少字段：" + field);
        String s = v.asText("").trim();
        if (s.isEmpty()) throw new IllegalArgumentException("字段为空：" + field);
        return s;
    }

    private static int reqIntNode(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) throw new IllegalArgumentException("缺少字段：" + field);
        if (v.isInt() || v.isLong()) {
            int n = v.asInt();
            if (n <= 0) throw new IllegalArgumentException("字段必须为正整数：" + field);
            return n;
        }
        String s = v.asText("").trim();
        int n = parseIntSafe(s, -1);
        if (n <= 0) throw new IllegalArgumentException("字段必须为正整数：" + field);
        return n;
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("请求数据序列化失败");
        }
    }

    private static String reqTrim(Object v, String field) {
        String s = String.valueOf(v == null ? "" : v).trim();
        if (s.isEmpty()) throw new IllegalArgumentException("缺少字段：" + field);
        return s;
    }

    private static int reqInt(Object v, String field) {
        int n = parseIntSafe(v, -1);
        if (n <= 0) throw new IllegalArgumentException("字段必须为正整数：" + field);
        return n;
    }

    private static int parseIntSafe(Object v, int fallback) {
        if (v == null) return fallback;
        if (v instanceof Number num) return num.intValue();
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return fallback;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> reqMapList(Object v, String field) {
        if (!(v instanceof List<?> list)) {
            throw new IllegalArgumentException("字段类型不正确：" + field);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                Map<String, Object> mm = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    mm.put(String.valueOf(e.getKey()), e.getValue());
                }
                out.add(mm);
            }
        }
        return out;
    }

    private static List<String> reqStringList(Object v, String field) {
        List<String> out = optStringList(v);
        if (out.isEmpty()) throw new IllegalArgumentException("字段不能为空：" + field);
        return out;
    }

    private static List<String> optStringList(Object v) {
        if (v == null) return List.of();
        if (v instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object it : list) {
                String s = String.valueOf(it == null ? "" : it).trim();
                if (!s.isEmpty()) out.add(s);
            }
            return out;
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return List.of();
        // 兼容：用逗号/顿号/分号拆分
        String[] parts = s.split("[,，;；、】【\\s]+");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String t = p == null ? "" : p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static String joinNonBlank(String sep, String... parts) {
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            String s = String.valueOf(p == null ? "" : p).trim();
            if (!s.isEmpty()) out.add(s);
        }
        return String.join(sep, out);
    }
}

