"""
检查建议引擎（脑区D）
"""
from typing import Dict, Any, List
import logging
from app.calculators.value_assessor import ValueAssessor
from app.calculators.information_gain_calculator import InformationGainCalculator
from app.services.verification_planner import VerificationPlanner

logger = logging.getLogger(__name__)


class WorkupPlanner:
    """检查建议引擎"""
    
    def __init__(self):
        """初始化检查建议引擎"""
        self.value_assessor = ValueAssessor()
        self.information_gain_calculator = InformationGainCalculator()
        self.verification_planner = VerificationPlanner()
        
        # 检查项目库（初期使用简单列表，后期可扩展为知识库）
        self.workup_items_library = self._load_workup_items_library()
        
    def _load_workup_items_library(self) -> List[Dict[str, Any]]:
        """
        加载检查项目库
        初期使用简单列表，后期可扩展为从数据库或知识库加载
        
        Returns:
            检查项目列表
        """
        # 示例检查项目库
        items = [
            {
                "testName": "心电图",
                "testCode": "ECG",
                "category": "cardiac",
                "urgency": "urgent"
            },
            {
                "testName": "心肌酶谱",
                "testCode": "CARDIAC_ENZYMES",
                "category": "cardiac",
                "urgency": "urgent"
            },
            {
                "testName": "胸部CT",
                "testCode": "CT_CHEST",
                "category": "imaging",
                "urgency": "urgent"
            },
            {
                "testName": "胸部X光",
                "testCode": "XRAY_CHEST",
                "category": "imaging",
                "urgency": "routine"
            },
            {
                "testName": "血常规",
                "testCode": "BLOOD_ROUTINE",
                "category": "laboratory",
                "urgency": "routine"
            },
            {
                "testName": "尿常规",
                "testCode": "URINE_ROUTINE",
                "category": "laboratory",
                "urgency": "routine"
            }
        ]
        return items
    
    def _get_relevant_tests(self, ddx_list: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        根据诊断候选集获取相关检查项
        
        Args:
            ddx_list: 鉴别诊断列表
            
        Returns:
            相关检查项列表
        """
        relevant_tests = []
        
        # 获取所有诊断名称
        diseases = [ddx_item.get("disease", "") for ddx_item in ddx_list]
        
        # 从检查项目库中筛选相关检查
        for test_item in self.workup_items_library:
            test_name = test_item.get("testName", "")
            test_rules = self.value_assessor.test_disease_rules.get(test_name, {})
            
            # 检查是否有诊断与该检查相关
            for disease in diseases:
                if disease in test_rules and test_rules[disease] > 0.3:
                    relevant_tests.append(test_item)
                    break
        
        return relevant_tests
    
    def _calculate_comprehensive_score(
        self, 
        test_item: Dict[str, Any],
        value_score: float,
        information_gain: float,
        ddx_list: List[Dict[str, Any]]
    ) -> float:
        """
        计算检查的综合评分
        综合评分 = 检查价值评分 * 权重1 + 信息增益 * 权重2 + 紧急程度评分 * 权重3
        
        Args:
            test_item: 检查项
            value_score: 检查价值评分
            information_gain: 信息增益
            ddx_list: 鉴别诊断列表
            
        Returns:
            综合评分（0-1）
        """
        # 权重配置
        weight_value = 0.4
        weight_gain = 0.4
        weight_urgency = 0.2
        
        # 紧急程度评分
        urgency = test_item.get("urgency", "routine")
        urgency_scores = {
            "emergency": 1.0,
            "urgent": 0.8,
            "routine": 0.5
        }
        urgency_score = urgency_scores.get(urgency, 0.5)
        
        # 综合评分
        comprehensive_score = (
            value_score * weight_value +
            information_gain * weight_gain +
            urgency_score * weight_urgency
        )
        
        return min(max(comprehensive_score, 0.0), 1.0)
    
    def _determine_priority(self, comprehensive_score: float) -> str:
        """
        根据综合评分确定优先级
        
        Args:
            comprehensive_score: 综合评分
            
        Returns:
            优先级（high/medium/low）
        """
        if comprehensive_score >= 0.7:
            return "high"
        elif comprehensive_score >= 0.4:
            return "medium"
        else:
            return "low"
    
    def plan_workup(self, cdp: Dict[str, Any]) -> Dict[str, Any]:
        """
        生成检查建议
        基于当前DDx和已有证据，建议下一步检查，并评估检查的价值，制定验证计划
        
        Args:
            cdp: 临床决策包
            
        Returns:
            检查建议结果
        """
        logger.info("生成检查建议")
        
        ddx = cdp.get("ddx", [])
        patient_state = cdp.get("patient_state", {})
        must_exclude = cdp.get("must_exclude", [])
        
        if not ddx:
            logger.warning("DDx为空，无法生成检查建议")
            return {
                "workupItems": [],
                "verificationPlan": {},
                "priority": "normal"
            }
        
        # 1. 获取相关检查项
        relevant_tests = self._get_relevant_tests(ddx)
        
        if not relevant_tests:
            logger.warning("未找到相关检查项")
            return {
                "workupItems": [],
                "verificationPlan": {},
                "priority": "normal"
            }
        
        # 2. 评估每个检查的价值和信息增益
        workup_items = []
        for test_item in relevant_tests:
            test_name = test_item.get("testName", "")
            
            # 检查价值评估
            value_score = self.value_assessor.assess_value(test_name, ddx)
            
            # 信息增益计算
            information_gain = self.information_gain_calculator.calculate_gain(test_name, ddx)
            
            # 区分能力评估
            differentiation_value = self.value_assessor.assess_differentiation_value(test_name, ddx)
            
            # 综合评分
            comprehensive_score = self._calculate_comprehensive_score(
                test_item, value_score, information_gain, ddx
            )
            
            # 确定优先级
            priority = self._determine_priority(comprehensive_score)
            
            # 生成检查目的（区分哪些诊断）
            purpose = self._generate_purpose(test_name, ddx)
            
            # 构建检查项
            workup_item = {
                "testName": test_name,
                "testCode": test_item.get("testCode", ""),
                "priority": priority,
                "informationGain": round(information_gain, 3),
                "purpose": purpose,
                "urgency": test_item.get("urgency", "routine"),
                "reason": f"价值评分: {round(value_score, 2)}, 信息增益: {round(information_gain, 2)}"
            }
            
            workup_items.append(workup_item)
        
        # 3. 按综合评分排序（降序）
        workup_items.sort(key=lambda x: (
            x.get("priority") == "high",
            x.get("informationGain", 0)
        ), reverse=True)
        
        # 4. 生成验证计划（如果有必须排除的高危诊断）
        verification_plan = {}
        if must_exclude:
            # 取第一个必须排除的诊断
            first_must_exclude = must_exclude[0] if isinstance(must_exclude, list) else must_exclude
            key_differentiating_points = []
            
            verification_plan = self.verification_planner.build_verification_plan(
                must_exclude=first_must_exclude,
                key_differentiating_points=key_differentiating_points
            )
        
        # 5. 确定整体优先级
        overall_priority = "normal"
        if workup_items:
            highest_priority = workup_items[0].get("priority", "normal")
            if highest_priority == "high":
                overall_priority = "high"
            elif highest_priority == "medium":
                overall_priority = "medium"
        
        return {
            "workupItems": workup_items,
            "verificationPlan": verification_plan,
            "priority": overall_priority
        }
    
    def _generate_purpose(self, test_name: str, ddx_list: List[Dict[str, Any]]) -> str:
        """
        生成检查目的描述
        
        Args:
            test_name: 检查名称
            ddx_list: 鉴别诊断列表
            
        Returns:
            检查目的描述
        """
        test_rules = self.value_assessor.test_disease_rules.get(test_name, {})
        
        # 找出与该检查相关的诊断
        relevant_diseases = []
        for ddx_item in ddx_list:
            disease = ddx_item.get("disease", "")
            if disease in test_rules and test_rules[disease] > 0.5:
                relevant_diseases.append(disease)
        
        if relevant_diseases:
            if len(relevant_diseases) == 1:
                return f"确诊或排除{relevant_diseases[0]}"
            else:
                return f"区分{', '.join(relevant_diseases[:3])}"
        else:
            return "辅助诊断"

