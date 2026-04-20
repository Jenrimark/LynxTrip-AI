package com.lynxtrip.api;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lynxtrip.service.BaiduMapService;

@RestController
@RequestMapping("/api/maps")
public class MapController {

    private final BaiduMapService baiduMapService;

    public record SuggestResponse(
            boolean success,
            String message,
            List<BaiduMapService.SuggestionItem> data
    ) {}

    public MapController(BaiduMapService baiduMapService) {
        this.baiduMapService = baiduMapService;
    }

    @GetMapping("/suggest")
    public ResponseEntity<SuggestResponse> suggest(
            @RequestParam("q") String query,
            @RequestParam(value = "region", required = false, defaultValue = "全国") String region
    ) {
        if (!baiduMapService.isConfigured()) {
            return ResponseEntity.ok(new SuggestResponse(false, "未配置 BAIDU_MAP_SERVER_AK", List.of()));
        }
        List<BaiduMapService.SuggestionItem> list = baiduMapService.suggest(query, region);
        return ResponseEntity.ok(new SuggestResponse(true, "ok", list));
    }

    /**
     * 静态地图图片（用于导出 PDF 等），由后端携带服务端 AK 去请求百度静态图 API，
     * 避免前端暴露 AK。
     *
     * points: "lng,lat|lng,lat|..."
     */
    @GetMapping("/staticimage")
    public ResponseEntity<byte[]> staticImage(
            @RequestParam("points") String points,
            @RequestParam(value = "w", required = false, defaultValue = "1200") int w,
            @RequestParam(value = "h", required = false, defaultValue = "800") int h,
            @RequestParam(value = "zoom", required = false, defaultValue = "6") int zoom
    ) {
        if (!baiduMapService.isConfigured()) {
            return ResponseEntity.status(503).body(new byte[0]);
        }
        String raw = points == null ? "" : points.trim();
        if (raw.isBlank()) {
            return ResponseEntity.badRequest().body(new byte[0]);
        }
        List<String> list = Arrays.stream(raw.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .limit(20)
                .collect(Collectors.toList());
        byte[] image = baiduMapService.staticImage(list, w, h, zoom);
        if (image == null || image.length == 0) {
            return ResponseEntity.status(502).body(new byte[0]);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }
}

