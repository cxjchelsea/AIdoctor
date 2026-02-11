"""
工具调用上下文模型（ToolContext）
参考文档：《7.接口规范/工具调用协议.md》
"""
from pydantic import BaseModel
from typing import List, Dict, Any, Optional


class CdpReference(BaseModel):
    """CDP引用"""
    cdp_id: str
    version: int
    read_fields: List[str]


class AgentStateSummary(BaseModel):
    """AgentState摘要"""
    current_step: int
    work_mode: str  # wellness_mode/clinical_mode


class Constraints(BaseModel):
    """约束（成本/时间/风险）"""
    max_time_seconds: int
    max_cost: float
    risk_level_limit: Optional[str] = None  # L1/L2/L3/L4


class ToolContext(BaseModel):
    """工具调用上下文"""
    trace_id: str
    cdp_reference: CdpReference
    agent_state_summary: AgentStateSummary
    constraints: Constraints
    call_params: Dict[str, Any] = {}

