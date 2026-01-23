"""
终点结论包构建器
生成终点结论包（四要素）
"""
import logging
from typing import Dict, Any, List, Optional
from app.utils.specific_exceptions import ConclusionPackageGenerationFailedException

logger = logging.getLogger(__name__)


class ConclusionPackageBuilder:
    """终点结论包构建器"""
    
    def build(self, cdp_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        构建终点结论包
        
        Args:
            cdp_data: CDP数据
            
        Returns:
            终点结论包
        """
        try:
            # 1. 构建结论
            conclusion = self._build_conclusion(cdp_data)
            
            # 2. 构建必须排除项状态
            must_exclude_status = self._build_must_exclude_status(cdp_data)
            
            # 3. 构建关键依据
            key_evidence = self._build_key_evidence(cdp_data, min_count=3)
            
            # 4. 构建行动与随访
            action_and_followup = self._build_action_and_followup(cdp_data)
            
            return {
                "conclusion": conclusion,
                "mustExcludeStatus": must_exclude_status,
                "keyEvidence": key_evidence,
                "actionAndFollowUp": action_and_followup
            }
        except Exception as e:
            logger.error(f"终点结论包生成失败: {str(e)}", exc_info=True)
            raise ConclusionPackageGenerationFailedException(str(e))
    
    def _build_conclusion(self, cdp_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        构建结论
        
        Args:
            cdp_data: CDP数据
            
        Returns:
            结论
        """
        ddx = cdp_data.get("ddx", {})
        primary_hypothesis = ddx.get("primary_hypothesis", [])
        
        if not primary_hypothesis:
            return {
                "type": "uncertain",
                "diagnosis": None,
                "confidence": 0.0,
                "severity": "unknown",
                "uncertaintySource": "缺少诊断信息"
            }
        
        # 获取主要假设
        primary = primary_hypothesis[0] if isinstance(primary_hypothesis, list) else primary_hypothesis
        
        if isinstance(primary, dict):
            diagnosis = primary.get("disease", primary.get("name", ""))
            confidence = primary.get("confidence", primary.get("score", 0.0))
            severity = primary.get("severity", "moderate")
        else:
            diagnosis = str(primary)
            confidence = 0.8
            severity = "moderate"
        
        # 确定结论类型
        if confidence >= 0.8:
            conclusion_type = "likely_diagnosis"
        elif confidence >= 0.5:
            conclusion_type = "possible_diagnosis"
        else:
            conclusion_type = "uncertain"
        
        return {
            "type": conclusion_type,
            "diagnosis": diagnosis,
            "confidence": float(confidence) if isinstance(confidence, (int, float)) else 0.0,
            "severity": severity if isinstance(severity, str) else "moderate",
            "uncertaintySource": "需要进一步检查确认" if confidence < 0.8 else None
        }
    
    def _build_must_exclude_status(self, cdp_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        构建必须排除项状态
        
        Args:
            cdp_data: CDP数据
            
        Returns:
            必须排除项状态
        """
        ddx = cdp_data.get("ddx", {})
        must_exclude = ddx.get("must_exclude", [])
        risk_assessment = cdp_data.get("risk_assessment", {})
        
        excluded = []
        not_excluded = []
        need_offline_exclude = []
        
        if isinstance(must_exclude, list):
            for item in must_exclude:
                if isinstance(item, dict):
                    disease = item.get("disease", item.get("name", ""))
                    status = item.get("status", "not_excluded")
                    
                    if status == "excluded":
                        excluded.append(disease)
                    elif status == "need_offline_exclude":
                        need_offline_exclude.append(disease)
                    else:
                        not_excluded.append(disease)
                elif isinstance(item, str):
                    not_excluded.append(item)
        
        # 从风险评估中获取高危诊断
        if isinstance(risk_assessment, dict):
            high_risk_diseases = risk_assessment.get("high_risk_diseases", [])
            if isinstance(high_risk_diseases, list):
                for disease in high_risk_diseases:
                    disease_name = disease.get("disease", disease.get("name", str(disease))) if isinstance(disease, dict) else str(disease)
                    if disease_name not in not_excluded and disease_name not in excluded:
                        not_excluded.append(disease_name)
        
        return {
            "excluded": excluded,
            "notExcluded": not_excluded,
            "needOfflineExclude": need_offline_exclude
        }
    
    def _build_key_evidence(
        self,
        cdp_data: Dict[str, Any],
        min_count: int = 3
    ) -> List[Dict[str, Any]]:
        """
        构建关键依据
        
        Args:
            cdp_data: CDP数据
            min_count: 最少证据数量
            
        Returns:
            关键依据列表
        """
        key_evidence = []
        patient_state = cdp_data.get("patient_state", {})
        evidence_graph = cdp_data.get("evidence_graph", {})
        
        # 从patient_state提取关键证据
        symptoms = patient_state.get("symptoms", [])
        if isinstance(symptoms, list):
            for symptom in symptoms[:3]:
                if isinstance(symptom, dict):
                    key_evidence.append({
                        "type": "positive",
                        "description": symptom.get("name", str(symptom)),
                        "strength": "strong"
                    })
                else:
                    key_evidence.append({
                        "type": "positive",
                        "description": str(symptom),
                        "strength": "strong"
                    })
        
        # 从evidence_graph提取证据
        if isinstance(evidence_graph, dict):
            evidence_list = evidence_graph.get("evidence", [])
            if isinstance(evidence_list, list):
                for evidence in evidence_list:
                    if isinstance(evidence, dict):
                        key_evidence.append({
                            "type": evidence.get("type", "positive"),
                            "description": evidence.get("item", evidence.get("description", "")),
                            "strength": evidence.get("strength", "medium")
                        })
                    elif len(key_evidence) < min_count:
                        key_evidence.append({
                            "type": "positive",
                            "description": str(evidence),
                            "strength": "medium"
                        })
        
        # 如果证据不足，添加默认证据
        while len(key_evidence) < min_count:
            key_evidence.append({
                "type": "positive",
                "description": "需要进一步收集信息",
                "strength": "weak"
            })
        
        return key_evidence[:min_count * 2]  # 最多返回min_count*2条
    
    def _build_action_and_followup(self, cdp_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        构建行动与随访
        
        Args:
            cdp_data: CDP数据
            
        Returns:
            行动与随访
        """
        workup_plan = cdp_data.get("workup_plan", {})
        treatment_plan = cdp_data.get("treatment_plan", {})
        risk_assessment = cdp_data.get("risk_assessment", {})
        conclusion = self._build_conclusion(cdp_data)
        
        # 构建立即行动
        immediate_action = {
            "medicalAdvice": "建议尽快就医",
            "examinations": [],
            "treatmentDirection": None
        }
        
        # 从检查计划中提取检查项
        if isinstance(workup_plan, dict):
            examinations = workup_plan.get("examinations", [])
            if isinstance(examinations, list):
                immediate_action["examinations"] = [
                    exam.get("name", str(exam)) if isinstance(exam, dict) else str(exam)
                    for exam in examinations[:5]
                ]
        
        # 从治疗方案中提取治疗方向
        if isinstance(treatment_plan, dict):
            treatment_direction = treatment_plan.get("direction", treatment_plan.get("summary", ""))
            if treatment_direction:
                immediate_action["treatmentDirection"] = str(treatment_direction)
        
        # 根据结论调整建议
        if conclusion.get("confidence", 0.0) < 0.5:
            immediate_action["medicalAdvice"] = "建议进一步检查以明确诊断"
        
        # 构建复评时间窗
        review_time_window = {
            "defaultTime": "3-7天",
            "earlyReviewConditions": ["症状加重", "出现新症状"]
        }
        
        # 从风险评估中获取升级条件
        upgrade_conditions = ["持续症状", "症状加重"]
        if isinstance(risk_assessment, dict):
            risk_level = risk_assessment.get("risk_level", "L2")
            if risk_level in ["L3", "L4"]:
                upgrade_conditions.append("高风险状态")
                review_time_window["defaultTime"] = "1-3天"
        
        return {
            "immediateAction": immediate_action,
            "reviewTimeWindow": review_time_window,
            "upgradeConditions": upgrade_conditions
        }

