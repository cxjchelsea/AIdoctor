"""
解释生成请求模型
"""
from pydantic import BaseModel
from typing import Optional, Dict, Any

class ExplanationRequest(BaseModel):
    """解释生成请求"""
    cdpId: str
    diagnosisResult: Optional[Dict[str, Any]] = None

