"""
治疗推理服务API路由
"""
from typing import List
from fastapi import APIRouter, HTTPException
from app.services.treatment_engine import TreatmentEngine
from app.services.medication_recommender import MedicationRecommender
from app.models.request import TreatmentPlanRequest
from app.models.response import TreatmentPlanResponse, MedicationAdvice
import logging

logger = logging.getLogger(__name__)

router = APIRouter()

treatment_engine = TreatmentEngine()
medication_recommender = MedicationRecommender()


@router.post("/treatment/plan", response_model=TreatmentPlanResponse)
async def plan_treatment(request: TreatmentPlanRequest):
    """
    生成治疗建议
    基于诊断结果，生成完整的治疗建议（包括治疗方案、药物推荐、非药物治疗建议）
    """
    try:
        # 构建CDP数据
        cdp = request.cdp or {}
        
        # 如果请求中提供了ddx和patient_state，则合并到cdp中
        if request.ddx:
            cdp["ddx"] = request.ddx
        if request.patient_state:
            cdp["patient_state"] = request.patient_state
        
        # 确保cdp中有ddx字段
        if "ddx" not in cdp:
            cdp["ddx"] = []
        
        # 生成治疗建议
        result = treatment_engine.plan_treatment(cdp)
        
        # 添加cdpId到结果中
        if request.cdpId:
            result["cdpId"] = request.cdpId
        
        return TreatmentPlanResponse(**result)
        
    except Exception as e:
        logger.error(f"生成治疗建议失败: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"生成治疗建议失败: {str(e)}"
        )


@router.post("/treatment/medication", response_model=List[MedicationAdvice])
async def recommend_medication(request: dict):
    """
    推荐药物
    基于诊断和患者情况，推荐合适的药物
    """
    try:
        diagnosis = request.get("diagnosis")
        patient_state = request.get("patient_state", {})
        
        if not diagnosis:
            raise HTTPException(
                status_code=400,
                detail="diagnosis参数不能为空"
            )
        
        result = medication_recommender.recommend(
            diagnosis=diagnosis,
            patient_state=patient_state
        )
        
        return [MedicationAdvice(**item) for item in result]
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"推荐药物失败: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"推荐药物失败: {str(e)}"
        )

