"""
结构化数据提取器
"""
import logging
from typing import List, Dict, Optional
from app.utils.text_processor import TextProcessor

logger = logging.getLogger(__name__)


class StructuredExtractor:
    """结构化数据提取器"""
    
    def __init__(self):
        """初始化提取器"""
        self.text_processor = TextProcessor()
    
    def extract_symptoms(self, text: str, concepts: List[Dict]) -> List[Dict]:
        """
        提取症状及其属性
        
        Args:
            text: 原始文本
            concepts: 识别到的概念列表
        
        Returns:
            症状列表
        """
        symptoms = []
        
        # 从概念中提取症状
        symptom_concepts = [c for c in concepts if c.get('concept_type') == 'symptom']
        
        for concept in symptom_concepts:
            symptom = {
                'name': concept.get('standard_term', ''),
                'cui': concept.get('cui', ''),
                'duration': self._extract_duration(text),
                'severity': self._extract_severity(text),
                'trigger': self._extract_trigger(text),
                'location': self._extract_location(text),
            }
            symptoms.append(symptom)
        
        return symptoms
    
    def extract_diseases(self, text: str, concepts: List[Dict]) -> List[Dict]:
        """
        提取疾病信息
        
        Args:
            text: 原始文本
            concepts: 识别到的概念列表
        
        Returns:
            疾病列表
        """
        diseases = []
        
        # 从概念中提取疾病
        disease_concepts = [c for c in concepts if c.get('concept_type') == 'disease']
        
        for concept in disease_concepts:
            # 判断是既往史还是当前疾病
            status = 'ongoing' if '有' in text or '患' in text else 'past'
            
            disease = {
                'disease': concept.get('standard_term', ''),
                'icd': concept.get('icd', ''),
                'status': status,
            }
            diseases.append(disease)
        
        return diseases
    
    def extract_medications(self, text: str, concepts: List[Dict]) -> List[Dict]:
        """
        提取药物信息
        
        Args:
            text: 原始文本
            concepts: 识别到的概念列表
        
        Returns:
            药物列表
        """
        medications = []
        
        # 从概念中提取药物
        medication_concepts = [c for c in concepts if c.get('concept_type') == 'medication']
        
        for concept in medication_concepts:
            # 判断是当前用药还是既往用药
            status = 'current' if '正在' in text or '服用' in text else 'past'
            
            medication = {
                'name': concept.get('standard_term', ''),
                'atc': concept.get('atc', ''),
                'status': status,
            }
            medications.append(medication)
        
        return medications
    
    def extract_examinations(self, text: str, concepts: List[Dict]) -> List[Dict]:
        """
        提取检查信息
        
        Args:
            text: 原始文本
            concepts: 识别到的概念列表
        
        Returns:
            检查列表
        """
        examinations = []
        
        # 从概念中提取检查
        examination_concepts = [c for c in concepts if c.get('concept_type') == 'examination']
        
        for concept in examination_concepts:
            examination = {
                'name': concept.get('standard_term', ''),
                'loinc': concept.get('loinc', ''),
                'result': None,  # 检查结果需要从其他地方提取
                'abnormal': False,  # 需要进一步判断
            }
            examinations.append(examination)
        
        return examinations
    
    def extract_allergies(self, text: str, concepts: List[Dict]) -> List[Dict]:
        """
        提取过敏信息
        
        Args:
            text: 原始文本
            concepts: 识别到的概念列表
        
        Returns:
            过敏列表
        """
        allergies = []
        
        # 从概念中提取过敏源
        allergy_concepts = [c for c in concepts if c.get('concept_type') == 'allergy']
        
        for concept in allergy_concepts:
            allergy = {
                'allergen': concept.get('standard_term', ''),
                'reaction': None,  # 过敏反应需要进一步提取
            }
            allergies.append(allergy)
        
        return allergies
    
    def _extract_duration(self, text: str) -> Optional[str]:
        """
        提取持续时间
        返回格式：如果是具体天数，返回"X天"；如果是相对时间，返回标准化时间
        """
        time_expressions = self.text_processor.extract_time_expressions(text)
        if time_expressions:
            expr, normalized, days = time_expressions[0]
            # 如果有具体天数，返回天数
            if days is not None:
                return f"{days}天"
            # 否则返回标准化时间
            return normalized
        return None
    
    def _extract_severity(self, text: str) -> Optional[str]:
        """提取严重度"""
        severity_expressions = self.text_processor.extract_severity_expressions(text)
        if severity_expressions:
            return severity_expressions[0][1]  # 返回标准化严重度
        return None
    
    def _extract_trigger(self, text: str) -> Optional[str]:
        """提取诱因"""
        triggers = self.text_processor.extract_trigger_expressions(text)
        if triggers:
            return triggers[0]
        return None
    
    def _extract_location(self, text: str) -> Optional[str]:
        """提取部位"""
        locations = self.text_processor.extract_location_expressions(text)
        if locations:
            return locations[0]  # 返回第一个匹配的部位
        return None

