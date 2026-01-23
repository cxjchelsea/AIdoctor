"""
解释生成响应模型
"""
from pydantic import BaseModel
from typing import List, Optional, Dict, Any

class Evidence(BaseModel):
    """证据"""
    item: str
    type: str
    strength: str
    supportingDiseases: List[str] = []
    contradictingDiseases: List[str] = []

class EvidenceChain(BaseModel):
    """证据链"""
    evidence: List[Evidence]
    reasoningPaths: List[str]

class ConclusionPackage(BaseModel):
    """终点结论包"""
    conclusion: Dict[str, Any]
    mustExcludeStatus: Dict[str, Any]
    keyEvidence: List[Dict[str, Any]]
    actionAndFollowUp: Dict[str, Any]

class ReasoningPath(BaseModel):
    """推理路径"""
    pathId: str
    description: str
    confidence: float
    nodes: List[Dict[str, Any]] = []
    edges: List[Dict[str, Any]] = []

class ExplanationResponse(BaseModel):
    """解释生成响应"""
    evidenceChain: EvidenceChain
    reasoningPaths: List[ReasoningPath]
    conclusionPackage: ConclusionPackage
    naturalLanguageExplanation: Optional[str] = None

