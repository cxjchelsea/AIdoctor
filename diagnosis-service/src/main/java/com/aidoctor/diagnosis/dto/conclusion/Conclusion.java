package com.aidoctor.diagnosis.dto.conclusion;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * 结论
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conclusion {
    private ConclusionType type;  // CONFIRMED / PROBABLE
    private String diagnosis;
    private Double confidence;
    private String uncertaintyReason;  // 不可确证时的不确定性来源
    private String reviewWindow;  // 复评时间窗
    private List<String> upgradeTriggers;  // 升级触发条件
    
    public enum ConclusionType {
        CONFIRMED,    // 可确证终点
        PROBABLE      // 不可确证终点
    }
}

