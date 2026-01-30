"""
请求模型
"""
from typing import Dict, Any, Optional, List
from pydantic import BaseModel


class RiskAssessmentRequest(BaseModel):
    """风险评估请求"""
    cdpId: Optional[str] = None  # CDP ID（可选）
    cdp: Optional[Dict[str, Any]] = None  # 临床决策包（可选）
    patient_state: Optional[Dict[str, Any]] = None  # 患者状态（可选）
    ddx: Optional[List[Dict[str, Any]]] = None  # 鉴别诊断列表（可选）
    
    class Config:
        # 允许从其他字段构建cdp（向后兼容）
        extra = "allow"


class TriageRequest(BaseModel):
    """分诊评估请求"""
    patient_state: Dict[str, Any]  # 患者状态
    ddx: List[Dict[str, Any]]  # 鉴别诊断列表


class UpgradeRulesRequest(BaseModel):
    """升级规则请求"""
    risk_level: str  # 风险等级
    patient_state: Dict[str, Any]  # 患者状态


class ConclusionPackageRequest(BaseModel):
    """终点结论包请求"""
    cdpId: str  # CDP ID
    three_layer_result: Dict[str, Any]  # 三层分层结果
    evidence_analysis: Optional[Dict[str, Any]] = None  # 证据分析结果（可选）
    risk_level: Optional[str] = None  # 风险等级（可选）

