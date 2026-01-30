"""
请求模型
"""
from typing import Dict, Any, Optional
from pydantic import BaseModel


class WorkupPlanRequest(BaseModel):
    """检查建议请求"""
    cdpId: Optional[str] = None  # CDP ID（可选）
    cdp: Optional[Dict[str, Any]] = None  # 临床决策包（可选）
    ddx: list = []  # 鉴别诊断列表（可选，如果提供cdp则从cdp中读取）
    patient_state: Dict[str, Any] = {}  # 患者状态（可选）

