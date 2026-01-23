"""
解释生成服务API路由
"""
from fastapi import APIRouter
from app.models.request import ExplanationRequest
from app.models.response import ExplanationResponse, EvidenceChain, ConclusionPackage
from app.services.explanation_service import ExplanationService

router = APIRouter()
service = ExplanationService()

@router.post("/explain", response_model=ExplanationResponse)
async def explain(request: ExplanationRequest):
    """
    生成可解释性结果
    生成完整的证据链、推理路径、终点结论包和自然语言解释
    """
    return await service.explain(request)

@router.post("/explain/evidence-chain", response_model=EvidenceChain)
async def generate_evidence_chain(request: ExplanationRequest):
    """
    仅生成证据链
    """
    return await service.generate_evidence_chain(request)

@router.post("/explain/conclusion-package", response_model=ConclusionPackage)
async def generate_conclusion_package(request: ExplanationRequest):
    """
    仅生成终点结论包
    """
    return await service.generate_conclusion_package(request)

@router.post("/explain/natural-language")
async def generate_natural_language(request: ExplanationRequest):
    """
    仅生成自然语言解释
    """
    return await service.generate_natural_language(request)

