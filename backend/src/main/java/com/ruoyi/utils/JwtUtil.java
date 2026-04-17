package com.ruoyi.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    // 建议从配置文件读取，这里先写死示例
    private static final String SECRET = "your-256-bit-secret-key-here-please-make-it-long-enough";
    private static final long EXPIRATION = 1000 * 60 * 60 * 24; // 24小时

    // 生成密钥（新版必须用SecretKey，不能直接用字符串）
    private static SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    // 新版token验证方法
    public static boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey()) // 新版用verifyWith替代setSigningKey
                    .build()
                    .parseSignedClaims(token); // 新版用parseSignedClaims替代parseClaimsJws
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 新版从token获取用户名
    public static String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload(); // 新版用getPayload替代getBody
        return claims.getSubject(); // 通常用户名存在subject字段
    }

    // 生成token（补充完整工具类）
    public static String generateToken(String username) {
        return Jwts.builder()
                .subject(username) // 新版用subject替代setSubject
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSigningKey())
                .compact();
    }

    // 从token获取用户ID
    public static String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject(); // 使用subject存储用户ID
    }
}