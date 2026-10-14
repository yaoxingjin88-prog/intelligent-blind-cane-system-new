package com.ruoyi.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 统一接口返回封装
 */
@Data
@AllArgsConstructor
public class Result<T> {
    // 响应码：200成功；业务失败常用 4xx；未指定时默认 500
    private int code;
    // 提示信息
    private String msg;
    // 返回数据
    private T data;

    // 成功重载（无数据）
    public static Result success() {
        return new Result(200, "操作成功", null);
    }

    // 成功重载（带数据）
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    // 失败重载（默认系统错误 500）
    public static Result error(String msg) {
        return error(500, msg);
    }

    // 失败重载（显式业务/HTTP 语义码，如 400/401/403）
    public static Result error(int code, String msg) {
        return new Result(code, msg, null);
    }
}
