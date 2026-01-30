"""
升级规则引擎
"""
from typing import Dict, Any, List
import logging

logger = logging.getLogger(__name__)


class UpgradeRuleEngine:
    """升级规则引擎"""
    
    def __init__(self):
        """初始化升级规则引擎"""
        # 复评与升级规则库（初期使用简单规则，后期可扩展为知识库）
        self.review_rules = self._load_review_rules()
    
    def _load_review_rules(self) -> Dict[str, Dict[str, Any]]:
        """
        加载复评与升级规则库
        格式: {risk_level: {review_time_window, early_review_conditions, upgrade_conditions}}
        
        Returns:
            复评规则字典
        """
        # 示例规则：风险等级 -> 复评规则
        rules = {
            "L1": {
                "review_time_window": "立即",
                "early_review_conditions": [
                    "症状加重",
                    "出现新症状",
                    "生命体征恶化"
                ],
                "upgrade_conditions": [
                    "生命体征持续恶化",
                    "出现新的危险信号",
                    "症状急剧加重"
                ]
            },
            "L2": {
                "review_time_window": "24小时内",
                "early_review_conditions": [
                    "症状加重",
                    "出现新症状",
                    "生命体征异常"
                ],
                "upgrade_conditions": [
                    "生命体征恶化",
                    "出现危险信号",
                    "症状持续加重"
                ]
            },
            "L3": {
                "review_time_window": "3天内",
                "early_review_conditions": [
                    "症状加重",
                    "出现新症状"
                ],
                "upgrade_conditions": [
                    "症状明显加重",
                    "出现危险信号"
                ]
            },
            "L4": {
                "review_time_window": "1周内",
                "early_review_conditions": [
                    "症状加重"
                ],
                "upgrade_conditions": [
                    "症状持续加重"
                ]
            },
            "L5": {
                "review_time_window": "2周内",
                "early_review_conditions": [],
                "upgrade_conditions": [
                    "症状加重"
                ]
            }
        }
        return rules
    
    def get_upgrade_rules(
            self,
            risk_level: str,
            patient_state: Dict[str, Any]) -> Dict[str, Any]:
        """
        获取升级规则
        根据风险等级和患者状态，确定复评与升级规则
        
        Args:
            risk_level: 风险等级（L1-L5）
            patient_state: 患者状态
            
        Returns:
            升级规则
        """
        logger.info(f"获取升级规则: {risk_level}")
        
        # 从规则库中获取对应风险等级的规则
        rule = self.review_rules.get(risk_level, self.review_rules.get("L3", {}))
        
        return {
            "reviewTimeWindow": rule.get("review_time_window", "3天内"),
            "earlyReviewConditions": rule.get("early_review_conditions", []),
            "upgradeConditions": rule.get("upgrade_conditions", [])
        }

