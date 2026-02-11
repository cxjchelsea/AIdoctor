"""
治疗推理引擎（tool_5）
"""
from typing import Dict, Any, List, Optional
import logging
from app.services.medication_recommender import MedicationRecommender

logger = logging.getLogger(__name__)


class TreatmentEngine:
    """治疗推理引擎"""
    
    def __init__(self):
        """初始化治疗推理引擎"""
        self.medication_recommender = MedicationRecommender()
        
        # 治疗方案推理规则库（初期使用简单规则，后期可扩展为知识库）
        self.treatment_rules = self._load_treatment_rules()
        
        # 非药物治疗建议库
        self.non_medication_advice_library = self._load_non_medication_advice()
    
    def _load_treatment_rules(self) -> Dict[str, Dict[str, Any]]:
        """
        加载治疗方案推理规则库
        格式: {disease: {treatment_plan}}
        
        Returns:
            治疗方案规则字典
        """
        # 示例规则：疾病 -> 治疗方案
        rules = {
            "急性心肌梗死": {
                "primaryTreatment": {
                    "name": "急性心肌梗死标准治疗",
                    "description": "包括抗血小板、抗凝、他汀类、ACEI/ARB等药物治疗，必要时介入治疗",
                    "priority": "high"
                },
                "alternativeTreatments": [
                    {
                        "name": "保守治疗",
                        "description": "适用于低风险患者",
                        "priority": "medium"
                    }
                ]
            },
            "不稳定心绞痛": {
                "primaryTreatment": {
                    "name": "不稳定心绞痛标准治疗",
                    "description": "包括抗血小板、抗凝、他汀类、β受体阻滞剂等药物治疗",
                    "priority": "high"
                },
                "alternativeTreatments": []
            },
            "肺炎": {
                "primaryTreatment": {
                    "name": "肺炎标准治疗",
                    "description": "包括抗感染、对症支持治疗",
                    "priority": "high"
                },
                "alternativeTreatments": []
            },
            "高血压": {
                "primaryTreatment": {
                    "name": "高血压标准治疗",
                    "description": "包括ACEI/ARB、利尿剂、钙通道阻滞剂等药物治疗，生活方式干预",
                    "priority": "high"
                },
                "alternativeTreatments": []
            }
        }
        return rules
    
    def _load_non_medication_advice(self) -> Dict[str, List[Dict[str, Any]]]:
        """
        加载非药物治疗建议库
        格式: {disease: [advice_items]}
        
        Returns:
            非药物治疗建议字典
        """
        # 示例建议：疾病 -> 非药物治疗建议列表
        advice = {
            "急性心肌梗死": [
                {
                    "type": "lifestyle",
                    "content": "低盐低脂饮食，戒烟限酒，控制体重",
                    "priority": "high"
                },
                {
                    "type": "rehabilitation",
                    "content": "心脏康复训练，逐步增加活动量",
                    "priority": "medium"
                }
            ],
            "不稳定心绞痛": [
                {
                    "type": "lifestyle",
                    "content": "低盐低脂饮食，戒烟限酒，避免剧烈运动",
                    "priority": "high"
                }
            ],
            "肺炎": [
                {
                    "type": "lifestyle",
                    "content": "充分休息，多饮水，保持室内通风",
                    "priority": "high"
                }
            ],
            "高血压": [
                {
                    "type": "lifestyle",
                    "content": "低盐饮食，适量运动，控制体重，戒烟限酒",
                    "priority": "high"
                }
            ]
        }
        return advice
    
    def _get_primary_diagnosis(self, ddx_list: List[Dict[str, Any]]) -> Optional[Dict[str, Any]]:
        """
        从诊断候选集中获取主要诊断（primary_hypothesis）
        
        Args:
            ddx_list: 鉴别诊断列表
            
        Returns:
            主要诊断，如果没有则返回None
        """
        for ddx_item in ddx_list:
            layer = ddx_item.get("layer", "")
            if layer == "primary_hypothesis":
                return ddx_item
        
        # 如果没有找到primary_hypothesis，返回概率最高的诊断
        if ddx_list:
            sorted_ddx = sorted(ddx_list, key=lambda x: x.get("probability", 0.0), reverse=True)
            return sorted_ddx[0]
        
        return None
    
    def _personalize_treatment(
        self,
        treatment_plan: Dict[str, Any],
        patient_state: Dict[str, Any],
        risk_level: Optional[str] = None
    ) -> Dict[str, Any]:
        """
        根据患者状态个性化调整治疗方案
        
        Args:
            treatment_plan: 基础治疗方案
            patient_state: 患者状态
            risk_level: 风险等级
            
        Returns:
            个性化治疗方案
        """
        # 根据风险等级调整优先级
        if risk_level:
            if risk_level in ["L1", "L2"]:
                treatment_plan["priority"] = "high"
            elif risk_level == "L3":
                treatment_plan["priority"] = "medium"
            else:
                treatment_plan["priority"] = "low"
        
        return treatment_plan
    
    def plan_treatment(self, cdp: Dict[str, Any]) -> Dict[str, Any]:
        """
        生成治疗建议
        基于诊断结果，生成治疗方案和处置建议，包括对症处理、用药建议、非药物治疗建议等
        
        Args:
            cdp: 临床决策包
            
        Returns:
            治疗建议结果
        """
        logger.info("生成治疗建议")
        
        ddx = cdp.get("ddx", [])
        patient_state = cdp.get("patient_state", {})
        risk_level = cdp.get("risk_level")
        
        if not ddx:
            logger.warning("DDx为空，无法生成治疗建议")
            return {
                "treatmentPlan": {},
                "medicationAdvice": [],
                "nonMedicationAdvice": []
            }
        
        # 1. 获取主要诊断
        primary_diagnosis = self._get_primary_diagnosis(ddx)
        if not primary_diagnosis:
            logger.warning("未找到主要诊断")
            return {
                "treatmentPlan": {},
                "medicationAdvice": [],
                "nonMedicationAdvice": []
            }
        
        disease = primary_diagnosis.get("disease", "")
        
        # 2. 从治疗方案规则库中获取治疗方案
        treatment_rule = self.treatment_rules.get(disease, {})
        
        if not treatment_rule:
            logger.warning(f"未找到{disease}的治疗方案规则，生成默认方案")
            treatment_plan = {
                "primaryTreatment": {
                    "name": f"{disease}标准治疗",
                    "description": "根据诊断结果制定个性化治疗方案",
                    "priority": "medium"
                },
                "alternativeTreatments": []
            }
        else:
            treatment_plan = {
                "primaryTreatment": treatment_rule.get("primaryTreatment", {}),
                "alternativeTreatments": treatment_rule.get("alternativeTreatments", [])
            }
        
        # 3. 个性化调整治疗方案
        if treatment_plan.get("primaryTreatment"):
            treatment_plan["primaryTreatment"] = self._personalize_treatment(
                treatment_plan["primaryTreatment"],
                patient_state,
                risk_level
            )
        
        # 4. 药物推荐
        medication_advice = self.medication_recommender.recommend(
            diagnosis=primary_diagnosis,
            patient_state=patient_state
        )
        
        # 5. 非药物治疗建议
        non_medication_advice = self.non_medication_advice_library.get(disease, [])
        
        return {
            "treatmentPlan": treatment_plan,
            "medicationAdvice": medication_advice,
            "nonMedicationAdvice": non_medication_advice
        }

