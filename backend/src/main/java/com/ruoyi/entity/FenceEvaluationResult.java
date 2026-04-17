package com.ruoyi.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FenceEvaluationResult {
    private ElectronicFence fence;
    private Double distanceMeters;
    private Boolean outside;
    private Boolean triggered;
}
