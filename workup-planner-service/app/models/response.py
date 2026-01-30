"""
响应模型
"""
from typing import Dict, Any, List, Optional
from pydantic import BaseModel


class WorkupItem(BaseModel):
    """检查项"""
    testName: str  # 检查名称
    testCode: str  # 检查编码
    priority: str  # 优先级（high/medium/low）
    informationGain: float  # 信息增益（0-1）
    purpose: str  # 检查目的
    urgency: str  # 紧急程度（emergency/urgent/routine）
    reason: Optional[str] = None  # 推荐理由


class VerificationItem(BaseModel):
    """验证检查项"""
    testName: str  # 检查名称
    testCode: str  # 检查编码
    priority: str  # 优先级
    reason: str  # 推荐理由


class VerificationPlan(BaseModel):
    """验证计划"""
    targetDisease: Optional[str] = None  # 目标诊断
    verificationItems: List[VerificationItem] = []  # 验证检查项
    priority: str  # 优先级
    reason: Optional[str] = None  # 原因


class WorkupPlanResponse(BaseModel):
    """检查建议响应"""
    cdpId: Optional[str] = None  # CDP ID
    workupItems: List[WorkupItem] = []  # 检查建议列表
    verificationPlan: Optional[VerificationPlan] = None  # 验证计划
    priority: str = "normal"  # 整体优先级

