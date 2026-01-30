"""
风险评估引擎（脑区F）
"""
from typing import Dict, Any, List
import logging
from app.services.upgrade_rule_engine import UpgradeRuleEngine

logger = logging.getLogger(__name__)


class RiskAssessmentEngine:
    """风险评估引擎"""
    
    def __init__(self):
        """初始化风险评估引擎"""
        self.upgrade_rule_engine = UpgradeRuleEngine()
        
        # 高危识别规则库（初期使用简单规则，后期可扩展为知识库）
        self.high_risk_rules = self._load_high_risk_rules()
        
        # 严重程度评估规则库
        self.severity_rules = self._load_severity_rules()
        
        # 紧急程度分级规则库
        self.urgency_rules = self._load_urgency_rules()
    
    def _load_high_risk_rules(self) -> Dict[str, List[str]]:
        """
        加载高危识别规则库
        格式: {risk_type: [indicators]}
        
        Returns:
            高危识别规则字典
        """
        # 示例规则：风险类型 -> 指标列表
        rules = {
            "symptom_combination": [
                "胸痛+气短+心率快",
                "胸痛+出汗+呼吸困难",
                "剧烈头痛+恶心+呕吐",
                "腹痛+发热+血便"
            ],
            "vital_signs_abnormal": [
                "血压>180/120",
                "心率>120或<50",
                "体温>39或<35",
                "血氧<90"
            ],
            "high_risk_disease": [
                "急性心肌梗死",
                "主动脉夹层",
                "肺栓塞",
                "脑出血",
                "急性阑尾炎"
            ]
        }
        return rules
    
    def _load_severity_rules(self) -> Dict[str, Dict[str, str]]:
        """
        加载严重程度评估规则库
        格式: {disease: {severity}}
        
        Returns:
            严重程度规则字典
        """
        # 示例规则：疾病 -> 严重程度
        rules = {
            "急性心肌梗死": "severe",
            "主动脉夹层": "critical",
            "肺栓塞": "severe",
            "脑出血": "critical",
            "不稳定心绞痛": "moderate",
            "肺炎": "moderate",
            "高血压": "mild"
        }
        return rules
    
    def _load_urgency_rules(self) -> Dict[str, str]:
        """
        加载紧急程度分级规则库
        格式: {severity: urgency}
        
        Returns:
            紧急程度规则字典
        """
        # 示例规则：严重程度 -> 紧急程度
        rules = {
            "critical": "emergency",
            "severe": "urgent",
            "moderate": "urgent",
            "mild": "routine"
        }
        return rules
    
    def _identify_high_risk(
        self,
        patient_state: Dict[str, Any],
        ddx: List[Dict[str, Any]]
    ) -> List[Dict[str, Any]]:
        """
        识别高危情况
        
        Args:
            patient_state: 患者状态
            ddx: 鉴别诊断列表
            
        Returns:
            危险信号列表
        """
        red_flags = []
        
        # 检查高危症状组合
        symptoms = patient_state.get("symptoms", [])
        symptom_str = "+".join(symptoms)
        
        high_risk_combinations = self.high_risk_rules.get("symptom_combination", [])
        for combination in high_risk_combinations:
            if all(symptom in symptom_str for symptom in combination.split("+")):
                red_flags.append({
                    "type": "symptom_combination",
                    "description": combination,
                    "risk": "high"
                })
        
        # 检查生命体征异常
        vital_signs = patient_state.get("vital_signs", {})
        bp = vital_signs.get("bp", {})
        heart_rate = vital_signs.get("heart_rate")
        temperature = vital_signs.get("temperature")
        oxygen_saturation = vital_signs.get("oxygen_saturation")
        
        if bp:
            systolic = bp.get("systolic", 0)
            diastolic = bp.get("diastolic", 0)
            if systolic > 180 or diastolic > 120:
                red_flags.append({
                    "type": "vital_signs_abnormal",
                    "description": f"血压异常: {systolic}/{diastolic}",
                    "risk": "high"
                })
        
        if heart_rate and (heart_rate > 120 or heart_rate < 50):
            red_flags.append({
                "type": "vital_signs_abnormal",
                "description": f"心率异常: {heart_rate}",
                "risk": "high"
            })
        
        # 检查高危诊断
        high_risk_diseases = self.high_risk_rules.get("high_risk_disease", [])
        for ddx_item in ddx:
            disease = ddx_item.get("disease", "")
            if disease in high_risk_diseases:
                probability = ddx_item.get("probability", 0.0)
                if probability > 0.3:  # 概率超过30%认为有风险
                    red_flags.append({
                        "type": "high_risk_disease",
                        "description": f"高危诊断: {disease} (概率: {probability:.2f})",
                        "risk": "high"
                    })
        
        return red_flags
    
    def _assess_severity(self, ddx: List[Dict[str, Any]]) -> str:
        """
        评估严重程度
        
        Args:
            ddx: 鉴别诊断列表
            
        Returns:
            严重程度（mild/moderate/severe/critical）
        """
        if not ddx:
            return "mild"
        
        # 获取主要诊断
        primary_diagnosis = None
        for ddx_item in ddx:
            layer = ddx_item.get("layer", "")
            if layer == "primary_hypothesis":
                primary_diagnosis = ddx_item
                break
        
        if not primary_diagnosis:
            # 如果没有primary_hypothesis，选择概率最高的
            sorted_ddx = sorted(ddx, key=lambda x: x.get("probability", 0.0), reverse=True)
            primary_diagnosis = sorted_ddx[0] if sorted_ddx else None
        
        if not primary_diagnosis:
            return "mild"
        
        disease = primary_diagnosis.get("disease", "")
        severity = self.severity_rules.get(disease, "moderate")
        
        return severity
    
    def _determine_urgency(self, severity: str, red_flags: List[Dict[str, Any]]) -> str:
        """
        确定紧急程度
        
        Args:
            severity: 严重程度
            red_flags: 危险信号列表
            
        Returns:
            紧急程度（emergency/urgent/routine）
        """
        # 如果有危险信号，提高紧急程度
        if red_flags:
            if severity == "critical":
                return "emergency"
            elif severity == "severe":
                return "urgent"
            else:
                return "urgent"
        
        # 根据严重程度确定紧急程度
        urgency = self.urgency_rules.get(severity, "routine")
        return urgency
    
    def _calculate_risk_level(
        self,
        severity: str,
        urgency: str,
        red_flags: List[Dict[str, Any]]
    ) -> str:
        """
        计算风险等级
        
        Args:
            severity: 严重程度
            urgency: 紧急程度
            red_flags: 危险信号列表
            
        Returns:
            风险等级（L1-L5，L1最紧急）
        """
        # 有危险信号且紧急程度为emergency -> L1
        if red_flags and urgency == "emergency":
            return "L1"
        
        # 有危险信号或紧急程度为urgent -> L2
        if red_flags or urgency == "urgent":
            return "L2"
        
        # 严重程度为moderate -> L3
        if severity == "moderate":
            return "L3"
        
        # 严重程度为mild -> L4
        if severity == "mild":
            return "L4"
        
        # 默认L3
        return "L3"
    
    def assess_risk(self, cdp: Dict[str, Any]) -> Dict[str, Any]:
        """
        风险评估
        识别高危情况，评估紧急程度，决定是否需要立即升级处理
        
        Args:
            cdp: 临床决策包
            
        Returns:
            风险评估结果
        """
        logger.info("进行风险评估")
        
        patient_state = cdp.get("patient_state", {})
        ddx = cdp.get("ddx", [])
        
        # 1. 高危识别
        red_flags = self._identify_high_risk(patient_state, ddx)
        
        # 2. 严重程度评估
        severity = self._assess_severity(ddx)
        
        # 3. 紧急程度分级
        urgency = self._determine_urgency(severity, red_flags)
        
        # 4. 风险等级计算
        risk_level = self._calculate_risk_level(severity, urgency, red_flags)
        
        # 5. 复评与升级规则
        review_plan = self.upgrade_rule_engine.get_upgrade_rules(
            risk_level=risk_level,
            patient_state=patient_state
        )
        
        return {
            "riskLevel": risk_level,
            "severity": severity,
            "urgency": urgency,
            "redFlags": red_flags,
            "reviewPlan": review_plan
        }

