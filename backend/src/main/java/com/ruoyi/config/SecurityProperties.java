package com.ruoyi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 安全相关配置，绑定 {@code app.security.*}
 * 生产环境由 ProdSecurityValidator 校验弱口令 / JWT / 设备密钥
 */
@Data
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    /** 是否启用 JWT 鉴权；本地联调可设 false */
    private boolean authEnabled = true;

    /** 是否允许测试验证码 123456（生产必须 false） */
    private boolean allowTestSmsCode = false;

    /** CORS / WebSocket 允许的源（支持 pattern，如 http://localhost:*），逗号分隔 */
    private String allowedOrigins = "https://intelligent.shinegoldyao.store,http://localhost:*,http://127.0.0.1:*";

    /** 管理端登录用户名 */
    private String adminUsername = "admin";

    /** 管理端密码：明文（开发）或 BCrypt 哈希（生产推荐） */
    private String adminPassword = "123456";

    /** JWT 签名密钥，生产长度建议 ≥32 */
    private String jwtSecret = "change-me-to-a-long-random-secret-at-least-32-bytes";

    /** JWT 过期时间（毫秒），默认 24 小时 */
    private long jwtExpirationMs = 1000L * 60 * 60 * 24;

    /**
     * 硬件上报 {@code POST /api/sensor-data} 的共享密钥。
     * 为空则不校验（便于本地联调）；生产建议配置并通过请求头 {@code X-Device-Key} 传递。
     */
    private String deviceApiKey = "";

    /**
     * 将逗号分隔的 allowedOrigins 拆成数组，供 CORS / WebSocket 使用
     *
     * @return 来源 pattern 数组，可能为空
     */
    public String[] allowedOriginArray() {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return new String[0];
        }
        return java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }
}
