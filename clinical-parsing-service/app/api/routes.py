"""
病例理解服务API路由
"""
import logging
from fastapi import APIRouter
from app.models.request import ClinicalParsingRequest
from app.models.response import ClinicalParsingResponse
from app.services.parsing_service import ClinicalParsingService
from app.utils.response import success_response

logger = logging.getLogger(__name__)

router = APIRouter()
service = ClinicalParsingService()

@router.post("/parsing/parse")
async def parse_clinical_data(request: ClinicalParsingRequest):
    """
    病例理解与结构化
    
    将非结构化的患者信息转换为结构化的临床要素
    
    - **userId**: 用户ID
    - **sessionId**: 会话ID
    - **text**: 输入文本
    - **cdpId**: CDP ID（可选）
    - **input**: 额外输入信息（可选）
    """
    logger.info(f"收到病例理解请求: userId={request.userId}, sessionId={request.sessionId}")
    result = await service.parse(request)
    return success_response(result.dict())

@router.get("/parsing/health")
async def health_check():
    """健康检查"""
    return success_response({
        "status": "healthy",
        "service": "clinical-parsing-service"
    })

