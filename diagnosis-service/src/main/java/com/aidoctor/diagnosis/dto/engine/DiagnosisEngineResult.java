package com.aidoctor.diagnosis.dto.engine;

import lombok.Data;
import lombok.Builder;
import java.util.Map;

/**
 * 诊断引擎响应
 */
@Data
@Builder
public class DiagnosisEngineResult {
    
    private Map<String, Double> possibilities;
    private EngineResults engineResults;
    
    @Data
    @Builder
    public static class EngineResults {
        private Map<String, Object> ruleEngine;
        private Map<String, Object> knowledgeGraph;
        private Map<String, Object> statistical;
        private Map<String, Object> llm;
        private Map<String, Object> differential;
    }
}

