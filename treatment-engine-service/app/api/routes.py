"""
治疗推理服务API路由
"""
import time
from typing import List
from fastapi import APIRouter, HTTPException
from app.services.treatment_engine import TreatmentEngine
from app.services.medication_recommender import MedicationRecommender
from app.models.request import TreatmentPlanRequest
from app.models.response import TreatmentPlanResponse, MedicationAdvice
from app.models.tool_context import ToolContext
from app.models.tool_result import ToolResult, Evidence, Quality, SuggestedWrite, ErrorInfo
from app.utils.cdp_reader import read_cdp_fields
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


@router.post("/tools/tool_5/invoke", response_model=ToolResult)
async def invoke_tool_5(tool_context: ToolContext) -> ToolResult:
    """
    统一的工具调用接口（tool_5：治疗建议工具）
    
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
        treatment_request = TreatmentPlanRequest(
            cdpId=cdp_id,
            cdp={"ddx": ddx, "triage": triage},
            ddx=ddx if isinstance(ddx, list) else []
        )
        
        # 4. 调用现有业务逻辑
        treatment_result = treatment_engine.plan_treatment(treatment_request.cdp or {})
        
        # 5. 转换为ToolResult格式
        duration_ms = int((time.time() - start_time) * 1000)
        
        # 构建evidence
        evidence_list = [
            Evidence(
                source="rule",
                reference="treatment_planning_rule",
                strength="medium",
                evidence_name="治疗建议生成规则"
            )
        ]
        
        # 构建suggested_writes
        suggested_writes = [
            SuggestedWrite(
                field_path="cdp.management_plan",
                value=treatment_result,
                reason="更新治疗计划"
            )
        ]
        
        tool_result = ToolResult(
            trace_id=tool_context.trace_id,
            tool_id="tool_5",
            status="success",
            payload=treatment_result if isinstance(treatment_result, dict) else {"result": treatment_result},
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
        logger.error(f"工具调用失败: tool_id=tool_5, trace_id={tool_context.trace_id}, error={str(e)}", exc_info=True)
        return ToolResult(
            trace_id=tool_context.trace_id,
            tool_id="tool_5",
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

