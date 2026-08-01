package com.eduagent.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * JWT 工具。使用 jjwt 0.12.x API。
 * 注意：HS256 要求密钥至少 256 bit；请在 Nacos/配置中心设置 edu-agent.jwt.secret（>=32 字节）。
 */
@Component
public class JwtUtil {

    @Value("${edu-agent.jwt.secret:change-me-this-is-a-placeholder-secret-please-set-in-nacos}")
    private String secret;

    @Value("${edu-agent.jwt.expiration:86400000}")
    private long expirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String userId, String roles) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(userId)
                .claims(Map.of("roles", roles))
                .issuedAt(now)
                .expiration(exp)
                .signWith(key())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUserId(String token) {
        return parseToken(token).getSubject();
    }

    public String getRoles(String token) {
        return parseToken(token).get("roles", String.class);
    }

    public boolean isExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }
}
