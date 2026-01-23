"""
健康状态判定服务API路由
"""
import time
from fastapi import APIRouter, HTTPException
from typing import Dict, Any

from app.models.request import HealthStateAssessmentRequest
from app.models.response import HealthStateAssessmentResponse
from app.services.health_state_assessment import HealthStateAssessmentService
from app.utils.exceptions import BusinessException
from app.utils.specific_exceptions import (
    UserIdEmptyException,
    UserInputEmptyException
)
from app.utils.logger import logger

router = APIRouter()
service = HealthStateAssessmentService()


def create_api_response(data: HealthStateAssessmentResponse) -> Dict[str, Any]:
    """创建统一API响应格式"""
    return {
        "code": 200,
        "message": "success",
        "data": data.model_dump(),
        "timestamp": int(time.time() * 1000)  # 毫秒时间戳
    }


@router.post("/health-state-assessment/assess")
async def assess_health_state(request: HealthStateAssessmentRequest) -> Dict[str, Any]:
    """
    健康状态判定（脑区0）
    
    判定用户是否需要进入临床诊疗态，决定工作态（健康管理态/临床诊疗态）。
    这是诊断流程的第一步（脑区0）。
    
    整合入口判定流程（P0模块，Step 1-5）和工作态判定。
    """
    logger.info(f"收到健康状态判定请求: userId={request.userId}")
    
    try:
        # 参数验证
        if not request.userId:
            raise UserIdEmptyException()
        
        if not request.userInput and not request.symptoms:
            raise UserInputEmptyException("用户输入或症状列表不能同时为空")
        
        # 执行健康状态判定
        result = await service.assess(request)
        
        logger.info(
            f"健康状态判定完成: userId={request.userId}, "
            f"workMode={result.workMode}, riskLevel={result.riskLevel}, "
            f"cdpId={result.cdpId}"
        )
        
        return create_api_response(result)
    except BusinessException as e:
        logger.warning(f"业务异常: code={e.code}, message={e.message}")
        raise
    except Exception as e:
        logger.error(f"健康状态判定失败: userId={request.userId}", exc_info=True)
        raise BusinessException(1001, f"健康状态判定失败：{str(e)}")


# ========== 健康筛查流程接口（A路径） ==========

from app.services.wellness_screening_service import WellnessScreeningService

wellness_screening_service = WellnessScreeningService()


@router.post("/wellness-screening/a1-demand-classification")
async def a1_demand_classification(request: Dict[str, Any]) -> Dict[str, Any]:
    """
    A1: 需求分类
    
    分类用户需求类型（筛查建议/健康目标管理/报告解读）
    """
    logger.info(f"A1: 需求分类 - cdpId={request.get('cdpId')}")
    
    try:
        result = wellness_screening_service.a1_demand_classification(request)
        return {
            "code": 200,
            "message": "success",
            "data": result,
            "timestamp": int(time.time() * 1000)
        }
    except Exception as e:
        logger.error(f"A1需求分类失败: cdpId={request.get('cdpId')}", exc_info=True)
        from app.utils.specific_exceptions import WellnessScreeningA1Exception
        raise WellnessScreeningA1Exception(f"A1需求分类失败：{str(e)}")


@router.post("/wellness-screening/a2-health-profile-collection")
async def a2_health_profile_collection(request: Dict[str, Any]) -> Dict[str, Any]:
    """
    A2: 收集健康画像
    
    收集用户的健康画像信息，计算完整度
    """
    logger.info(f"A2: 收集健康画像 - cdpId={request.get('cdpId')}")
    
    try:
        result = wellness_screening_service.a2_health_profile_collection(request)
        return {
            "code": 200,
            "message": "success",
            "data": result,
            "timestamp": int(time.time() * 1000)
        }
    except Exception as e:
        logger.error(f"A2收集健康画像失败: cdpId={request.get('cdpId')}", exc_info=True)
        from app.utils.specific_exceptions import WellnessScreeningA2Exception
        raise WellnessScreeningA2Exception(f"A2收集健康画像失败：{str(e)}")


@router.post("/wellness-screening/a3-branch-execution")
async def a3_branch_execution(request: Dict[str, Any]) -> Dict[str, Any]:
    """
    A3: 执行分支
    
    根据需求类型执行对应的分支（筛查建议/健康目标管理/报告解读）
    """
    logger.info(f"A3: 执行分支 - cdpId={request.get('cdpId')}")
    
    try:
        result = wellness_screening_service.a3_branch_execution(request)
        return {
            "code": 200,
            "message": "success",
            "data": result,
            "timestamp": int(time.time() * 1000)
        }
    except Exception as e:
        logger.error(f"A3执行分支失败: cdpId={request.get('cdpId')}", exc_info=True)
        from app.utils.specific_exceptions import WellnessScreeningA3Exception
        raise WellnessScreeningA3Exception(f"A3执行分支失败：{str(e)}")


@router.post("/wellness-screening/a4-unified-result-generation")
async def a4_unified_result_generation(request: Dict[str, Any]) -> Dict[str, Any]:
    """
    A4: 生成统一结果
    
    整合分支执行结果，生成统一的结果和摘要
    """
    logger.info(f"A4: 生成统一结果 - cdpId={request.get('cdpId')}")
    
    try:
        result = wellness_screening_service.a4_unified_result_generation(request)
        return {
            "code": 200,
            "message": "success",
            "data": result,
            "timestamp": int(time.time() * 1000)
        }
    except Exception as e:
        logger.error(f"A4生成统一结果失败: cdpId={request.get('cdpId')}", exc_info=True)
        from app.utils.specific_exceptions import WellnessScreeningA4Exception
        raise WellnessScreeningA4Exception(f"A4生成统一结果失败：{str(e)}")


@router.post("/wellness-screening/a5-follow-up-setup")
async def a5_follow_up_setup(request: Dict[str, Any]) -> Dict[str, Any]:
    """
    A5: 设置随访
    
    根据统一结果设置随访计划
    """
    logger.info(f"A5: 设置随访 - cdpId={request.get('cdpId')}")
    
    try:
        result = wellness_screening_service.a5_follow_up_setup(request)
        return {
            "code": 200,
            "message": "success",
            "data": result,
            "timestamp": int(time.time() * 1000)
        }
    except Exception as e:
        logger.error(f"A5设置随访失败: cdpId={request.get('cdpId')}", exc_info=True)
        from app.utils.specific_exceptions import WellnessScreeningA5Exception
        raise WellnessScreeningA5Exception(f"A5设置随访失败：{str(e)}")