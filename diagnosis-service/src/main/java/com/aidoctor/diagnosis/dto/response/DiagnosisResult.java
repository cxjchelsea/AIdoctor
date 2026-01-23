package com.aidoctor.diagnosis.dto.response;

import com.aidoctor.diagnosis.dto.threeLayer.ThreeLayerResult;
import com.aidoctor.diagnosis.dto.conclusion.ConclusionPackage;
import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * 诊断结果
 */
@Data
@Builder
public class DiagnosisResult {
    
    private String summary;
    
    /**
     * 可能性列表（Top 3-5）（兼容旧版本）
     */
    private List<DiseasePossibility> possibilities;
    
    /**
     * 三层分层结果（新增，根据系统设计方案v2.0）
     */
    private ThreeLayerResult threeLayerResult;
    
    /**
     * 终点结论包（新增，根据系统设计方案v2.0）
     * 包含四要素：结论、必须排除项状态、关键依据、行动与随访
     */
    private ConclusionPackage conclusionPackage;
    
    private ExaminationSuggestion examinationSuggestion;
    private MedicalAdvice medicalAdvice;
    private Precautions precautions;
    private String reasoning;
    
    @Data
    @Builder
    public static class DiseasePossibility {
        private String disease;
        private String level;
        private Double confidence;
        private List<String> supportingEvidence;
        private List<String> opposingEvidence;
        private List<String> missingInfo;
    }
    
    @Data
    @Builder
    public static class ExaminationSuggestion {
        private List<ExaminationItem> priorityExaminations;
        private List<ExaminationItem> optionalExaminations;
        private String explanation;
    }
    
    @Data
    @Builder
    public static class ExaminationItem {
        private String name;
        private String purpose;
        private String priority;
        private String reason;
    }
    
    @Data
    @Builder
    public static class MedicalAdvice {
        private String department;
        private String timing;
        private MedicalPreparation preparation;
        private String sbarSummary;
    }
    
    @Data
    @Builder
    public static class MedicalPreparation {
        private List<String> documents;
        private List<String> questions;
    }
    
    @Data
    @Builder
    public static class Precautions {
        private List<String> observationPoints;
        private List<String> dangerSigns;
        private String disclaimer;
    }
}

