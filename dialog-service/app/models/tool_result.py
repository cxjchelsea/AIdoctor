"""
工具返回结果模型（ToolResult）
参考文档：《7.接口规范/工具调用协议.md》
"""
from pydantic import BaseModel
from typing import List, Dict, Any, Optional


class Evidence(BaseModel):
    """证据引用"""
    source: str  # knowledge_base/kg_path/rule/llm
    reference: str
    strength: str  # strong/medium/weak
    affected_direction: Optional[str] = None
    evidence_direction: Optional[str] = None
    evidence_name: Optional[str] = None


class Quality(BaseModel):
    """质量指标"""
    confidence: float  # 0.0-1.0
    completeness: float  # 0.0-1.0
    accuracy: Optional[float] = None  # 0.0-1.0


class SuggestedWrite(BaseModel):
    """建议写回CDP的字段"""
    field_path: str  # 如：cdp.patient_state
    value: Any
    reason: str


class ErrorInfo(BaseModel):
    """错误信息"""
    error_type: str  # timeout/validation_error/runtime_error
    error_message: str
    error_details: Dict[str, Any] = {}


class ToolResult(BaseModel):
    """工具返回结果"""
    trace_id: str
    tool_id: str
    status: str  # success/partial_success/failure/timeout
    payload: Dict[str, Any]
    evidence: List[Evidence]
    quality: Quality
    suggested_writes: List[SuggestedWrite]
    errors: List[ErrorInfo]
    duration_ms: int
    metadata: Dict[str, Any] = {}

