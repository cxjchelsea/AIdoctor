"""
风险评估服务API路由
"""
import time
from fastapi import APIRouter, HTTPException
from app.services.risk_assessment_engine import RiskAssessmentEngine
from app.services.triage_engine import TriageEngine
from app.services.upgrade_rule_engine import UpgradeRuleEngine
from app.builders.conclusion_package_builder import ConclusionPackageBuilder
from app.models.request import (
    RiskAssessmentRequest,
    TriageRequest,
    UpgradeRulesRequest,
    ConclusionPackageRequest
)
from app.models.response import (
    RiskAssessmentResponse,
    TriageResponse,
    UpgradeRulesResponse,
    ConclusionPackageResponse
)
from app.models.tool_context import ToolContext
from app.models.tool_result import ToolResult, Evidence, Quality, SuggestedWrite, ErrorInfo
from app.utils.cdp_reader import read_cdp_fields
import logging

logger = logging.getLogger(__name__)

router = APIRouter()

risk_assessment_engine = RiskAssessmentEngine()
triage_engine = TriageEngine()
upgrade_rule_engine = UpgradeRuleEngine()
conclusion_package_builder = ConclusionPackageBuilder()


@router.post("/risk/assess", response_model=RiskAssessmentResponse)
async def assess_risk(request: RiskAssessmentRequest):
    """
    风险评估
    识别高危情况，评估紧急程度，决定是否需要立即升级处理
    """
    try:
        # 构建CDP数据
        cdp = request.cdp or {}
        
        # 如果请求中提供了patient_state和ddx，则合并到cdp中
        if request.patient_state:
            cdp["patient_state"] = request.patient_state
        if request.ddx:
            cdp["ddx"] = request.ddx
        
        # 确保cdp中有必要字段
        if "patient_state" not in cdp:
            cdp["patient_state"] = {}
        if "ddx" not in cdp:
            cdp["ddx"] = []
        
        # 进行风险评估
        result = risk_assessment_engine.assess_risk(cdp)
        
        # 添加cdpId到结果中
        if request.cdpId:
            result["cdpId"] = request.cdpId
        
        return RiskAssessmentResponse(**result)
        
    except Exception as e:
        logger.error(f"风险评估失败: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"风险评估失败: {str(e)}"
        )


@router.post("/risk/triage", response_model=TriageResponse)
async def triage(request: TriageRequest):
    """
    分诊评估
    评估患者的紧急程度和分诊级别
    """
    try:
        result = triage_engine.triage(
            patient_state=request.patient_state,
            ddx=request.ddx
        )
        return TriageResponse(**result)
        
    except Exception as e:
        logger.error(f"分诊评估失败: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"分诊评估失败: {str(e)}"
        )


@router.post("/risk/upgrade-rules", response_model=UpgradeRulesResponse)
async def get_upgrade_rules(request: UpgradeRulesRequest):
    """
    获取升级规则
    根据风险等级和患者状态，确定复评与升级规则
    """
    try:
        result = upgrade_rule_engine.get_upgrade_rules(
            risk_level=request.risk_level,
            patient_state=request.patient_state
        )
        return UpgradeRulesResponse(**result)
        
    except Exception as e:
        logger.error(f"获取升级规则失败: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"获取升级规则失败: {str(e)}"
        )


@router.post("/risk/conclusion-package", response_model=ConclusionPackageResponse)
async def build_conclusion_package(request: ConclusionPackageRequest):
    """
    构建终点结论包
    包含结论、必须排除项状态、关键依据、行动与随访
    """
    try:
        result = conclusion_package_builder.build_conclusion_package(
            three_layer_result=request.three_layer_result,
            evidence_analysis=request.evidence_analysis or {},
            risk_level=request.risk_level
        )
        return ConclusionPackageResponse(**result)
        
    except Exception as e:
        logger.error(f"构建终点结论包失败: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"构建终点结论包失败: {str(e)}"
        )


@router.post("/risk/assess-final", response_model=RiskAssessmentResponse)
async def assess_final_risk(request: dict):
    """
    最终风险评估
    在诊断流程结束时进行最终风险评估
    """
    try:
        cdp = request.get("cdp", {})
        if not cdp:
            # 如果请求中没有cdp字段，尝试从其他字段构建
            cdp = {
                "id": request.get("cdpId", ""),
                "patient_state": request.get("patient_state", {}),
                "ddx": request.get("ddx", []),
                "management_plan": request.get("management_plan", request.get("managementPlan", []))
            }
        
        result = risk_assessment_engine.assess_risk(cdp)
        
        # 添加cdpId到结果中
        if request.get("cdpId"):
            result["cdpId"] = request.get("cdpId")
        
        return RiskAssessmentResponse(**result)
        
    except Exception as e:
        logger.error(f"最终风险评估失败: {str(e)}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"最终风险评估失败: {str(e)}"
        )


@router.post("/tools/tool_6/invoke", response_model=ToolResult)
async def invoke_tool_6(tool_context: ToolContext) -> ToolResult:
    """
    统一的工具调用接口（tool_6：风险评估工具）
    
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
        ddx = cdp_data.get("cdp.ddx", {})
        
        # 3. 构建现有服务的请求格式
        risk_request = RiskAssessmentRequest(
            cdpId=cdp_id,
            cdp={"patient_state": patient_state, "ddx": ddx},
            patient_state=patient_state,
            ddx=ddx if isinstance(ddx, list) else []
        )
        
        # 4. 调用现有业务逻辑
        risk_result = risk_assessment_engine.assess_risk(risk_request.cdp or {})
        
        # 5. 转换为ToolResult格式
        duration_ms = int((time.time() - start_time) * 1000)
        
        # 构建evidence
        evidence_list = [
            Evidence(
                source="rule",
                reference="risk_assessment_rule",
                strength="strong",
                evidence_name="风险评估规则"
            )
        ]
        
        # 构建suggested_writes
        suggested_writes = [
            SuggestedWrite(
                field_path="cdp.triage",
                value=risk_result,
                reason="更新风险评估结果"
            )
        ]
        
        tool_result = ToolResult(
            trace_id=tool_context.trace_id,
            tool_id="tool_6",
            status="success",
            payload=risk_result if isinstance(risk_result, dict) else {"result": risk_result},
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
        logger.error(f"工具调用失败: tool_id=tool_6, trace_id={tool_context.trace_id}, error={str(e)}", exc_info=True)
        return ToolResult(
            trace_id=tool_context.trace_id,
            tool_id="tool_6",
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
