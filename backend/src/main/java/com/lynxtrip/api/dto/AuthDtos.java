package com.lynxtrip.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record AuthUser(
            Long id,
            String yonghuming,
            String xingming,
            String xingbie,
            String lianxidianhua,
            String shimingrenzheng,
            String touxiang) {
    }

    public record ApiResult(
            boolean ok,
            String message,
            AuthUser user) {
    }

    public record LoginRequest(
            @NotBlank(message = "账号不能为空") String account,
            @NotBlank(message = "密码不能为空") String mima) {
    }

    public record RegisterRequest(
            @NotBlank(message = "账号不能为空") @Size(max = 64, message = "账号过长") String account,
            @NotBlank(message = "密码不能为空") @Size(max = 128, message = "密码过长") String mima,
            @Size(max = 64, message = "姓名过长") String xingming,
            @Size(max = 16, message = "性别过长") String xingbie) {
    }

    public record UpdatePasswordRequest(
            @NotBlank(message = "旧密码不能为空") String oldMima,
            @NotBlank(message = "新密码不能为空") String newMima) {
    }
}
