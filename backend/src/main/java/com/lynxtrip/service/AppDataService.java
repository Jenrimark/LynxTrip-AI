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
        return jdbcTemplate.queryForList("SELECT id, xianlufenlei, addtime FROM xianlufenlei ORDER BY id ASC");
    }

    public List<Map<String, Object>> listRoutes(String kind) {
        String t = "zuixinxianlu".equals(kind) ? "zuixinxianlu" : "lvyouxianlu";
        return jdbcTemplate.queryForList("SELECT * FROM " + t + " ORDER BY clicknum DESC, id DESC");
    }

    public void bumpRouteClick(String kind, Long id) {
        String t = "zuixinxianlu".equals(kind) ? "zuixinxianlu" : "lvyouxianlu";
        jdbcTemplate.update("UPDATE " + t + " SET clicknum = clicknum + 1, clicktime=? WHERE id=?", Instant.now().toString(), id);
    }

    public List<Map<String, Object>> listNews() {
        return jdbcTemplate.queryForList("SELECT * FROM news ORDER BY id DESC");
    }

    public List<Map<String, Object>> listCart(Long userId) {
        return jdbcTemplate.queryForList("SELECT * FROM cart WHERE userid=? ORDER BY id DESC", userId);
    }

    public void upsertCart(Long userId, Map<String, Object> body) {
        String tablename = String.valueOf(body.get("tablename"));
        Number goodid = (Number) body.get("goodid");
        List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                "SELECT id, buynumber FROM cart WHERE userid=? AND tablename=? AND goodid=? LIMIT 1", userId, tablename, goodid.longValue());
        if (!existing.isEmpty()) {
            Long id = ((Number) existing.get(0).get("id")).longValue();
            jdbcTemplate.update("UPDATE cart SET buynumber = buynumber + 1 WHERE id=?", id);
            return;
        }
        jdbcTemplate.update(
                "INSERT INTO cart(userid, tablename, goodid, goodname, picture, buynumber, price, discountprice) VALUES(?,?,?,?,?,?,?,?)",
                userId, tablename, goodid.longValue(), String.valueOf(body.get("goodname")), String.valueOf(body.get("picture")), 1,
                new BigDecimal(String.valueOf(body.getOrDefault("price", "0"))), BigDecimal.ZERO);
    }

    public void updateCartQuantity(Long userId, Long id, Integer buynumber) {
        jdbcTemplate.update("UPDATE cart SET buynumber=? WHERE id=? AND userid=?", Math.max(1, buynumber), id, userId);
    }

    public void removeCart(Long userId, Long id) {
        jdbcTemplate.update("DELETE FROM cart WHERE id=? AND userid=?", id, userId);
    }

    public List<Map<String, Object>> listAddresses(Long userId) {
        return jdbcTemplate.queryForList("SELECT * FROM address WHERE userid=? ORDER BY (isdefault='是') DESC, id DESC", userId);
    }

    public void addAddress(Long userId, Map<String, Object> body) {
        String isDefault = String.valueOf(body.getOrDefault("isdefault", "否"));
        if ("是".equals(isDefault)) jdbcTemplate.update("UPDATE address SET isdefault='否' WHERE userid=?", userId);
        jdbcTemplate.update("INSERT INTO address(userid,address,name,phone,isdefault) VALUES(?,?,?,?,?)",
                userId, String.valueOf(body.get("address")), String.valueOf(body.get("name")), String.valueOf(body.get("phone")), isDefault);
    }

    public void setDefaultAddress(Long userId, Long id) {
        jdbcTemplate.update("UPDATE address SET isdefault='否' WHERE userid=?", userId);
        jdbcTemplate.update("UPDATE address SET isdefault='是' WHERE userid=? AND id=?", userId, id);
    }

    public void checkout(Long userId, String addressText) {
        List<Map<String, Object>> cart = listCart(userId);
        String base = String.valueOf(System.currentTimeMillis());
        int i = 0;
        for (Map<String, Object> c : cart) {
            BigDecimal price = new BigDecimal(String.valueOf(c.get("price")));
            int num = ((Number) c.get("buynumber")).intValue();
            BigDecimal total = price.multiply(BigDecimal.valueOf(num));
            jdbcTemplate.update(
                    "INSERT INTO orders(userid,orderid,tablename,goodid,goodname,picture,buynumber,price,discountprice,total,discounttotal,type,status,address) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    userId, base + i++, c.get("tablename"), c.get("goodid"), c.get("goodname"), c.get("picture"), num, price, BigDecimal.ZERO, total,
                    BigDecimal.ZERO, 1, "已支付", addressText);
        }
        jdbcTemplate.update("DELETE FROM cart WHERE userid=?", userId);
    }

    public List<Map<String, Object>> listOrders(Long userId) {
        return jdbcTemplate.queryForList("SELECT * FROM orders WHERE userid=? ORDER BY id DESC", userId);
    }

    public void updateOrderStatus(Long userId, Long id, String status) {
        jdbcTemplate.update("UPDATE orders SET status=? WHERE id=? AND userid=?", status, id, userId);
    }

    public void removeOrder(Long userId, Long id) {
        jdbcTemplate.update("DELETE FROM orders WHERE id=? AND userid=?", id, userId);
    }

    public List<Map<String, Object>> listStoreups(Long userId) {
        return jdbcTemplate.queryForList("SELECT * FROM storeup WHERE userid=? ORDER BY id DESC", userId);
    }

    public Map<String, Object> toggleStoreup(Long userId, Map<String, Object> body) {
        String tablename = String.valueOf(body.get("tablename"));
        Long refid = Long.valueOf(String.valueOf(body.get("refid")));
        List<Map<String, Object>> existing = jdbcTemplate.queryForList("SELECT id FROM storeup WHERE userid=? AND tablename=? AND refid=? LIMIT 1",
                userId, tablename, refid);
        if (!existing.isEmpty()) {
            jdbcTemplate.update("DELETE FROM storeup WHERE id=?", ((Number) existing.get(0).get("id")).longValue());
            return Map.of("ok", true, "fav", false);
        }
        jdbcTemplate.update("INSERT INTO storeup(userid,refid,tablename,name,picture) VALUES(?,?,?,?,?)",
                userId, refid, tablename, String.valueOf(body.get("name")), String.valueOf(body.get("picture")));
        return Map.of("ok", true, "fav", true);
    }

    public List<Map<String, Object>> listGallery(Long userId) {
        return jdbcTemplate.queryForList("SELECT * FROM gallery WHERE userid=? ORDER BY id DESC", userId);
    }

    public void addGallery(Long userId, Map<String, Object> body) {
        jdbcTemplate.update("INSERT INTO gallery(userid,title,photoUrl,note,takenAt,location) VALUES(?,?,?,?,?,?)",
                userId, String.valueOf(body.get("title")), String.valueOf(body.get("photoUrl")), String.valueOf(body.get("note")),
                String.valueOf(body.get("takenAt")), String.valueOf(body.get("location")));
    }

    public List<Map<String, Object>> listChat(Long userId) {
        return jdbcTemplate.queryForList("SELECT * FROM chat WHERE userid=? ORDER BY id ASC", userId);
    }

    public void sendChat(Long userId, String ask) {
        jdbcTemplate.update("INSERT INTO chat(userid,adminid,ask,reply,isreply) VALUES(?,?,?,?,?)", userId, 1, ask, null, 0);
        jdbcTemplate.update("INSERT INTO chat(userid,adminid,ask,reply,isreply) VALUES(?,?,?,?,?)", userId, 1, null,
                "已收到，我们会尽快安排人工客服。", 1);
    }

    public List<Map<String, Object>> listTrips(Long userId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM trips WHERE userid=? ORDER BY id DESC", userId);
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
        jdbcTemplate.update("INSERT INTO trips(userid,title,payload) VALUES(?,?,?)", userId, String.valueOf(body.get("title")), payload);
    }
}
