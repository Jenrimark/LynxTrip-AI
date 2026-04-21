package com.lynxtrip.service;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import com.lynxtrip.api.dto.AuthDtos.AuthUser;
import com.lynxtrip.domain.UserEntity;
import com.lynxtrip.mapper.UserMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final Pattern PHONE_11 = Pattern.compile("^1\\d{10}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{1,19}$");
    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserMapper userMapper, JdbcTemplate jdbcTemplate) {
        this.userMapper = userMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public AuthUser register(String accountRaw, String password, String xingming, String xingbie) {
        String account = normalizeAccount(accountRaw);
        if (!validAccount(account)) {
            throw new IllegalArgumentException("账号格式不正确");
        }
        if (userMapper.findByAccount(account) != null) {
            throw new IllegalStateException("账号已存在");
        }
        UserEntity user = new UserEntity();
        user.setYonghuming(account);
        user.setMima(encoder.encode(password));
        user.setXingming(blankToNull(xingming));
        user.setTouxiang("");
        user.setXingbie(blankToNull(xingbie));
        user.setLianxidianhua(PHONE_11.matcher(account).matches() ? account : null);
        user.setMoney(BigDecimal.ZERO);
        user.setShimingrenzheng("未认证");
        userMapper.insert(user);
        return toAuthUser(userMapper.findById(user.getId()));
    }

    public AuthUser login(String accountRaw, String password) {
        String account = normalizeAccount(accountRaw);
        UserEntity user = userMapper.findByAccount(account);
        if (user == null || !encoder.matches(password, user.getMima())) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        return toAuthUser(user);
    }

    public AuthUser me(Long userId) {
        UserEntity user = userMapper.findById(userId);
        return user == null ? null : toAuthUser(user);
    }

    public boolean updatePassword(Long userId, String oldPassword, String newPassword) {
        UserEntity user = userMapper.findById(userId);
        if (user == null || !encoder.matches(oldPassword, user.getMima())) {
            return false;
        }
        return userMapper.updatePassword(userId, encoder.encode(newPassword)) > 0;
    }

    public AuthUser updateProfile(Long userId, String xingming, String xingbie, String lianxidianhua, String touxiang) {
        UserEntity user = userMapper.findById(userId);
        if (user == null) {
            return null;
        }
        user.setXingming(blankToNull(xingming));
        user.setXingbie(blankToNull(xingbie));
        user.setLianxidianhua(blankToNull(lianxidianhua));
        user.setTouxiang(blankToNull(touxiang));
        userMapper.updateProfile(user);
        return toAuthUser(userMapper.findById(userId));
    }

    @Transactional
    public boolean deleteAccount(Long userId) {
        if (userId == null || userId <= 0) {
            return false;
        }
        if (userMapper.findById(userId) == null) {
            return false;
        }
        jdbcTemplate.update("DELETE FROM shopping_cart WHERE userid=?", userId);
        jdbcTemplate.update("DELETE FROM purchase_orders WHERE userid=?", userId);
        jdbcTemplate.update("DELETE FROM user_addresses WHERE userid=?", userId);
        jdbcTemplate.update("DELETE FROM favorites WHERE userid=?", userId);
        jdbcTemplate.update("DELETE FROM support_chats WHERE userid=?", userId);
        jdbcTemplate.update("DELETE FROM user_gallery WHERE userid=?", userId);
        jdbcTemplate.update("DELETE FROM trip_plans WHERE userid=?", userId);
        return jdbcTemplate.update("DELETE FROM users WHERE id=?", userId) > 0;
    }

    private AuthUser toAuthUser(UserEntity u) {
        return new AuthUser(
                u.getId(),
                u.getYonghuming(),
                u.getXingming(),
                u.getXingbie(),
                u.getLianxidianhua(),
                u.getShimingrenzheng(),
                u.getTouxiang());
    }

    private static String normalizeAccount(String raw) {
        return String.valueOf(raw == null ? "" : raw).trim();
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    private static boolean validAccount(String account) {
        return PHONE_11.matcher(account).matches() || USERNAME_PATTERN.matcher(account).matches();
    }
}
