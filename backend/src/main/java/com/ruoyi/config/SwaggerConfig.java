package com.ruoyi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 文档配置
 * 定义标题、鉴权说明与 Bearer JWT 安全方案
 */
@Configuration
public class SwaggerConfig {

    /**
     * 自定义 OpenAPI 元数据（访问 /doc.html）
     *
     * @return OpenAPI 定义
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智能盲杖系统接口文档")
                        .version("1.0")
                        .description("""
                                ## 鉴权说明
                                - 管理端：`POST /api/login` 获取 JWT，请求头携带 `Authorization: Bearer <token>`。
                                - 小程序：短信登录/注册接口公开；其余小程序接口按业务校验 token。
                                - 设备上报：`POST /api/sensor-data` 不走 JWT；若配置了 `DEVICE_API_KEY`，请求头需带 `X-Device-Key`。
                                - Knife4j 右上角 Authorize 填入 Bearer Token 后即可调试受保护接口。

                                ## 错误码约定（HTTP + body.code）
                                - `200`：成功
                                - `400`：参数错误 / 校验失败
                                - `401`：未登录或 token 无效
                                - `429`：触发限流（AI / 传感器上报）
                                - `500`：服务器内部错误

                                ## 限流
                                - `/api/ai/**`：按客户端 IP，默认 20 次/分钟
                                - `POST /api/sensor-data`：按设备或 IP，默认 120 次/分钟
                                """))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(
                        new io.swagger.v3.oas.models.Components()
                                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .name("Authorization")
                                        .description("在此粘贴登录返回的 JWT（无需手写 Bearer 前缀）")
                                        .in(SecurityScheme.In.HEADER)
                                )
                );
    }
}
