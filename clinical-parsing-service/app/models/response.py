"""
病例理解响应模型
"""
from pydantic import BaseModel
from typing import List, Optional, Dict, Any

class NormalizedConcept(BaseModel):
    """归一化概念"""
    originalText: str
    normalizedSymptom: str
    cui: Optional[str] = None
    icd: Optional[str] = None
    snomed: Optional[str] = None
    loinc: Optional[str] = None
    atc: Optional[str] = None
    confidence: float
    conceptType: str  # symptom/disease/medication/examination/allergy/indicator

class Symptom(BaseModel):
    """症状"""
    name: str
    cui: Optional[str] = None
    duration: Optional[str] = None
    severity: Optional[str] = None
    trigger: Optional[str] = None

class Sign(BaseModel):
    """体征"""
    name: str
    value: Optional[str] = None
    unit: Optional[str] = None

class Examination(BaseModel):
    """检查"""
    name: str
    loinc: Optional[str] = None
    result: Optional[str] = None
    abnormal: bool = False

class MedicalHistory(BaseModel):
    """既往史"""
    disease: str
    icd: Optional[str] = None
    status: str  # past/ongoing

class Medication(BaseModel):
    """药物"""
    name: str
    atc: Optional[str] = None
    status: str  # current/past

class Allergy(BaseModel):
    """过敏"""
    allergen: str
    reaction: Optional[str] = None

class StructuredData(BaseModel):
    """结构化数据"""
    symptoms: List[Symptom] = []
    signs: List[Sign] = []
    examinations: List[Examination] = []
    medicalHistory: List[MedicalHistory] = []
    medications: List[Medication] = []
    allergies: List[Allergy] = []

class AmbiguousExpression(BaseModel):
    """歧义表达"""
    text: str
    type: str
    suggestedQuestions: List[str]

class ClinicalParsingResponse(BaseModel):
    """病例理解响应"""
    concepts: List[NormalizedConcept] = []
    structuredData: StructuredData
    ambiguousExpressions: Optional[List[AmbiguousExpression]] = None

