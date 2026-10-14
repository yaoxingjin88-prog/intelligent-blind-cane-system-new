package com.ruoyi.common;

/** 请求过于频繁，由全局处理器映射为 HTTP 429 */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
