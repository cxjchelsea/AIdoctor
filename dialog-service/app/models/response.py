"""
响应模型
按照《dialog-service - 服务实现方案.md》定义
"""
from pydantic import BaseModel
from typing import List, Optional, Dict, Any


class MissingInfoItem(BaseModel):
    """缺失信息项"""
    field: str
    level: str  # required/important/optional
    description: str


class InformationGapItem(BaseModel):
    """信息缺口项"""
    field: str
    description: str
    reason: str


class InformationGaps(BaseModel):
    """信息缺口分类"""
    required: List[InformationGapItem]
    important: List[InformationGapItem]
    optional: List[InformationGapItem]


class ConversationMessage(BaseModel):
    """对话消息"""
    role: str  # user/assistant
    content: str


class ConversationContext(BaseModel):
    """对话上下文"""
    conversationHistory: List[ConversationMessage]


class QuestionResponse(BaseModel):
    """问题响应"""
    question: Optional[str]
    questionType: Optional[str]  # 使用questionType而不是question_type
    missingInfo: List[MissingInfoItem]  # 使用missingInfo而不是missing_info
    completeness: float
    informationGaps: InformationGaps  # 新增字段


class UnderstandingResponse(BaseModel):
    """理解响应"""
    extractedInfo: Dict[str, Any]  # 使用extractedInfo而不是extracted_info
    confidence: float
    updatedFields: List[str]  # 新增字段


class IdentifyGapsResponse(BaseModel):
    """识别信息缺口响应"""
    informationGaps: InformationGaps
    completeness: float

