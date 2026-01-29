"""
完整度计算器
按照《dialog-service - 服务实现方案.md》实现
"""
from typing import Dict, Any
import logging

# 使用dialog-service的logger，确保日志能正确输出
logger = logging.getLogger("dialog-service")


class CompletenessCalculator:
    """完整度计算器"""
    
    def __init__(self):
        # 定义信息项及其权重
        self.info_items = {
            # 必填项（权重高）
            "chief_complaint": 3.0,
            "symptom_trigger": 3.0,
            "symptom_duration": 3.0,
            # 重要项（权重中）
            "symptom_severity": 2.0,
            "symptom_location": 2.0,
            "symptom_frequency": 2.0,
            "accompanying_symptoms": 2.0,
            # 可选项（权重低）
            "family_history": 1.0,
            "past_history": 1.0,
            "medication_history": 1.0,
            "allergy_history": 1.0,
        }
        
        # 计算总权重
        self.total_weight = sum(self.info_items.values())
    
    def calculate_completeness(self, patient_state: Dict[str, Any]) -> float:
        """
        计算信息完整度（基于患者状态）
        
        Args:
            patient_state: 患者状态（从CDP.patient_state读取）
            
        Returns:
            信息完整度（0-1）
        """
        collected_weight = 0.0
        
        # 分析已收集信息
        symptoms = patient_state.get("symptoms", [])
        signs = patient_state.get("signs", {})
        examination_results = patient_state.get("examination_results", [])
        health_profile = patient_state.get("health_profile", {})
        problem_list = patient_state.get("problem_list", {})
        
        # 调试信息：输出到控制台（不写入日志文件）
        print(f"[DEBUG] 计算完整度 - symptoms数量: {len(symptoms) if symptoms else 0}, problem_list存在: {bool(problem_list)}")
        print(f"[DEBUG] patient_state的keys: {list(patient_state.keys())}")
        if symptoms:
            print(f"[DEBUG] 第一个症状: {symptoms[0] if len(symptoms) > 0 else None}")
        else:
            # 如果没有symptoms，打印patient_state的完整内容用于调试
            print(f"[DEBUG] patient_state内容: {patient_state}")
        
        # 检查主诉（优先从problem_list读取，如果没有则从symptoms中提取）
        chief_complaint_found = False
        if problem_list.get("chief_complaint"):
            chief_complaint = problem_list["chief_complaint"]
            # 如果problem_list中有主诉（无论是字符串还是对象），都认为已收集
            if isinstance(chief_complaint, dict):
                if chief_complaint.get("name"):
                    chief_complaint_found = True
            elif chief_complaint:
                chief_complaint_found = True
        
        if not chief_complaint_found and symptoms and len(symptoms) > 0:
            # 如果problem_list中没有主诉，从symptoms数组中提取第一个症状作为主诉
            first_symptom = symptoms[0]
            if isinstance(first_symptom, dict) and first_symptom.get("name"):
                chief_complaint_found = True
        
        if chief_complaint_found:
            collected_weight += self.info_items.get("chief_complaint", 0)
        
        # 检查症状信息
        if symptoms:
            for symptom in symptoms:
                if isinstance(symptom, dict):
                    if symptom.get("trigger"):
                        collected_weight += self.info_items.get("symptom_trigger", 0)
                    if symptom.get("duration"):
                        collected_weight += self.info_items.get("symptom_duration", 0)
                    if symptom.get("severity"):
                        collected_weight += self.info_items.get("symptom_severity", 0)
                    if symptom.get("location"):
                        collected_weight += self.info_items.get("symptom_location", 0)
                    if symptom.get("frequency"):
                        collected_weight += self.info_items.get("symptom_frequency", 0)
        
        # 检查伴随症状
        if problem_list.get("accompanying_symptoms"):
            collected_weight += self.info_items.get("accompanying_symptoms", 0)
        
        # 检查健康档案
        if health_profile:
            if health_profile.get("family_history"):
                collected_weight += self.info_items.get("family_history", 0)
            if health_profile.get("past_history"):
                collected_weight += self.info_items.get("past_history", 0)
            if health_profile.get("medication_history"):
                collected_weight += self.info_items.get("medication_history", 0)
            if health_profile.get("allergy_history"):
                collected_weight += self.info_items.get("allergy_history", 0)
        
        # 计算完整度
        completeness = collected_weight / self.total_weight if self.total_weight > 0 else 0.0
        completeness = min(1.0, max(0.0, completeness))  # 限制在0-1之间
        
        # 调试信息：输出到控制台（不写入日志文件）
        print(f"[DEBUG] 信息完整度计算: collected_weight={collected_weight}, total_weight={self.total_weight}, completeness={completeness:.2f}")
        
        return completeness

