package com.ruoyi.entity;

import lombok.Data;
import org.apache.ibatis.type.Alias;

/**
 * 过马路辅助识别结果快照（视觉模块上报，供小程序/监护端展示）
 */
@Data
@Alias("CrossingAssistSnapshot")
public class CrossingAssistSnapshot {
    /** 设备编号 */
    private String deviceId;
    /** 红绿灯状态：RED / YELLOW / GREEN / UNKNOWN */
    private String trafficLightStatus;
    /** 是否检测到斑马线 */
    private Boolean zebraCrossingDetected;
    /** 斑马线相对方向：LEFT / CENTER / RIGHT */
    private String zebraCrossingDirection;
    /** 是否有车辆靠近（粗提醒） */
    private Boolean vehicleApproaching;
    /** 通行建议枚举文案（如 WAIT / GO） */
    private String recommendation;
    /** 对用户播报/展示的完整提示语 */
    private String message;
    /** 识别置信度，0.0-1.0 */
    private Double confidence;
    /** 数据来源：vision / mock 等 */
    private String source;
    /** 更新时间 */
    private String updateTime;
}
