package com.lynxtrip.api;

import java.util.List;
import java.util.Map;

import com.lynxtrip.service.AppDataService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data")
public class AppDataController {
    private final AppDataService appDataService;

    public AppDataController(AppDataService appDataService) {
        this.appDataService = appDataService;
    }

    @GetMapping("/categories")
    public List<Map<String, Object>> categories() {
        return appDataService.listCategories();
    }

    @GetMapping("/routes")
    public List<Map<String, Object>> routes(@RequestParam(defaultValue = "lvyouxianlu") String kind) {
        return appDataService.listRoutes(kind);
    }

    @PostMapping("/routes/click")
    public Map<String, Object> bumpClick(@RequestBody Map<String, Object> body) {
        appDataService.bumpRouteClick(reqString(body, "kind"), reqLong(body, "id"));
        return Map.of("ok", true);
    }

    @GetMapping("/news")
    public List<Map<String, Object>> news() {
        return appDataService.listNews();
    }

    @GetMapping("/cart")
    public List<Map<String, Object>> cart(HttpServletRequest request) {
        return appDataService.listCart(appDataService.requireUserId(request));
    }

    @PostMapping("/cart")
    public Map<String, Object> upsertCart(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        appDataService.upsertCart(appDataService.requireUserId(request), body);
        return Map.of("ok", true);
    }

    @PatchMapping("/cart/quantity")
    public Map<String, Object> updateCartQuantity(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        appDataService.updateCartQuantity(appDataService.requireUserId(request), reqLong(body, "id"),
                Integer.parseInt(reqString(body, "buyNumber")));
        return Map.of("ok", true);
    }

    @DeleteMapping("/cart")
    public Map<String, Object> removeCart(@RequestParam Long id, HttpServletRequest request) {
        appDataService.removeCart(appDataService.requireUserId(request), id);
        return Map.of("ok", true);
    }

    @GetMapping("/addresses")
    public List<Map<String, Object>> addresses(HttpServletRequest request) {
        return appDataService.listAddresses(appDataService.requireUserId(request));
    }

    @PostMapping("/addresses")
    public Map<String, Object> addAddress(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        appDataService.addAddress(appDataService.requireUserId(request), body);
        return Map.of("ok", true);
    }

    @PatchMapping("/addresses/default")
    public Map<String, Object> setDefaultAddress(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        appDataService.setDefaultAddress(appDataService.requireUserId(request), reqLong(body, "id"));
        return Map.of("ok", true);
    }

    @PostMapping("/checkout")
    public Map<String, Object> checkout(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        appDataService.checkout(appDataService.requireUserId(request), reqString(body, "addressText"));
        return Map.of("ok", true);
    }

    @GetMapping("/orders")
    public List<Map<String, Object>> orders(HttpServletRequest request) {
        return appDataService.listOrders(appDataService.requireUserId(request));
    }

    @PatchMapping("/orders/status")
    public Map<String, Object> updateOrderStatus(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        appDataService.updateOrderStatus(appDataService.requireUserId(request), reqLong(body, "id"),
                reqString(body, "status"));
        return Map.of("ok", true);
    }

    @DeleteMapping("/orders")
    public Map<String, Object> removeOrder(@RequestParam Long id, HttpServletRequest request) {
        appDataService.removeOrder(appDataService.requireUserId(request), id);
        return Map.of("ok", true);
    }

    @GetMapping("/storeups")
    public List<Map<String, Object>> storeups(HttpServletRequest request) {
        return appDataService.listStoreups(appDataService.requireUserId(request));
    }

    @PostMapping("/storeups/toggle")
    public Map<String, Object> toggleStoreup(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        return appDataService.toggleStoreup(appDataService.requireUserId(request), body);
    }

    @GetMapping("/gallery")
    public List<Map<String, Object>> gallery(HttpServletRequest request) {
        return appDataService.listGallery(appDataService.requireUserId(request));
    }

    @PostMapping("/gallery")
    public Map<String, Object> addGallery(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        appDataService.addGallery(appDataService.requireUserId(request), body);
        return Map.of("ok", true);
    }

    @GetMapping("/chat")
    public List<Map<String, Object>> chat(HttpServletRequest request) {
        return appDataService.listChat(appDataService.requireUserId(request));
    }

    @PostMapping("/chat")
    public Map<String, Object> sendChat(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        appDataService.sendChat(appDataService.requireUserId(request), reqString(body, "ask"));
        return Map.of("ok", true);
    }

    @GetMapping("/trips")
    public List<Map<String, Object>> trips(HttpServletRequest request) {
        return appDataService.listTrips(appDataService.requireUserId(request));
    }

    @PostMapping("/trips")
    public Map<String, Object> saveTrip(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long id = appDataService.saveTripReturningId(appDataService.requireUserId(request), body);
        return Map.of("ok", true, "id", id);
    }

    @PatchMapping("/trips")
    public Map<String, Object> updateTrip(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long id = reqLong(body, "id");
        boolean ok = appDataService.updateTrip(appDataService.requireUserId(request), id, body);
        return Map.of("ok", ok);
    }

    @DeleteMapping("/trips")
    public Map<String, Object> deleteTrips(@RequestParam String ids, HttpServletRequest request) {
        appDataService.deleteTrips(appDataService.requireUserId(request), ids);
        return Map.of("ok", true);
    }

    private static String reqString(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) throw new IllegalArgumentException("missing field: " + key);
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) throw new IllegalArgumentException("empty field: " + key);
        return s;
    }

    private static Long reqLong(Map<String, Object> body, String key) {
        return Long.valueOf(reqString(body, key));
    }
}
