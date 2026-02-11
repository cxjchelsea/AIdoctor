"""
上下文分析器
分析症状的上下文信息（时间、程度、频率等）
"""
from typing import Dict, Any, List
from app.models.nlu import SymptomEntity
from app.utils.logger import logger


class ContextAnalyzer:
    """上下文分析器"""
    
    def analyze(
        self,
        nlu_result: Dict[str, Any],
        symptoms: List[SymptomEntity]
    ) -> Dict[str, Any]:
        """
        分析上下文信息
        
        Args:
            nlu_result: Step 1的NLU结果
            symptoms: 症状列表
        
        Returns:
            Dict: 上下文分析结果
        """
        context_analysis = {
            "temporal_info": {},
            "severity_info": {},
            "location_info": {},
            "frequency_info": {},
            "completeness": {}
        }
        
        # 分析时间信息
        temporal_info = nlu_result.get("temporal_info", {})
        if temporal_info:
            context_analysis["temporal_info"] = {
                "start_time": temporal_info.get("start_time"),
                "duration": temporal_info.get("duration"),
                "frequency": temporal_info.get("frequency"),
                "has_temporal_info": bool(temporal_info)
            }
        
        # 分析症状的严重程度
        severity_list = []
        for symptom in symptoms:
            if symptom.severity:
                severity_list.append(symptom.severity)
        
        if severity_list:
            context_analysis["severity_info"] = {
                "severities": severity_list,
                "max_severity": self._get_max_severity(severity_list),
                "has_severity_info": True
            }
        else:
            context_analysis["severity_info"] = {
                "has_severity_info": False
            }
        
        # 分析位置信息
        location_list = []
        for symptom in symptoms:
            if symptom.location:
                location_list.append(symptom.location)
        
        if location_list:
            context_analysis["location_info"] = {
                "locations": location_list,
                "has_location_info": True
            }
        else:
            context_analysis["location_info"] = {
                "has_location_info": False
            }
        
        # 分析频率信息
        frequency_list = []
        for symptom in symptoms:
            if symptom.frequency:
                frequency_list.append(symptom.frequency)
        
        if frequency_list:
            context_analysis["frequency_info"] = {
                "frequencies": frequency_list,
                "has_frequency_info": True
            }
        else:
            context_analysis["frequency_info"] = {
                "has_frequency_info": False
            }
        
        # 分析信息完整性
        context_analysis["completeness"] = {
            "has_temporal": bool(temporal_info),
            "has_severity": len(severity_list) > 0,
            "has_location": len(location_list) > 0,
            "has_frequency": len(frequency_list) > 0,
            "completeness_score": self._calculate_completeness_score(
                temporal_info, severity_list, location_list, frequency_list
            )
        }
        
        return context_analysis
    
    def _get_max_severity(self, severities: List[str]) -> str:
        """获取最大严重程度"""
        severity_map = {"轻度": 1, "中度": 2, "重度": 3}
        max_severity = "轻度"
        max_value = 0
        
        for severity in severities:
            value = severity_map.get(severity, 0)
            if value > max_value:
                max_value = value
                max_severity = severity
        
        return max_severity
    
    def _calculate_completeness_score(
        self,
        temporal_info: Dict[str, Any],
        severity_list: List[str],
        location_list: List[str],
        frequency_list: List[str]
    ) -> float:
        """
        计算信息完整性得分
        
        Returns:
            float: 0-1之间的得分
        """
        score = 0.0
        total_items = 4
        
        if temporal_info:
            score += 0.25
        if severity_list:
            score += 0.25
        if location_list:
            score += 0.25
        if frequency_list:
            score += 0.25
        
        return score

