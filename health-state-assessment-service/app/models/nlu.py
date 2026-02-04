"""
NLU数据模型
"""
from pydantic import BaseModel
from typing import List, Optional, Dict, Any


class IntentResult(BaseModel):
    """意图识别结果"""
    intent: str  # "screening" | "diagnosis" | "mixed" | "unknown"
    confidence: float  # 置信度 0-1
    reasoning: str  # 识别理由
    has_screening_intent: bool
    has_symptom_intent: bool
    details: Dict[str, Any] = {}  # 详细信息


class SymptomEntity(BaseModel):
    """症状实体"""
    original_text: str  # 原始文本
    standard_term: str  # 标准术语
    cui: Optional[str] = None  # UMLS CUI
    confidence: float  # 置信度
    context: Dict[str, Any] = {}  # 上下文信息（时间、程度等）
    location: Optional[str] = None  # 位置
    severity: Optional[str] = None  # 严重程度：轻度/中度/重度
    duration: Optional[str] = None  # 持续时间
    frequency: Optional[str] = None  # 频率


class EntityResult(BaseModel):
    """实体提取结果"""
    symptoms: List[SymptomEntity] = []  # 症状实体列表
    basic_info: Dict[str, Any] = {}  # 基本信息
    vital_signs: Dict[str, Any] = {}  # 生命体征
    temporal_info: Dict[str, Any] = {}  # 时间信息
    severity_info: Dict[str, Any] = {}  # 程度信息
    entities: List[Dict] = []  # 所有实体（兼容现有格式）

