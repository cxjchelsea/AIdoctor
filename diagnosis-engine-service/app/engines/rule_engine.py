"""
专科规则引擎
"""
from typing import Dict, List
import logging
from app.models.request import DiagnosisEngineRequest

logger = logging.getLogger(__name__)


class RuleEngine:
    """专科规则引擎"""
    
    def __init__(self):
        self.rules = self._load_rules()
    
    def _load_rules(self) -> List[Dict]:
        """加载规则库"""
        # TODO: 从文件或数据库加载规则
        # 规则格式：{symptoms: [...], diseases: {...}, confidence: 0.8}
        logger.info("加载规则库")
        return []
    
    async def diagnose(self, request: DiagnosisEngineRequest) -> Dict:
        """规则引擎诊断"""
        symptoms = request.symptom_info.get('symptoms', []) if request.symptom_info else []
        vital_signs = request.vital_signs or {}
        
        matches = []
        for rule in self.rules:
            score = self._calculate_match_score(rule, symptoms, vital_signs)
            if score > 0.7:  # 匹配阈值
                matches.append({
                    'disease': rule['disease'],
                    'confidence': score,
                    'supporting_symptoms': rule.get('symptoms', [])
                })
        
        return {
            'possibilities': {m['disease']: m['confidence'] for m in matches},
            'engine_type': 'rule'
        }
    
    def _calculate_match_score(self, rule: Dict, symptoms: List, vital_signs: Dict) -> float:
        """计算匹配分数"""
        rule_symptoms = set(rule.get('symptoms', []))
        user_symptoms = set(symptoms)
        
        # 完全匹配
        if rule_symptoms.issubset(user_symptoms):
            return 1.0
        
        # 部分匹配
        intersection = rule_symptoms.intersection(user_symptoms)
        if len(rule_symptoms) > 0 and len(intersection) / len(rule_symptoms) >= 0.7:
            return len(intersection) / len(rule_symptoms)
        
        return 0.0

