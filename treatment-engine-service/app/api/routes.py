"""
治疗推理服务API路由
"""
from fastapi import APIRouter
from app.services.treatment_engine import TreatmentEngine
from app.services.medication_recommender import MedicationRecommender
from app.models.request import TreatmentPlanRequest
from app.models.response import TreatmentPlanResponse

router = APIRouter()

treatment_engine = TreatmentEngine()
medication_recommender = MedicationRecommender()


@router.post("/treatment/plan", response_model=TreatmentPlanResponse)
async def plan_treatment(request: TreatmentPlanRequest):
    """
    生成治疗建议
    """
    result = treatment_engine.plan_treatment(request.cdp)
    return TreatmentPlanResponse(**result)


@router.post("/treatment/medication")
async def recommend_medication(request: dict):
    """
    推荐药物
    """
    result = medication_recommender.recommend(
        diagnosis=request.get("diagnosis"),
        patient_state=request.get("patient_state", {})
    )
    return result

