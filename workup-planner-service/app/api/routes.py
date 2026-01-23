"""
检查建议服务API路由
"""
from fastapi import APIRouter
from app.services.workup_planner import WorkupPlanner
from app.services.verification_planner import VerificationPlanner
from app.models.request import WorkupPlanRequest
from app.models.response import WorkupPlanResponse

router = APIRouter()

workup_planner = WorkupPlanner()
verification_planner = VerificationPlanner()


@router.post("/workup/plan", response_model=WorkupPlanResponse)
async def plan_workup(request: WorkupPlanRequest):
    """
    生成检查建议
    """
    result = workup_planner.plan_workup(request.cdp)
    return WorkupPlanResponse(**result)


@router.post("/workup/verification-plan")
async def build_verification_plan(request: dict):
    """
    构建验证计划
    """
    result = verification_planner.build_verification_plan(
        must_exclude=request.get("must_exclude"),
        key_differentiating_points=request.get("key_differentiating_points", [])
    )
    return result

