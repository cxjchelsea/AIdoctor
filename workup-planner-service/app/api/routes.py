"""
检查建议服务API路由
"""
import time
from fastapi import APIRouter, HTTPException
from app.services.workup_planner import WorkupPlanner
from app.services.verification_planner import VerificationPlanner
from app.models.request import WorkupPlanRequest
from app.models.response import WorkupPlanResponse, VerificationPlan
from app.models.tool_context import ToolContext
from app.models.tool_result import ToolResult, Evidence, Quality, SuggestedWrite, ErrorInfo
from app.utils.exceptions import ServiceException
from app.utils.cdp_reader import read_cdp_fields
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


@router.post("/tools/tool_4/invoke", response_model=ToolResult)
async def invoke_tool_4(tool_context: ToolContext) -> ToolResult:
    """
    统一的工具调用接口（tool_4：检查建议工具）
    
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
        ddx = cdp_data.get("cdp.ddx", {})
        triage = cdp_data.get("cdp.triage", {})
        
        # 3. 构建现有服务的请求格式
        workup_request = WorkupPlanRequest(
            cdpId=cdp_id,
            cdp={"ddx": ddx, "triage": triage},
            ddx=ddx if isinstance(ddx, list) else []
        )
        
        # 4. 调用现有业务逻辑
        workup_result = workup_planner.plan_workup(workup_request.cdp or {})
        
        # 5. 转换为ToolResult格式
        duration_ms = int((time.time() - start_time) * 1000)
        
        # 构建evidence
        evidence_list = [
            Evidence(
                source="rule",
                reference="workup_planning_rule",
                strength="medium",
                evidence_name="检查建议生成规则"
            )
        ]
        
        # 构建suggested_writes
        suggested_writes = [
            SuggestedWrite(
                field_path="cdp.workup_plan",
                value=workup_result,
                reason="更新检查计划"
            )
        ]
        
        tool_result = ToolResult(
            trace_id=tool_context.trace_id,
            tool_id="tool_4",
            status="success",
            payload=workup_result if isinstance(workup_result, dict) else {"result": workup_result},
            evidence=evidence_list,
            quality=Quality(
                confidence=0.85,
                completeness=0.80,
                accuracy=0.82
            ),
            suggested_writes=suggested_writes,
            errors=[],
            duration_ms=duration_ms,
            metadata={}
        )
        
        return tool_result
        
    except Exception as e:
        duration_ms = int((time.time() - start_time) * 1000)
        logger.error(f"工具调用失败: tool_id=tool_4, trace_id={tool_context.trace_id}, error={str(e)}", exc_info=True)
        return ToolResult(
            trace_id=tool_context.trace_id,
            tool_id="tool_4",
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

