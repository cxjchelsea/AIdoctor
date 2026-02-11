"""
解释生成服务API路由
"""
import time
from fastapi import APIRouter
from app.models.request import ExplanationRequest
from app.models.response import ExplanationResponse, EvidenceChain, ConclusionPackage
from app.models.tool_context import ToolContext
from app.models.tool_result import ToolResult, Evidence, Quality, SuggestedWrite, ErrorInfo
from app.services.explanation_service import ExplanationService
from app.utils.cdp_reader import read_cdp_fields
import logging

logger = logging.getLogger(__name__)

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


@router.post("/tools/tool_7/invoke", response_model=ToolResult)
async def invoke_tool_7(tool_context: ToolContext) -> ToolResult:
    """
    统一的工具调用接口（tool_7：解释生成工具）
    
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
        evidence_graph = cdp_data.get("cdp.evidence_graph", {})
        workup_plan = cdp_data.get("cdp.workup_plan", {})
        management_plan = cdp_data.get("cdp.management_plan", {})
        
        # 3. 构建现有服务的请求格式
        explanation_request = ExplanationRequest(
            cdpId=cdp_id,
            diagnosisResult={
                "ddx": ddx,
                "evidence_graph": evidence_graph,
                "workup_plan": workup_plan,
                "management_plan": management_plan
            }
        )
        
        # 4. 调用现有业务逻辑
        explanation_result = await service.explain(explanation_request)
        
        # 5. 转换为ToolResult格式
        duration_ms = int((time.time() - start_time) * 1000)
        
        # 构建evidence（从解释结果中提取）
        evidence_list = []
        if hasattr(explanation_result, 'evidence_chain') and explanation_result.evidence_chain:
            evidence_list.append(Evidence(
                source="kg_path",
                reference="evidence_chain",
                strength="strong",
                evidence_name="证据链"
            ))
        
        # 构建suggested_writes
        suggested_writes = [
            SuggestedWrite(
                field_path="cdp.evidence_graph",
                value=explanation_result.evidence_chain.dict() if hasattr(explanation_result, 'evidence_chain') else {},
                reason="更新证据图"
            )
        ]
        
        tool_result = ToolResult(
            trace_id=tool_context.trace_id,
            tool_id="tool_7",
            status="success",
            payload=explanation_result.dict() if hasattr(explanation_result, 'dict') else {"result": explanation_result},
            evidence=evidence_list if evidence_list else [
                Evidence(
                    source="llm",
                    reference="explanation_generation",
                    strength="medium",
                    evidence_name="解释生成"
                )
            ],
            quality=Quality(
                confidence=0.85,
                completeness=0.90,
                accuracy=0.80
            ),
            suggested_writes=suggested_writes,
            errors=[],
            duration_ms=duration_ms,
            metadata={}
        )
        
        return tool_result
        
    except Exception as e:
        duration_ms = int((time.time() - start_time) * 1000)
        logger.error(f"工具调用失败: tool_id=tool_7, trace_id={tool_context.trace_id}, error={str(e)}", exc_info=True)
        return ToolResult(
            trace_id=tool_context.trace_id,
            tool_id="tool_7",
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

