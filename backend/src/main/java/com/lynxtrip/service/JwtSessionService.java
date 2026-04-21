package com.lynxtrip.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtSessionService {
    private static final Duration EXPIRES = Duration.ofDays(7);

    private final SecretKey key;
    private final String issuer;

    public JwtSessionService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer:lynxtrip}") String issuer) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        byte[] fixed = new byte[Math.max(bytes.length, 32)];
        System.arraycopy(bytes, 0, fixed, 0, Math.min(bytes.length, fixed.length));
        this.key = Keys.hmacShaKeyFor(fixed);
        this.issuer = issuer;
    }

    public String issue(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(EXPIRES)))
                .signWith(key)
                .compact();
    }

    public Optional<Long> parseUserId(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            if (!issuer.equals(claims.getIssuer())) {
                return Optional.empty();
            }
            return Optional.of(Long.parseLong(claims.getSubject()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
