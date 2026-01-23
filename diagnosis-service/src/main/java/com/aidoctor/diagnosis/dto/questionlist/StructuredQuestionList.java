package com.aidoctor.diagnosis.dto.questionlist;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * 结构化问题清单
 * 核心目标：把用户的自然语言描述，转化成可推理、可复用、可审计的结构化"问题清单"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructuredQuestionList {
    
    /**
     * 主要问题（用户最关注的主诉）
     */
    private ChiefComplaint chiefComplaint;
    
    /**
     * 伴随问题（同时间窗出现的关键症状/体征）
     */
    private List<AccompanyingSymptom> accompanyingSymptoms;
    
    /**
     * 关键背景（诊断错误常源于信息采集偏差，必须纳入）
     */
    private KeyBackground keyBackground;
    
    /**
     * 生命体征（主动询问或设备采集）
     */
    private VitalSigns vitalSigns;
    
    /**
     * 检查结果（用户上传）
     */
    private List<ExaminationResult> examinationResults;
    
    /**
     * 历史诊断（从诊断记录获取）
     */
    private List<HistoricalDiagnosis> historicalDiagnoses;
    
    /**
     * 信息缺口标记
     */
    private InformationGaps informationGaps;
    
    /**
     * 信息完整度（0-1）
     */
    private Double completeness;
    
    /**
     * 主要问题
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChiefComplaint {
        private String name;  // 归一化后的主诉名称
        private String originalText;  // 用户原始描述
        private String duration;  // 持续时间
        private String onsetMode;  // 起病方式（sudden/gradual）
        private Integer severity;  // 严重程度（0-10）
        private String frequency;  // 频率（continuous/intermittent/occasional）
        private String location;  // 位置
        private Map<String, Object> features;  // 症状特征（诱因、缓解因素等）
        private String triageLevel;  // 分诊等级（L1-L4）
    }
    
    /**
     * 伴随症状
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccompanyingSymptom {
        private String name;
        private String startTime;
        private Integer severity;
    }
    
    /**
     * 关键背景
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeyBackground {
        private Integer age;
        private String gender;
        private List<String> medicalHistory;
        private List<String> medicationHistory;
        private List<String> familyHistory;
        private Map<String, Object> lifestyle;
        private List<String> recentEvents;
    }
    
    /**
     * 生命体征
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VitalSigns {
        private BloodPressure bloodPressure;
        private Integer heartRate;
        private Double temperature;
        private Integer oxygenSaturation;
        private Integer respiratoryRate;
        
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class BloodPressure {
            private Integer systolic;
            private Integer diastolic;
        }
    }
    
    /**
     * 检查结果（简化版）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExaminationResult {
        private String type;
        private String name;
        private String date;
        private Map<String, Object> data;
    }
    
    /**
     * 历史诊断（简化版）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoricalDiagnosis {
        private String diagnosis;
        private String date;
        private String status;
    }
}

