package com.aidoctor.diagnosis.agent;

import com.aidoctor.diagnosis.entity.AgentState;
import com.aidoctor.diagnosis.entity.CDP;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 停止条件评估器
 * 评估是否满足停止条件
 * 
 * 参考文档：
 * - 《5.主agent设计/主Agent核心算法设计.md》五、停止条件评估算法
 */
@Slf4j
@Component
public class StopConditionEvaluator {
    
    /**
     * 评估停止条件
     * 
     * @param cdp 当前CDP
     * @param agentState AgentState
     * @return 停止条件评估结果
     */
    @SuppressWarnings("unchecked")
    public StopConditionResult evaluate(CDP cdp, AgentState agentState) {
        log.debug("评估停止条件: cdpId={}", cdp.getId());
        
        List<String> satisfiedConditions = new ArrayList<>();
        List<String> unsatisfiedConditions = new ArrayList<>();
        Map<String, Object> stopConditions = agentState.getStopConditionsMap();
        
        // 1. 检查CDP必填项
        boolean cdpRequiredFieldsComplete = checkCDPRequiredFields(cdp);
        if (cdpRequiredFieldsComplete) {
            satisfiedConditions.add("cdp_required_fields_complete");
            stopConditions.put("cdp_required_fields_complete", true);
        } else {
            unsatisfiedConditions.add("cdp_required_fields_complete");
            stopConditions.put("cdp_required_fields_complete", false);
        }
        
        // 2. 检查证据引用齐全
        boolean evidenceReferencesComplete = checkEvidenceReferencesComplete(cdp);
        if (evidenceReferencesComplete) {
            satisfiedConditions.add("evidence_references_complete");
            stopConditions.put("evidence_references_complete", true);
        } else {
            unsatisfiedConditions.add("evidence_references_complete");
            stopConditions.put("evidence_references_complete", false);
        }
        
        // 3. 检查风险评估完成
        boolean riskAssessmentComplete = checkRiskAssessmentComplete(cdp);
        if (riskAssessmentComplete) {
            satisfiedConditions.add("risk_assessment_complete");
            stopConditions.put("risk_assessment_complete", true);
        } else {
            unsatisfiedConditions.add("risk_assessment_complete");
            stopConditions.put("risk_assessment_complete", false);
        }
        
        // 4. 检查诊断结论明确
        boolean diagnosisConclusionClear = checkDiagnosisConclusionClear(cdp);
        if (diagnosisConclusionClear) {
            satisfiedConditions.add("diagnosis_conclusion_clear");
            stopConditions.put("diagnosis_conclusion_clear", true);
        } else {
            unsatisfiedConditions.add("diagnosis_conclusion_clear");
            stopConditions.put("diagnosis_conclusion_clear", false);
        }
        
        // 5. 检查检查建议完成
        boolean workupPlanComplete = checkWorkupPlanComplete(cdp);
        if (workupPlanComplete) {
            satisfiedConditions.add("workup_plan_complete");
            stopConditions.put("workup_plan_complete", true);
        } else {
            unsatisfiedConditions.add("workup_plan_complete");
            stopConditions.put("workup_plan_complete", false);
        }
        
        // 6. 检查治疗建议完成
        boolean managementPlanComplete = checkManagementPlanComplete(cdp);
        if (managementPlanComplete) {
            satisfiedConditions.add("management_plan_complete");
            stopConditions.put("management_plan_complete", true);
        } else {
            unsatisfiedConditions.add("management_plan_complete");
            stopConditions.put("management_plan_complete", false);
        }
        
        // 7. 检查证据链完整
        boolean evidenceChainComplete = checkEvidenceChainComplete(cdp);
        if (evidenceChainComplete) {
            satisfiedConditions.add("evidence_chain_complete");
            stopConditions.put("evidence_chain_complete", true);
        } else {
            unsatisfiedConditions.add("evidence_chain_complete");
            stopConditions.put("evidence_chain_complete", false);
        }
        
        // 8. 检查终点结论包生成
        boolean finalConclusionGenerated = checkFinalConclusionGenerated(cdp);
        if (finalConclusionGenerated) {
            satisfiedConditions.add("final_conclusion_generated");
            stopConditions.put("final_conclusion_generated", true);
        } else {
            unsatisfiedConditions.add("final_conclusion_generated");
            stopConditions.put("final_conclusion_generated", false);
        }
        
        // 更新AgentState
        agentState.setStopConditionsMap(stopConditions);
        
        // 汇总评估结果
        boolean satisfied = unsatisfiedConditions.isEmpty();
        String evaluationReason = String.format("满足%d/8个停止条件", satisfiedConditions.size());
        if (!unsatisfiedConditions.isEmpty()) {
            evaluationReason += "，未满足：" + String.join("、", unsatisfiedConditions);
        }
        
        log.info("停止条件评估完成: cdpId={}, satisfied={}, satisfiedCount={}/8", 
            cdp.getId(), satisfied, satisfiedConditions.size());
        
        return StopConditionResult.builder()
            .satisfied(satisfied)
            .satisfiedConditions(satisfiedConditions)
            .unsatisfiedConditions(unsatisfiedConditions)
            .evaluationReason(evaluationReason)
            .build();
    }
    
