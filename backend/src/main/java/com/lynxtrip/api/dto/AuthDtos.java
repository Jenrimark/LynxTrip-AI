package com.lynxtrip.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record AuthUser(
            Long id,
            String username,
            String displayName,
            String gender,
            String phone,
            String identityStatus,
            String avatarUrl) {
    }

    public record ApiResult(
            boolean ok,
            String message,
            AuthUser user) {
    }

    public record LoginRequest(
            @NotBlank(message = "账号不能为空") String account,
            @NotBlank(message = "密码不能为空") String password) {
    }

    public record RegisterRequest(
            @NotBlank(message = "账号不能为空") @Size(max = 64, message = "账号过长") String account,
            @NotBlank(message = "密码不能为空") @Size(max = 128, message = "密码过长") String password,
            @Size(max = 64, message = "昵称过长") String displayName,
            @Size(max = 16, message = "性别过长") String gender) {
    }

    public record UpdatePasswordRequest(
            @NotBlank(message = "旧密码不能为空") String oldPassword,
            @NotBlank(message = "新密码不能为空") String newPassword) {
    }

    public record UpdateProfileRequest(
            @Size(max = 64, message = "昵称过长") String displayName,
            @Size(max = 16, message = "性别过长") String gender,
            @Size(max = 32, message = "电话过长") String phone,
            String avatarUrl) {
    }
}
