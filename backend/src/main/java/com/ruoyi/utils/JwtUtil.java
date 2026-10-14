package com.ruoyi.utils;

import com.ruoyi.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 签发与校验工具
 * 密钥、过期时间来自 {@link SecurityProperties}
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final SecurityProperties securityProperties;

    /** 生成 HMAC 签名密钥；本地短密钥会填充至 32 字节（生产由校验器强制足够长度） */
    private SecretKey getSigningKey() {
        byte[] keyBytes = securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // 仅兼容本地短密钥启动；生产由 ProdSecurityValidator 强制足够长度，勿依赖此填充
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 校验令牌签名与过期
     *
     * @param token JWT 字符串
     * @return 有效 true，否则 false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 管理端：从令牌取 subject（用户名） */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /** 小程序端：从令牌取 subject（监护人 id） */
    public String getUserIdFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 签发令牌
     *
     * @param subject 主体标识（用户名或用户 id）
     * @return JWT 字符串
     */
    public String generateToken(String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + securityProperties.getJwtExpirationMs()))
                .signWith(getSigningKey())
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
