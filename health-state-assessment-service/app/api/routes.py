"""
健康状态判定服务API路由
"""
import time
from fastapi import APIRouter, HTTPException
from typing import Dict, Any

from app.models.request import HealthStateAssessmentRequest
from app.models.response import HealthStateAssessmentResponse
from app.models.tool_context import ToolContext
from app.models.tool_result import ToolResult, Evidence, Quality, SuggestedWrite, ErrorInfo
from app.services.health_state_assessment import HealthStateAssessmentService
from app.utils.exceptions import BusinessException
from app.utils.specific_exceptions import (
    UserIdEmptyException,
    UserInputEmptyException
)
from app.utils.logger import logger
from app.utils.cdp_reader import read_cdp_fields

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
    健康状态判定（tool_0）
    
    判定用户是否需要进入临床诊疗态，决定工作态（健康管理态/临床诊疗态）。
    这是诊断流程的第一步（tool_0）。
    
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


@router.post("/tools/tool_0/invoke", response_model=ToolResult)
async def invoke_tool_0(tool_context: ToolContext) -> ToolResult:
    """
    统一的工具调用接口（tool_0：健康状态判定工具）
    
    接收ToolContext，返回ToolResult
    """
    start_time = time.time()
    
    try:
        # 1. 从ToolContext中提取CDP数据
        cdp_id = tool_context.cdp_reference.cdp_id
        cdp_version = tool_context.cdp_reference.version
        read_fields = tool_context.cdp_reference.read_fields
        
        # 2. 从CDP读取数据（根据read_fields）
        cdp_data = await read_cdp_fields(cdp_id, cdp_version, read_fields)
        patient_state = cdp_data.get("cdp.patient_state", {})
        
        # 3. 构建现有服务的请求格式
        assessment_request = HealthStateAssessmentRequest(
            userId=patient_state.get("user_id", "") or tool_context.call_params.get("userId", ""),
            cdpId=cdp_id,
            userInput=patient_state.get("user_input", "") or patient_state.get("text", ""),
            basicInfo=patient_state.get("basic_info", {}),
            symptoms=patient_state.get("symptoms", []),
            vitalSigns=patient_state.get("vital_signs", {})
        )
        
        # 4. 调用现有业务逻辑
        assessment_result = await service.assess(assessment_request)
        
        # 5. 转换为ToolResult格式
        duration_ms = int((time.time() - start_time) * 1000)
        
        # 构建evidence
        evidence_list = [
            Evidence(
                source="rule",
                reference="work_mode_determination_rule",
                strength="strong",
                evidence_name="工作态判定规则"
            )
        ]
        if assessment_result.riskLevel:
            evidence_list.append(Evidence(
                source="rule",
                reference="risk_assessment_rule",
                strength="strong",
                evidence_name=f"风险等级判定: {assessment_result.riskLevel}"
            ))
        
        # 构建suggested_writes
        suggested_writes = [
            SuggestedWrite(
                field_path="cdp.health_state_assessment",
                value=assessment_result.model_dump(),
                reason="更新健康状态判定结果"
            )
        ]
        if assessment_result.wellnessPlan:
            suggested_writes.append(SuggestedWrite(
                field_path="cdp.wellness_plan",
                value=assessment_result.wellnessPlan.model_dump() if hasattr(assessment_result.wellnessPlan, 'model_dump') else assessment_result.wellnessPlan,
                reason="更新健康管理计划"
            ))
        
        tool_result = ToolResult(
            trace_id=tool_context.trace_id,
            tool_id="tool_0",
            status="success",
            payload=assessment_result.model_dump(),
            evidence=evidence_list,
            quality=Quality(
                confidence=0.9,
                completeness=0.85,
                accuracy=0.88
            ),
            suggested_writes=suggested_writes,
            errors=[],
            duration_ms=duration_ms,
            metadata={}
        )
        
        return tool_result
        
    except Exception as e:
        duration_ms = int((time.time() - start_time) * 1000)
        logger.error(f"工具调用失败: tool_id=tool_0, trace_id={tool_context.trace_id}, error={str(e)}", exc_info=True)
        return ToolResult(
            trace_id=tool_context.trace_id,
            tool_id="tool_0",
            status="failure",
            payload={},
            evidence=[],
            quality=Quality(confidence=0.0, completeness=0.0, accuracy=0.0),
            suggested_writes=[],
            errors=[ErrorInfo(
                error_type="runtime_error",
                error_message=str(e),
                error_details={}
            )],
            duration_ms=duration_ms,
            metadata={}
        )