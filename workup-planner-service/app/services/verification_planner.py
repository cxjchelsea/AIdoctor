"""
验证计划构建器
"""
from typing import Dict, List, Any
import logging

logger = logging.getLogger(__name__)


class VerificationPlanner:
    """验证计划构建器"""
    
    def __init__(self):
        """初始化验证计划构建器"""
        # 验证计划规则库（初期使用简单规则，后期可扩展为知识库）
        self.verification_rules = self._load_verification_rules()
    
    def _load_verification_rules(self) -> Dict[str, List[Dict[str, Any]]]:
        """
        加载验证计划规则库
        格式: {disease: [verification_items]}
        
        Returns:
            验证计划规则字典
        """
        # 示例规则：疾病 -> 验证检查项列表
        rules = {
            "主动脉夹层": [
                {
                    "testName": "胸部CT血管造影",
                    "testCode": "CTA_CHEST",
                    "priority": "high",
                    "reason": "排除主动脉夹层的金标准检查"
                },
                {
                    "testName": "胸部MRI",
                    "testCode": "MRI_CHEST",
                    "priority": "medium",
                    "reason": "辅助排除主动脉夹层"
                }
            ],
            "肺栓塞": [
                {
                    "testName": "CT肺动脉造影",
                    "testCode": "CTPA",
                    "priority": "high",
                    "reason": "确诊肺栓塞的金标准检查"
                },
                {
                    "testName": "D-二聚体",
                    "testCode": "D_DIMER",
                    "priority": "medium",
                    "reason": "筛查肺栓塞"
                }
            ],
            "急性心肌梗死": [
                {
                    "testName": "心电图",
                    "testCode": "ECG",
                    "priority": "high",
                    "reason": "确诊急性心肌梗死"
                },
                {
                    "testName": "心肌酶谱",
                    "testCode": "CARDIAC_ENZYMES",
                    "priority": "high",
                    "reason": "确诊急性心肌梗死"
                }
            ],
            "脑出血": [
                {
                    "testName": "头颅CT",
                    "testCode": "CT_HEAD",
                    "priority": "high",
                    "reason": "排除脑出血的金标准检查"
                }
            ],
            "阑尾炎": [
                {
                    "testName": "腹部CT",
                    "testCode": "CT_ABDOMEN",
                    "priority": "high",
                    "reason": "确诊阑尾炎"
                },
                {
                    "testName": "腹部超声",
                    "testCode": "US_ABDOMEN",
                    "priority": "medium",
                    "reason": "辅助诊断阑尾炎"
                }
            ]
        }
        return rules
    
    def build_verification_plan(
            self,
            must_exclude: Dict[str, Any],
            key_differentiating_points: List[str] = None) -> Dict[str, Any]:
        """
        构建验证计划
        针对必须排除的高危诊断，制定专门的验证计划
        
        Args:
            must_exclude: 必须排除的高危诊断（可以是字典或列表中的字典）
            key_differentiating_points: 关键鉴别点列表
            
        Returns:
            验证计划
        """
        logger.info("构建验证计划")
        
        if key_differentiating_points is None:
            key_differentiating_points = []
        
        # 处理must_exclude参数（可能是字典或列表）
        if isinstance(must_exclude, list) and must_exclude:
            must_exclude = must_exclude[0]
        elif not isinstance(must_exclude, dict):
            logger.warning(f"must_exclude格式不正确: {type(must_exclude)}")
            return {
                "targetDisease": None,
                "verificationItems": [],
                "priority": "high"
            }
        
        target_disease = must_exclude.get("disease", "")
        reason = must_exclude.get("reason", "高危诊断，必须排除")
        
        if not target_disease:
            logger.warning("目标诊断为空")
            return {
                "targetDisease": None,
                "verificationItems": [],
                "priority": "high"
            }
        
        # 从验证计划规则库中获取验证检查项
        verification_items = self.verification_rules.get(target_disease, [])
        
        # 如果没有找到规则，生成默认验证计划
        if not verification_items:
            logger.warning(f"未找到{target_disease}的验证计划规则，生成默认计划")
            verification_items = [
                {
                    "testName": "相关检查",
                    "testCode": "RELATED_TEST",
                    "priority": "high",
                    "reason": f"排除{target_disease}"
                }
            ]
        
        # 格式化验证检查项
        formatted_items = []
        for item in verification_items:
            formatted_item = {
                "testName": item.get("testName", ""),
                "testCode": item.get("testCode", ""),
                "priority": item.get("priority", "high"),
                "reason": item.get("reason", f"排除{target_disease}")
            }
            formatted_items.append(formatted_item)
        
        return {
            "targetDisease": target_disease,
            "verificationItems": formatted_items,
            "priority": "high",
            "reason": reason
        }

