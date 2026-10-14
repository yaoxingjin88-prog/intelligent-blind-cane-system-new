package com.ruoyi.common;

/**
 * 报警处理状态常量
 * 库内历史可能混用中文/英文，判断时见 {@link #isPending(String)}
 */
public final class AlarmStatus {

    /** 待处理 */
    public static final String PENDING = "0";
    /** 已处理 */
    public static final String HANDLED = "1";

    private AlarmStatus() {
    }

    public static boolean isPending(String status) {
        return status == null
                || PENDING.equals(status)
                || "未处理".equals(status)
                || "pending".equalsIgnoreCase(status);
    }

    public static boolean isHandled(String status) {
        return !isPending(status);
    }
}
