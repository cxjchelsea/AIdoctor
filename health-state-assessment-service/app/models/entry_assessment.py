"""
入口判定数据模型
用于Step 2（识别症状/困扰）的数据结构
"""
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any


class SymptomInfo(BaseModel):
    """症状详细信息"""
    original_text: str = Field(..., description="原始文本")
    standard_term: str = Field(..., description="标准术语")
    cui: Optional[str] = Field(None, description="UMLS CUI")
    temporal_info: Dict[str, Any] = Field(default_factory=dict, description="时间信息（开始时间、持续时间、频率）")
    severity: Optional[str] = Field(None, description="严重程度（轻度/中度/重度）")
    location: Optional[str] = Field(None, description="位置")
    context: Dict[str, Any] = Field(default_factory=dict, description="其他上下文信息")


class ConcernInfo(BaseModel):
    """困扰信息"""
    type: str = Field(..., description="困扰类型（心理/功能变化/异常感觉等）")
    description: str = Field(..., description="困扰描述")
    severity: str = Field(..., description="严重程度（轻度/中度/重度）")
    confidence: float = Field(0.8, ge=0.0, le=1.0, description="识别置信度")


class SymptomIdentificationResult(BaseModel):
    """症状/困扰识别结果"""
    status: str = Field(..., description="状态：no_symptom/has_symptom/uncertain")
    symptoms: List[SymptomInfo] = Field(default_factory=list, description="症状详细信息")
    concerns: List[ConcernInfo] = Field(default_factory=list, description="困扰信息")
    confidence: float = Field(..., ge=0.0, le=1.0, description="识别置信度")
    reasoning: str = Field(..., description="识别理由")
    context_analysis: Dict[str, Any] = Field(default_factory=dict, description="上下文分析结果")


class SituationJudgmentResult(BaseModel):
    """情况判断结果"""
    status: str = Field(..., description="状态：no_symptom/has_symptom/uncertain")
    confidence: float = Field(..., ge=0.0, le=1.0, description="判断置信度")
    reasoning: str = Field(..., description="判断理由")
    symptoms: List[SymptomInfo] = Field(default_factory=list, description="症状列表")
    concerns: List[ConcernInfo] = Field(default_factory=list, description="困扰列表")
    context_analysis: Dict[str, Any] = Field(default_factory=dict, description="上下文分析结果")


# Step 3 澄清相关模型
class ClarificationOption(BaseModel):
    """澄清问题选项"""
    id: str = Field(..., description="选项ID（A或B）")
    text: str = Field(..., description="选项文本")
    description: Optional[str] = Field(None, description="选项描述")


class ClarificationQuestion(BaseModel):
    """澄清问题"""
    question: str = Field(..., description="问题文本")
    question_type: str = Field("single_choice", description="问题类型（single_choice/multiple_choice/open_ended）")
    options: List[ClarificationOption] = Field(default_factory=list, description="选项列表（如果是选择题）")
    reasoning: str = Field(..., description="生成理由")


class ClarificationResult(BaseModel):
    """澄清结果"""
    direction: str = Field(..., description="方向（A或B）")
    confidence: float = Field(..., ge=0.0, le=1.0, description="澄清置信度")
    reasoning: str = Field(..., description="判断理由")
    clarified: bool = Field(True, description="是否已澄清")
    user_answer: Optional[str] = Field(None, description="用户回答")
