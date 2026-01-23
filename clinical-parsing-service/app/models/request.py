"""
病例理解请求模型
"""
from pydantic import BaseModel
from typing import List, Optional, Dict, Any

class ClinicalParsingRequest(BaseModel):
    """病例理解请求"""
    userId: str
    sessionId: str
    text: str
    cdpId: Optional[str] = None
    input: Optional[Dict[str, Any]] = None

