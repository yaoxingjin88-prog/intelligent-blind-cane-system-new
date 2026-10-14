package com.ruoyi.common;

/** 未登录或令牌无效，由全局处理器映射为 HTTP 401 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