    /**
     * 检查CDP必填项
     */
    private boolean checkCDPRequiredFields(CDP cdp) {
        return cdp.getHealthStateAssessment() != null 
            && cdp.getPatientState() != null 
            && cdp.getDdx() != null;
    }
    
    /**
     * 检查证据引用齐全
     */
    @SuppressWarnings("unchecked")
    private boolean checkEvidenceReferencesComplete(CDP cdp) {
        List<Map<String, Object>> rankList = cdp.getDdx();
        if (rankList == null) {
            return false;
        }

        if (rankList == null || rankList.isEmpty()) {
            return false;
        }
        
        for (Map<String, Object> diagnosis : rankList) {
            List<Map<String, Object>> evidenceRefs = (List<Map<String, Object>>) diagnosis.get("evidence_references");
            if (evidenceRefs == null || evidenceRefs.isEmpty()) {
                return false;
            }
            
            for (Map<String, Object> evidenceRef : evidenceRefs) {
                if (evidenceRef.get("source") == null || evidenceRef.get("reference") == null) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * 检查风险评估完成
     */
    private boolean checkRiskAssessmentComplete(CDP cdp) {
        Map<String, Object> triageMap = cdp.getTriage();
        return triageMap != null && triageMap.containsKey("risk_level");
    }
    
    /**
     * 检查诊断结论明确
     */
    @SuppressWarnings("unchecked")
    private boolean checkDiagnosisConclusionClear(CDP cdp) {
        List<Map<String, Object>> ddx = cdp.getDdx();
        return ddx != null && !ddx.isEmpty();
    }
    
    /**
     * 检查检查建议完成
     */
    private boolean checkWorkupPlanComplete(CDP cdp) {
        List<Map<String, Object>> workupPlan = cdp.getWorkupPlan();
        if (workupPlan != null && !workupPlan.isEmpty()) {
            return true;
        }
        
        // 或者明确不需要检查
        Map<String, Object> uncertaintyMap = cdp.getUncertainty();
        if (uncertaintyMap != null && Boolean.TRUE.equals(uncertaintyMap.get("no_workup_needed"))) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查治疗建议完成
     */
    private boolean checkManagementPlanComplete(CDP cdp) {
        List<Map<String, Object>> managementPlan = cdp.getManagementPlan();
        return managementPlan != null && !managementPlan.isEmpty();
    }
    
    /**
     * 检查证据链完整
     */
    @SuppressWarnings("unchecked")
    private boolean checkEvidenceChainComplete(CDP cdp) {
        List<Map<String, Object>> evidenceGraph = cdp.getEvidenceGraph();
        return evidenceGraph != null && !evidenceGraph.isEmpty();
    }
    
    /**
     * 检查终点结论包生成
     */
    private boolean checkFinalConclusionGenerated(CDP cdp) {
        Map<String, Object> patientState = cdp.getPatientState();
        Object conclusionPackage = patientState == null ? null : patientState.get("conclusion_package");
        return conclusionPackage instanceof Map && !((Map<?, ?>) conclusionPackage).isEmpty();
    }
    
    /**
     * 停止条件评估结果
     */
    @lombok.Data
    @lombok.Builder
    public static class StopConditionResult {
        /**
         * 是否满足停止条件
         */
        private boolean satisfied;
        
        /**
         * 满足的条件列表
         */
        private List<String> satisfiedConditions;
        
        /**
         * 未满足的条件列表
         */
        private List<String> unsatisfiedConditions;
        
        /**
         * 评估依据
         */
        private String evaluationReason;
    }
}

