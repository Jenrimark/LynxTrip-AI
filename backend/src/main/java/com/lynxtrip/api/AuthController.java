package com.lynxtrip.api;

import java.time.Duration;
import java.util.Optional;

import com.lynxtrip.api.dto.AuthDtos.ApiResult;
import com.lynxtrip.api.dto.AuthDtos.LoginRequest;
import com.lynxtrip.api.dto.AuthDtos.RegisterRequest;
import com.lynxtrip.api.dto.AuthDtos.UpdateProfileRequest;
import com.lynxtrip.api.dto.AuthDtos.UpdatePasswordRequest;
import com.lynxtrip.api.dto.AuthDtos.AuthUser;
import com.lynxtrip.service.AuthService;
import com.lynxtrip.service.JwtSessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String SESSION_COOKIE = "lynxtrip_session";
    private final AuthService authService;
    private final JwtSessionService jwtSessionService;

    public AuthController(AuthService authService, JwtSessionService jwtSessionService) {
        this.authService = authService;
        this.jwtSessionService = jwtSessionService;
    }

    @PostMapping("/register")
    public ApiResult register(@Valid @RequestBody RegisterRequest req, HttpServletResponse response) {
        try {
            AuthUser user = authService.register(req.account(), req.password(), req.displayName(), req.gender());
            setSessionCookie(response, user.id());
            return new ApiResult(true, "注册成功", user);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/login")
    public ApiResult login(@Valid @RequestBody LoginRequest req, HttpServletResponse response) {
        try {
            AuthUser user = authService.login(req.account(), req.password());
            setSessionCookie(response, user.id());
            return new ApiResult(true, "登录成功", user);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    @GetMapping("/me")
    public ApiResult me(HttpServletRequest request) {
        Long userId = mustReadUserId(request);
        AuthUser user = authService.me(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录态失效");
        }
        return new ApiResult(true, "ok", user);
    }

    @PatchMapping("/password")
    public ApiResult updatePassword(@Valid @RequestBody UpdatePasswordRequest req, HttpServletRequest request) {
        Long userId = mustReadUserId(request);
        boolean ok = authService.updatePassword(userId, req.oldPassword(), req.newPassword());
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "原密码错误或新密码无效");
        }
        AuthUser user = authService.me(userId);
        return new ApiResult(true, "密码已更新", user);
    }

    @PatchMapping("/profile")
    public ApiResult updateProfile(@Valid @RequestBody UpdateProfileRequest req, HttpServletRequest request) {
        Long userId = mustReadUserId(request);
        AuthUser user = authService.updateProfile(userId, req.displayName(), req.gender(), req.phone(), req.avatarUrl());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录态失效");
        }
        return new ApiResult(true, "资料已更新", user);
    }

    @PostMapping("/logout")
    public ApiResult logout(HttpServletResponse response) {
        clearSessionCookie(response);
        return new ApiResult(true, "已退出登录", null);
    }

    @DeleteMapping("/account")
    public ApiResult deleteAccount(HttpServletRequest request, HttpServletResponse response) {
        Long userId = mustReadUserId(request);
        boolean ok = authService.deleteAccount(userId);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号注销失败");
        }
        clearSessionCookie(response);
        return new ApiResult(true, "账号已注销", null);
    }

    private Long mustReadUserId(HttpServletRequest request) {
        String token = readCookie(request, SESSION_COOKIE);
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        Optional<Long> uid = jwtSessionService.parseUserId(token);
        if (uid.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录态失效");
        }
        return uid.get();
    }

    private static String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    private void setSessionCookie(HttpServletResponse response, Long userId) {
        String token = jwtSessionService.issue(userId);
        ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE, token)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(7))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearSessionCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
