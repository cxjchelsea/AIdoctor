"""
检查建议服务API路由
"""
from fastapi import APIRouter, HTTPException
from app.services.workup_planner import WorkupPlanner
from app.services.verification_planner import VerificationPlanner
from app.models.request import WorkupPlanRequest
from app.models.response import WorkupPlanResponse, VerificationPlan
from app.utils.exceptions import ServiceException
import logging

logger = logging.getLogger(__name__)

router = APIRouter()

workup_planner = WorkupPlanner()
verification_planner = VerificationPlanner()


@router.post("/workup/plan", response_model=WorkupPlanResponse)
async def plan_workup(request: WorkupPlanRequest):
    """
    生成检查建议
    基于当前DDx和已有证据，建议下一步检查，并评估检查的价值，制定验证计划
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
        
        # 生成检查建议
        result = workup_planner.plan_workup(cdp)
        
        # 添加cdpId到结果中
        if request.cdpId:
            result["cdpId"] = request.cdpId
        
        return WorkupPlanResponse(**result)
        
    except Exception as e:
        logger.error(f"生成检查建议失败: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"生成检查建议失败: {str(e)}"
        )


@router.post("/workup/verification-plan", response_model=VerificationPlan)
async def build_verification_plan(request: dict):
    """
    构建验证计划
    针对必须排除的高危诊断，制定专门的验证计划
    """
    try:
        must_exclude = request.get("must_exclude")
        key_differentiating_points = request.get("key_differentiating_points", [])
        
        if not must_exclude:
            raise HTTPException(
                status_code=400,
                detail="must_exclude参数不能为空"
            )
        
        result = verification_planner.build_verification_plan(
            must_exclude=must_exclude,
            key_differentiating_points=key_differentiating_points
        )
        
        return VerificationPlan(**result)
        
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"构建验证计划失败: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"构建验证计划失败: {str(e)}"
        )

