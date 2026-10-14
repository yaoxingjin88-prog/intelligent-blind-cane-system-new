package com.ruoyi.common;

import com.ruoyi.entity.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常转统一 Result：401/429/400 与未捕获异常
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleUnauthorized(UnauthorizedException ex) {
        return Result.error(401, ex.getMessage() == null ? "未登录或令牌无效" : ex.getMessage());
    }

    @ExceptionHandler(TooManyRequestsException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Result<Void> handleTooManyRequests(TooManyRequestsException ex) {
        return Result.error(429, ex.getMessage() == null ? "请求过于频繁" : ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return Result.error(400, ex.getMessage() == null ? "参数错误" : ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(Exception ex) {
        String message = "参数校验失败";
        if (ex instanceof MethodArgumentNotValidException manv && manv.getBindingResult().getFieldError() != null) {
            message = manv.getBindingResult().getFieldError().getDefaultMessage();
        } else if (ex instanceof BindException be && be.getBindingResult().getFieldError() != null) {
            message = be.getBindingResult().getFieldError().getDefaultMessage();
        }
        return Result.error(400, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleOther(Exception ex) {
        log.error("未处理异常", ex);
        return Result.error(500, "服务器内部错误");
    }
}
