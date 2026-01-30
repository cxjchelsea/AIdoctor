"""
终点结论包构建器
"""
from typing import Dict, Any, List, Optional
import logging

logger = logging.getLogger(__name__)


class ConclusionPackageBuilder:
    """终点结论包构建器"""
    
    def __init__(self):
        """初始化终点结论包构建器"""
        pass
    
    def _build_conclusion(
        self,
        three_layer_result: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        构建诊断结论
        
        Args:
            three_layer_result: 三层分层结果
            
        Returns:
            诊断结论
        """
        primary_hypothesis = three_layer_result.get("primary_hypothesis", {})
        
        if primary_hypothesis:
            disease = primary_hypothesis.get("disease", "")
            probability = primary_hypothesis.get("probability", 0.0)
            
            return {
                "primary_diagnosis": disease,
                "confidence": round(probability, 2)
            }
        
        return {
            "primary_diagnosis": None,
            "confidence": 0.0
        }
    
    def _build_must_exclude_status(
        self,
        three_layer_result: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        构建必须排除项状态
        
        Args:
            three_layer_result: 三层分层结果
            
        Returns:
            必须排除项状态
        """
        must_exclude = three_layer_result.get("must_exclude", [])
        
        status = {}
        for item in must_exclude:
            if isinstance(item, dict):
                disease = item.get("disease", "")
                reason = item.get("reason", "高危诊断，必须排除")
                
                if disease:
                    # 简化处理：使用疾病名称作为key（实际可能需要编码）
                    disease_key = disease.replace(" ", "_").lower()
                    status[disease_key] = {
                        "status": "pending",
                        "reason": reason
                    }
        
        return status
    
    def _extract_key_evidence(
        self,
        evidence_analysis: Dict[str, Any]
    ) -> List[Dict[str, Any]]:
        """
        提取关键依据
        
        Args:
            evidence_analysis: 证据分析结果
            
        Returns:
            关键依据列表
        """
        key_evidence = []
        
        # 从证据分析中提取关键证据
        if isinstance(evidence_analysis, dict):
            evidence_items = evidence_analysis.get("key_evidence", [])
            if evidence_items:
                key_evidence = evidence_items
            else:
                # 如果没有key_evidence字段，尝试从其他字段提取
                supporting_evidence = evidence_analysis.get("supporting_evidence", [])
                if supporting_evidence:
                    # 选择强度高的证据
                    for evidence in supporting_evidence[:5]:  # 最多5个
                        if isinstance(evidence, dict):
                            strength = evidence.get("strength", "medium")
                            if strength in ["strong", "very_strong"]:
                                key_evidence.append(evidence)
        
        return key_evidence
    
    def _build_action_and_follow_up(
        self,
        three_layer_result: Dict[str, Any],
        risk_level: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        构建行动与随访
        
        Args:
            three_layer_result: 三层分层结果
            risk_level: 风险等级（可选）
            
        Returns:
            行动与随访
        """
        immediate_actions = []
        follow_up_plan = {}
        
        # 根据风险等级确定行动
        if risk_level:
            if risk_level in ["L1", "L2"]:
                immediate_actions.append("建议立即就医")
                immediate_actions.append("监测生命体征")
            elif risk_level == "L3":
                immediate_actions.append("建议尽快就医")
            else:
                immediate_actions.append("建议关注症状变化")
        
        # 根据必须排除项确定行动
        must_exclude = three_layer_result.get("must_exclude", [])
        if must_exclude:
            immediate_actions.append("需要排除高危诊断")
        
        # 构建随访计划
        if risk_level:
            if risk_level in ["L1", "L2"]:
                follow_up_plan = {
                    "review_time": "24小时内",
                    "review_conditions": [
                        "症状加重",
                        "出现新症状",
                        "生命体征异常"
                    ]
                }
            elif risk_level == "L3":
                follow_up_plan = {
                    "review_time": "3天内",
                    "review_conditions": [
                        "症状加重",
                        "出现新症状"
                    ]
                }
            else:
                follow_up_plan = {
                    "review_time": "1周内",
                    "review_conditions": [
                        "症状加重"
                    ]
                }
        
        return {
            "immediate_actions": immediate_actions,
            "follow_up_plan": follow_up_plan
        }
    
    def build_conclusion_package(
            self,
            three_layer_result: Dict[str, Any],
            evidence_analysis: Dict[str, Any] = None,
            risk_level: Optional[str] = None) -> Dict[str, Any]:
        """
        构建终点结论包
        包含结论、必须排除项状态、关键依据、行动与随访
        
        Args:
            three_layer_result: 三层分层结果
            evidence_analysis: 证据分析结果（可选）
            risk_level: 风险等级（可选）
            
        Returns:
            终点结论包
        """
        logger.info("构建终点结论包")
        
        if evidence_analysis is None:
            evidence_analysis = {}
        
        # 1. 构建结论
        conclusion = self._build_conclusion(three_layer_result)
        
        # 2. 构建必须排除项状态
        must_exclude_status = self._build_must_exclude_status(three_layer_result)
        
        # 3. 提取关键依据
        key_evidence = self._extract_key_evidence(evidence_analysis)
        
        # 4. 构建行动与随访
        action_and_follow_up = self._build_action_and_follow_up(
            three_layer_result,
            risk_level
        )
        
        return {
            "conclusion": conclusion,
            "must_exclude_status": must_exclude_status,
            "key_evidence": key_evidence,
            "action_and_follow_up": action_and_follow_up
        }

