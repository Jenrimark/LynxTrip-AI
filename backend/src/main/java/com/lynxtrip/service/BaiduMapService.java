package com.lynxtrip.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class BaiduMapService {

    private static final Logger log = LoggerFactory.getLogger(BaiduMapService.class);

    private final String serverAk;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public record SuggestionItem(
            String value,
            String name,
            String address,
            String province,
            String city,
            String district,
            Double lng,
            Double lat
    ) {}

    public BaiduMapService() {
        String ak = System.getProperty("BAIDU_MAP_SERVER_AK");
        this.serverAk = ak != null ? ak.trim() : "";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public boolean isConfigured() {
        return !serverAk.isBlank();
    }

    public List<SuggestionItem> suggest(String query, String region) {
        if (!isConfigured()) {
            return List.of();
        }
        String q = query == null ? "" : query.trim();
        if (q.isBlank()) {
            return List.of();
        }

        String useRegion = (region == null || region.isBlank()) ? "全国" : region.trim();
        try {
            String url = "https://api.map.baidu.com/place/v2/suggestion"
                    + "?query=" + URLEncoder.encode(q, StandardCharsets.UTF_8)
                    + "&region=" + URLEncoder.encode(useRegion, StandardCharsets.UTF_8)
                    + "&city_limit=false"
                    + "&output=json"
                    + "&ak=" + URLEncoder.encode(serverAk, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Baidu suggest http status={}", response.statusCode());
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (root.path("status").asInt(-1) != 0) {
                log.warn("Baidu suggest business status={}, message={}",
                        root.path("status").asInt(-1), root.path("message").asText(""));
                return List.of();
            }

            JsonNode result = root.path("result");
            if (!result.isArray()) {
                return List.of();
            }

            List<SuggestionItem> items = new ArrayList<>();
            for (JsonNode n : result) {
                String name = n.path("name").asText("");
                String address = n.path("address").asText("");
                String province = n.path("province").asText("");
                String city = n.path("city").asText("");
                String district = n.path("district").asText("");
                JsonNode loc = n.path("location");
                Double lng = loc.path("lng").isNumber() ? loc.path("lng").asDouble() : null;
                Double lat = loc.path("lat").isNumber() ? loc.path("lat").asDouble() : null;
                String value = (address == null || address.isBlank()) ? name : (name + " - " + address);
                items.add(new SuggestionItem(value, name, address, province, city, district, lng, lat));
            }
            return items;
        } catch (Exception e) {
            log.error("Baidu suggest failed", e);
            return List.of();
        }
    }

    /**
     * 生成百度静态图（v2）并返回图片字节，避免在前端暴露 AK。
     * points 格式：["lng,lat", "lng,lat", ...]
     */
    public byte[] staticImage(List<String> points, int width, int height, int zoom) {
        if (!isConfigured()) {
            return null;
        }
        if (points == null || points.isEmpty()) {
            return null;
        }
        int w = Math.max(240, Math.min(1600, width));
        int h = Math.max(180, Math.min(1600, height));
        int z = Math.max(3, Math.min(18, zoom));

        try {
            // 计算中心点（平均值）
            double sumLng = 0;
            double sumLat = 0;
            int ok = 0;
            for (String p : points) {
                if (p == null) continue;
                String[] parts = p.split(",");
                if (parts.length != 2) continue;
                try {
                    double lng = Double.parseDouble(parts[0].trim());
                    double lat = Double.parseDouble(parts[1].trim());
                    sumLng += lng;
                    sumLat += lat;
                    ok++;
                } catch (Exception ignore) {
                    // skip invalid point
                }
            }
            if (ok <= 0) return null;
            String center = (sumLng / ok) + "," + (sumLat / ok);

            // markers: lng,lat|lng,lat...
            String markers = String.join("|", points);
            // paths: lng,lat;lng,lat;...
            String paths = String.join(";", points);
            String url = "https://api.map.baidu.com/staticimage/v2"
                    + "?ak=" + URLEncoder.encode(serverAk, StandardCharsets.UTF_8)
                    + "&width=" + w
                    + "&height=" + h
                    + "&center=" + URLEncoder.encode(center, StandardCharsets.UTF_8)
                    + "&zoom=" + z
                    + "&scale=2"
                    + "&markers=" + URLEncoder.encode(markers, StandardCharsets.UTF_8)
                    + "&paths=" + URLEncoder.encode(paths, StandardCharsets.UTF_8)
                    + "&pathStyles=" + URLEncoder.encode("0xff8839,5,0.8", StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                log.warn("Baidu static image http status={}", response.statusCode());
                return null;
            }
            byte[] body = response.body();
            // 百度静态图在鉴权失败时可能返回 JSON/HTML，做个简单兜底
            if (body == null || body.length < 16) return null;
            String head = new String(Arrays.copyOfRange(body, 0, Math.min(80, body.length)), StandardCharsets.UTF_8);
            if (head.contains("{\"status\"") || head.toLowerCase().contains("<html")) {
                log.warn("Baidu static image returned non-image payload: {}", head);
                return null;
            }
            return body;
        } catch (Exception e) {
            log.error("Baidu static image failed", e);
            return null;
        }
    }
}

