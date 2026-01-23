package com.aidoctor.diagnosis.dto.threeLayer;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * 三层分层结果
 * 根据系统设计方案：
 * - 首要假设（1个）：当前信息最能支持、最符合整体表现的方向
 * - 主要备选诊断（1-2个）：与首要假设并列需要对比、仍可能成立的方向
 * - 必须排除的高危诊断（0-1个）：一旦漏诊后果严重，即使概率不高也必须纳入并优先排除
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreeLayerResult {
    
    /**
     * 首要假设（1个）
     */
    private PrimaryHypothesis primaryHypothesis;
    
    /**
     * 主要备选诊断（1-2个）
     */
    private List<MainAlternative> mainAlternatives;
    
    /**
     * 必须排除的高危诊断（0-1个）
     */
    private MustExcludeDiagnosis mustExclude;
    
    /**
     * 所有候选（Top 5）
     */
    private Map<String, Double> allCandidates;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrimaryHypothesis {
        private String disease;
        private Double score;
        private String layer;
        private String evidence;  // 入选依据
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MainAlternative {
        private String disease;
        private Double score;
        private String layer;
        private String evidence;  // 入选依据
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MustExcludeDiagnosis {
        private String disease;
        private Double score;
        private String layer;
        private String reason;  // 为什么必须排除
    }
}

