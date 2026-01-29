"""
信息缺口识别器
按照《dialog-service - 服务实现方案.md》实现
"""
from typing import Dict, List, Any
import logging

# 使用dialog-service的logger，确保日志能正确输出
logger = logging.getLogger("dialog-service")


class InformationGapIdentifier:
    """信息缺口识别器"""
    
    def __init__(self):
        # 定义必填信息字段
        self.required_fields = [
            {
                "field": "chief_complaint",
                "description": "主诉",
                "reason": "主诉是诊断的基础信息"
            },
            {
                "field": "symptom_trigger",
                "description": "症状诱因",
                "reason": "用于鉴别心绞痛和心肌梗死"
            },
            {
                "field": "symptom_duration",
                "description": "症状持续时间",
                "reason": "用于判断疾病严重程度"
            }
        ]
        
        # 定义重要信息字段
        self.important_fields = [
            {
                "field": "symptom_severity",
                "description": "症状严重程度",
                "reason": "用于判断疾病严重程度"
            },
            {
                "field": "symptom_location",
                "description": "症状部位",
                "reason": "用于定位疾病位置"
            },
            {
                "field": "symptom_frequency",
                "description": "症状频率",
                "reason": "用于判断疾病类型"
            },
            {
                "field": "accompanying_symptoms",
                "description": "伴随症状",
                "reason": "用于辅助诊断"
            }
        ]
        
        # 定义可选信息字段
        self.optional_fields = [
            {
                "field": "family_history",
                "description": "家族史",
                "reason": "用于风险评估"
            },
            {
                "field": "past_history",
                "description": "既往史",
                "reason": "用于了解患者病史"
            },
            {
                "field": "medication_history",
                "description": "用药史",
                "reason": "用于了解用药情况"
            },
            {
                "field": "allergy_history",
                "description": "过敏史",
                "reason": "用于用药安全"
            }
        ]
    
    def identify_gaps(self, patient_state: Dict[str, Any], ddx: List[Dict] = None) -> Dict[str, List[Dict]]:
        """
        识别信息缺口并分级
        
        Args:
            patient_state: 患者状态（从CDP.patient_state读取）
            ddx: 诊断候选集（可选，用于动态调整信息需求）
            
        Returns:
            信息缺口分类字典
        """
        # 分析已收集信息（移除logger，使用print输出到控制台）
        collected_info = self._analyze_collected_info(patient_state)
        
        # 获取诊断所需信息清单（可根据ddx动态调整）
        required_info_list = self._get_required_info_list(ddx)
        
        # 识别缺失信息
        missing_info = self._identify_missing_info(collected_info, required_info_list)
        
        # 信息缺口分级
        classified_gaps = self._classify_information_gaps(missing_info, ddx)
        
        return classified_gaps
    
    def _analyze_collected_info(self, patient_state: Dict[str, Any]) -> Dict[str, Any]:
        """分析已收集信息"""
        collected = {}
        
        # 从patient_state中提取信息
        symptoms = patient_state.get("symptoms", [])
        signs = patient_state.get("signs", {})
        examination_results = patient_state.get("examination_results", [])
        health_profile = patient_state.get("health_profile", {})
        problem_list = patient_state.get("problem_list", {})
        
        # 检查主诉（优先从problem_list读取，如果没有则从symptoms中提取）
        if problem_list.get("chief_complaint"):
            # 如果problem_list中有主诉，直接使用
            chief_complaint = problem_list["chief_complaint"]
            if isinstance(chief_complaint, dict):
                collected["chief_complaint"] = chief_complaint.get("name") or chief_complaint
            else:
                collected["chief_complaint"] = chief_complaint
        elif symptoms and len(symptoms) > 0:
            # 如果problem_list中没有主诉，从symptoms数组中提取第一个症状作为主诉
            first_symptom = symptoms[0]
            if isinstance(first_symptom, dict) and first_symptom.get("name"):
                collected["chief_complaint"] = first_symptom["name"]
        
        # 检查症状信息
        if symptoms:
            collected["symptoms"] = symptoms
            # 从症状中提取详细信息
            for symptom in symptoms:
                if isinstance(symptom, dict):
                    if symptom.get("duration"):
                        collected["symptom_duration"] = symptom["duration"]
                    if symptom.get("severity"):
                        collected["symptom_severity"] = symptom["severity"]
                    if symptom.get("location"):
                        collected["symptom_location"] = symptom["location"]
                    if symptom.get("trigger"):
                        collected["symptom_trigger"] = symptom["trigger"]
                    if symptom.get("frequency"):
                        collected["symptom_frequency"] = symptom["frequency"]
        
        # 检查伴随症状
        if problem_list.get("accompanying_symptoms"):
            collected["accompanying_symptoms"] = problem_list["accompanying_symptoms"]
        
        # 检查健康档案
        if health_profile:
            if health_profile.get("family_history"):
                collected["family_history"] = health_profile["family_history"]
            if health_profile.get("past_history"):
                collected["past_history"] = health_profile["past_history"]
            if health_profile.get("medication_history"):
                collected["medication_history"] = health_profile["medication_history"]
            if health_profile.get("allergy_history"):
                collected["allergy_history"] = health_profile["allergy_history"]
        
        return collected
    
    def _get_required_info_list(self, ddx: List[Dict] = None) -> List[str]:
        """
        获取诊断所需信息清单
        可根据ddx动态调整
        """
        # 基础必填字段
        required_fields = [field["field"] for field in self.required_fields]
        
        # 如果提供了ddx，可以根据诊断候选集动态调整
        if ddx:
            # TODO: 根据ddx中的疾病类型，动态添加特定信息需求
            # 例如：如果是心血管疾病，需要更多心血管相关字段
            pass
        
        return required_fields
    
    def _identify_missing_info(self, collected_info: Dict[str, Any], required_info_list: List[str]) -> List[str]:
        """识别缺失信息"""
        missing = []
        
        # 检查必填字段
        for field in required_info_list:
            if field not in collected_info or not collected_info[field]:
                missing.append(field)
        
        return missing
    
    def _classify_information_gaps(
        self,
        missing_info: List[str],
        ddx: List[Dict] = None
    ) -> Dict[str, List[Dict]]:
        """信息缺口分级"""
        required_gaps = []
        important_gaps = []
        optional_gaps = []
        
        # 分类缺失信息
        for field in missing_info:
            # 检查是否在必填字段中
            required_field = next(
                (f for f in self.required_fields if f["field"] == field),
                None
            )
            if required_field:
                required_gaps.append(required_field)
                continue
            
            # 检查是否在重要字段中
            important_field = next(
                (f for f in self.important_fields if f["field"] == field),
                None
            )
            if important_field:
                important_gaps.append(important_field)
                continue
            
            # 检查是否在可选字段中
            optional_field = next(
                (f for f in self.optional_fields if f["field"] == field),
                None
            )
            if optional_field:
                optional_gaps.append(optional_field)
        
        return {
            "required": required_gaps,
            "important": important_gaps,
            "optional": optional_gaps
        }

