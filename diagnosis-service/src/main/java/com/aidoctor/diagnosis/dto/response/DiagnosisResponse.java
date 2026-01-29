package com.aidoctor.diagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 诊断响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisResponse {
    
    private Integer code;
    private String message;
    private String diagnosisId; // 兼容旧字段
    private String cdpId; // CDP ID
    private String status; // 诊断状态
    private String workMode; // 工作态：wellness_mode / clinical_mode
    private String currentStep; // 当前步骤
    private Double completeness; // 完整度（0-100）
    private QuestionResponse question; // 追问问题
    private DiagnosisResult result; // 诊断结果
    private Map<String, Object> nextAction; // 下一步操作
    private Map<String, Object> wellnessPlan; // 健康管理计划（健康管理态）
    private List<Map<String, Object>> ddx; // 鉴别诊断列表
    private List<Map<String, Object>> workupPlan; // 检查计划
    private List<Map<String, Object>> managementPlan; // 治疗计划
    private Map<String, Object> triage; // 风险评估
    private String assessmentReason; // 健康状态判定理由
    private String riskLevel; // 风险等级
    private List<String> redFlags; // 危险信号列表
    private Map<String, Object> entryAssessment; // 入口判定结果（P0模块）
    private Map<String, Object> patientState; // 患者状态摘要（用于前端显示已收集信息）
    private Long timestamp;
}

