package com.lynxtrip.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class AppDataService {
    private final JdbcTemplate jdbcTemplate;
    private final JwtSessionService jwtSessionService;
    private final ObjectMapper objectMapper;

    public AppDataService(JdbcTemplate jdbcTemplate, JwtSessionService jwtSessionService, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.jwtSessionService = jwtSessionService;
        this.objectMapper = objectMapper;
    }

    public Long requireUserId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        String token = null;
        for (Cookie c : cookies) if ("lynxtrip_session".equals(c.getName())) token = c.getValue();
        if (token == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        Optional<Long> uid = jwtSessionService.parseUserId(token);
        if (uid.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录态失效");
        return uid.get();
    }

    public List<Map<String, Object>> listCategories() {
        return jdbcTemplate.queryForList("SELECT id, name, created_at FROM route_categories ORDER BY id ASC");
    }

    public List<Map<String, Object>> listRoutes(String kind) {
        String t = "zuixinxianlu".equals(kind) ? "latest_routes" : "travel_routes";
        return jdbcTemplate.queryForList("SELECT * FROM " + t + " ORDER BY click_count DESC, id DESC");
    }

    public void bumpRouteClick(String kind, Long id) {
        String t = "zuixinxianlu".equals(kind) ? "latest_routes" : "travel_routes";
        jdbcTemplate.update("UPDATE " + t + " SET click_count = click_count + 1, last_clicked_at=? WHERE id=?", Instant.now().toString(), id);
    }

    public List<Map<String, Object>> listNews() {
        return jdbcTemplate.queryForList("SELECT * FROM travel_news ORDER BY id DESC");
    }

    public List<Map<String, Object>> listCart(Long userId) {
        return jdbcTemplate.queryForList("SELECT * FROM shopping_cart WHERE user_id=? ORDER BY id DESC", userId);
    }

    public void upsertCart(Long userId, Map<String, Object> body) {
        String tableName = String.valueOf(body.get("tableName"));
        Number productId = (Number) body.get("productId");
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT id, buy_number FROM shopping_cart WHERE user_id=? AND table_name=? AND product_id=? LIMIT 1", userId, tableName, productId.longValue());
        if (!existing.isEmpty()) {
            Long id = ((Number) existing.get(0).get("id")).longValue();
            jdbcTemplate.update("UPDATE shopping_cart SET buy_number = buy_number + 1 WHERE id=?", id);
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO shopping_cart(user_id, table_name, product_id, product_name, picture, buy_number, price, discount_price) VALUES(?,?,?,?,?,?,?,?)",
                userId, tableName, productId.longValue(), String.valueOf(body.get("productName")), String.valueOf(body.get("picture")), 1,
                new BigDecimal(String.valueOf(body.getOrDefault("price", "0"))), BigDecimal.ZERO);
    }

    public void updateCartQuantity(Long userId, Long id, Integer buyNumber) {
        jdbcTemplate.update("UPDATE shopping_cart SET buy_number=? WHERE id=? AND user_id=?", Math.max(1, buyNumber), id, userId);
    }

    public void removeCart(Long userId, Long id) {
        jdbcTemplate.update("DELETE FROM shopping_cart WHERE id=? AND user_id=?", id, userId);
    }

    public List<Map<String, Object>> listAddresses(Long userId) {
        return jdbcTemplate.queryForList("SELECT * FROM user_addresses WHERE user_id=? ORDER BY (is_default='是') DESC, id DESC", userId);
    }

    public void addAddress(Long userId, Map<String, Object> body) {
        String isDefault = String.valueOf(body.getOrDefault("isDefault", "否"));
        if ("是".equals(isDefault)) jdbcTemplate.update("UPDATE user_addresses SET is_default='否' WHERE user_id=?", userId);
        jdbcTemplate.update("INSERT INTO user_addresses(user_id,address,name,phone,is_default) VALUES(?,?,?,?,?)",
                userId, String.valueOf(body.get("address")), String.valueOf(body.get("name")), String.valueOf(body.get("phone")), isDefault);
    }

    public void setDefaultAddress(Long userId, Long id) {
        jdbcTemplate.update("UPDATE user_addresses SET is_default='否' WHERE user_id=?", userId);
        jdbcTemplate.update("UPDATE user_addresses SET is_default='是' WHERE user_id=? AND id=?", userId, id);
    }

    public void checkout(Long userId, String addressText) {
        List<Map<String, Object>> cart = listCart(userId);
        String base = String.valueOf(System.currentTimeMillis());
        int i = 0;
        for (Map<String, Object> c : cart) {
            BigDecimal price = new BigDecimal(String.valueOf(c.get("price")));
            int num = ((Number) c.get("buy_number")).intValue();
            BigDecimal total = price.multiply(BigDecimal.valueOf(num));
            jdbcTemplate.update(
                    "INSERT INTO purchase_orders(user_id,order_no,table_name,product_id,product_name,picture,buy_number,price,discount_price,total,discount_total,type,status,address) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    userId, base + i++, c.get("table_name"), c.get("product_id"), c.get("product_name"), c.get("picture"), num, price, BigDecimal.ZERO, total,
                    BigDecimal.ZERO, 1, "已支付", addressText);
        }
        jdbcTemplate.update("DELETE FROM shopping_cart WHERE user_id=?", userId);
    }

    public List<Map<String, Object>> listOrders(Long userId) {
        return jdbcTemplate.queryForList("SELECT * FROM purchase_orders WHERE user_id=? ORDER BY id DESC", userId);
    }

    public void updateOrderStatus(Long userId, Long id, String status) {
        jdbcTemplate.update("UPDATE purchase_orders SET status=? WHERE id=? AND user_id=?", status, id, userId);
    }

    public void removeOrder(Long userId, Long id) {
        jdbcTemplate.update("DELETE FROM purchase_orders WHERE id=? AND user_id=?", id, userId);
    }

    public List<Map<String, Object>> listStoreups(Long userId) {
        return jdbcTemplate.queryForList("SELECT * FROM favorites WHERE user_id=? ORDER BY id DESC", userId);
    }

    public Map<String, Object> toggleStoreup(Long userId, Map<String, Object> body) {
        String tableName = String.valueOf(body.get("tableName"));
        Long refId = Long.valueOf(String.valueOf(body.get("refId")));
        List<Map<String, Object>> existing = jdbcTemplate.queryForList("SELECT id FROM favorites WHERE user_id=? AND table_name=? AND ref_id=? LIMIT 1",
                userId, tableName, refId);
        if (!existing.isEmpty()) {
            jdbcTemplate.update("DELETE FROM favorites WHERE id=?", ((Number) existing.get(0).get("id")).longValue());
            return Map.of("ok", true, "fav", false);
        }
        jdbcTemplate.update("INSERT INTO favorites(user_id,ref_id,table_name,name,picture) VALUES(?,?,?,?,?)",
                userId, refId, tableName, String.valueOf(body.get("name")), String.valueOf(body.get("picture")));
        return Map.of("ok", true, "fav", true);
    }

    public List<Map<String, Object>> listGallery(Long userId) {
        return jdbcTemplate.queryForList("SELECT * FROM user_gallery WHERE user_id=? ORDER BY id DESC", userId);
    }

    public void addGallery(Long userId, Map<String, Object> body) {
        jdbcTemplate.update("INSERT INTO user_gallery(user_id,title,photo_url,note,taken_at,location) VALUES(?,?,?,?,?,?)",
                userId, String.valueOf(body.get("title")), String.valueOf(body.get("photoUrl")), String.valueOf(body.get("note")),
                String.valueOf(body.get("takenAt")), String.valueOf(body.get("location")));
    }

    public List<Map<String, Object>> listChat(Long userId) {
        return jdbcTemplate.queryForList("SELECT * FROM support_chats WHERE user_id=? ORDER BY id ASC", userId);
    }

    public void sendChat(Long userId, String ask) {
        jdbcTemplate.update("INSERT INTO support_chats(user_id,admin_id,ask,reply,is_reply) VALUES(?,?,?,?,?)", userId, 1, ask, null, 0);
        jdbcTemplate.update("INSERT INTO support_chats(user_id,admin_id,ask,reply,is_reply) VALUES(?,?,?,?,?)", userId, 1, null,
                "已收到，我们会尽快安排人工客服。", 1);
    }

    public List<Map<String, Object>> listTrips(Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM trip_plans WHERE user_id=? ORDER BY id DESC", userId);
        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            Map<String, Object> next = new LinkedHashMap<>(row);
            Object payloadRaw = row.get("payload");
            if (payloadRaw instanceof String text && !text.isBlank()) {
                try {
                    next.put("payload", objectMapper.readValue(text, new TypeReference<Map<String, Object>>() {
                    }));
                } catch (Exception ignored) {
                    next.put("payload", Map.of("raw", text));
                }
            }
            out.add(next);
        }
        return out;
    }

    public void saveTrip(Long userId, Map<String, Object> body) {
        Object payloadObj = body.get("payload");
        String payload;
        try {
            payload = objectMapper.writeValueAsString(payloadObj == null ? Map.of() : payloadObj);
        } catch (Exception e) {
            payload = "{}";
        }
        jdbcTemplate.update("INSERT INTO trip_plans(user_id,title,payload) VALUES(?,?,?)", userId, String.valueOf(body.get("title")), payload);
    }

    public void deleteTrips(Long userId, String idsText) {
        if (idsText == null || idsText.isBlank()) {
            return;
        }
        String[] parts = idsText.split(",");
        List<Long> ids = new ArrayList<>();
        for (String p : parts) {
            String s = p == null ? "" : p.trim();
            if (s.isEmpty()) continue;
            try {
                ids.add(Long.valueOf(s));
            } catch (NumberFormatException ignored) {
                // skip invalid id
            }
        }
        if (ids.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", ids.stream().map(x -> "?").toList());
        List<Object> args = new ArrayList<>();
        args.add(userId);
        args.addAll(ids);
        jdbcTemplate.update("DELETE FROM trip_plans WHERE user_id=? AND id IN (" + placeholders + ")", args.toArray());
    }
}
