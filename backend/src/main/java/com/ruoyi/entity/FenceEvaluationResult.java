package com.ruoyi.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 电子围栏单次判定结果（非落库，供推送与告警链路使用）
 */
@Data
@AllArgsConstructor
public class FenceEvaluationResult {
    /** 参与判定的围栏 */
    private ElectronicFence fence;
    /** 当前位置到圆心距离（米） */
    private Double distanceMeters;
    /** 是否在围栏外 */
    private Boolean outside;
    /** 是否本次触发越界告警（仅 INSIDE→OUTSIDE 边沿为 true） */
    private Boolean triggered;
}
