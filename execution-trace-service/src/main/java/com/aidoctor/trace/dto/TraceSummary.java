package com.aidoctor.trace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 追踪摘要DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceSummary {
    private Long totalDuration;
    private Map<String, Long> serviceCalls;
    private List<StepSummary> steps;
    private Long errorCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepSummary {
        private String step;
        private Long startTime;
        private List<String> services;
    }

    /**
     * 转换为Map格式（用于更新CDP的executionTrace字段）
     */
    public Map<String, Object> toSummaryMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("totalDuration", totalDuration);
        map.put("serviceCalls", serviceCalls);
        map.put("errorCount", errorCount);
        
        List<Map<String, Object>> stepsList = new ArrayList<>();
        if (steps != null) {
            for (StepSummary step : steps) {
                Map<String, Object> stepMap = new HashMap<>();
                stepMap.put("step", step.getStep());
                stepMap.put("startTime", step.getStartTime());
                stepMap.put("services", step.getServices());
                stepsList.add(stepMap);
            }
        }
        map.put("steps", stepsList);
        
        return map;
    }
}


