"""
响应模型
"""
from typing import Dict, Any, List, Optional
from pydantic import BaseModel


class RedFlag(BaseModel):
    """危险信号"""
    type: str  # 类型
    description: str  # 描述
    risk: str  # 风险等级


class ReviewPlan(BaseModel):
    """复评计划"""
    reviewTimeWindow: str  # 复评时间窗口
    earlyReviewConditions: List[str]  # 提前复评条件
    upgradeConditions: List[str]  # 升级条件


class RiskAssessmentResponse(BaseModel):
    """风险评估响应"""
    cdpId: Optional[str] = None  # CDP ID
    riskLevel: str  # 风险等级（L1-L5，L1最紧急）
    severity: str  # 严重程度（mild/moderate/severe/critical）
    urgency: str  # 紧急程度（emergency/urgent/routine）
    redFlags: List[RedFlag] = []  # 危险信号列表
    reviewPlan: ReviewPlan  # 复评计划


class TriageResponse(BaseModel):
    """分诊评估响应"""
    risk_level: str  # 风险等级
    urgency: str  # 紧急程度
    triage_level: str  # 分诊级别
    red_flags: List[RedFlag] = []  # 危险信号列表
    risk_factors: List[str] = []  # 风险因素列表


class UpgradeRulesResponse(BaseModel):
    """升级规则响应"""
    upgrade_conditions: List[str]  # 升级条件
    review_time_window: str  # 复评时间窗口
    early_review_conditions: List[str]  # 提前复评条件


class ConclusionPackageResponse(BaseModel):
    """终点结论包响应"""
    conclusion: Dict[str, Any]  # 结论
    must_exclude_status: Dict[str, Any]  # 必须排除项状态
    key_evidence: List[Dict[str, Any]]  # 关键依据
    action_and_follow_up: Dict[str, Any]  # 行动与随访

