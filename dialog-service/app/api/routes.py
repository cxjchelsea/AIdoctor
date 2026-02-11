"""
对话服务路由
按照《dialog-service - 服务实现方案.md》实现
"""
import time
from fastapi import APIRouter, WebSocket, WebSocketDisconnect
from typing import Dict
from app.models.request import QuestionRequest, UserInputRequest, IdentifyGapsRequest
from app.models.response import (
    QuestionResponse,
    UnderstandingResponse,
    IdentifyGapsResponse,
    FieldConfigResponse
)
from app.models.tool_context import ToolContext
from app.models.tool_result import ToolResult, Evidence, Quality, SuggestedWrite, ErrorInfo
from app.services.dialog_service import DialogService
from app.utils.response import success_response
from app.utils.cdp_reader import read_cdp_fields
import logging

logger = logging.getLogger(__name__)

router = APIRouter()
dialog_service = DialogService()


@router.post("/generate-question", response_model=QuestionResponse)
async def generate_question(request: QuestionRequest) -> QuestionResponse:
    """
    生成追问问题接口
    
    基于当前信息状态生成智能追问问题
    """
    result = await dialog_service.generate_question(request)
    return QuestionResponse(**result)


@router.post("/understand", response_model=UnderstandingResponse)
async def understand(request: UserInputRequest) -> UnderstandingResponse:
    """
    理解用户输入接口
    
    理解用户的自然语言输入，提取结构化信息
    """
    result = await dialog_service.understand(request)
    return UnderstandingResponse(**result)


@router.post("/identify-gaps", response_model=IdentifyGapsResponse)
async def identify_gaps(request: IdentifyGapsRequest) -> IdentifyGapsResponse:
    """
    识别信息缺口接口
    
    识别当前信息缺口并分级
    """
    result = await dialog_service.identify_gaps(request)
    return IdentifyGapsResponse(**result)


@router.get("/field-config")
async def get_field_config():
    """
    获取字段配置接口
    
    返回所有字段配置（必填/重要/可选），用于前端动态生成信息收集列表
    """
    result = await dialog_service.get_field_config()
    # 将结果转换为Pydantic模型，然后序列化为字典
    field_config_response = FieldConfigResponse(**result)
    return success_response(field_config_response.dict())


@router.post("/design-routing-path")
async def design_routing_path(request: dict):
    """
    设计分流路径接口
    
    基于推理子组和关键差异点设计分流路径
    """
    result = await dialog_service.design_routing_path(request)
    return result


@router.post("/collect-key-evidence")
async def collect_key_evidence(request: dict):
    """
    采集关键证据接口
    
    基于分流路径采集关键证据
    """
    result = await dialog_service.collect_key_evidence(request)
    return result


@router.websocket("/ws/{cdp_id}")
async def websocket_dialog(websocket: WebSocket, cdp_id: str):
    """
    WebSocket实时对话接口
    
    支持双向通信的实时对话
    """
    await websocket.accept()
    
    try:
        while True:
            # 接收用户消息
            data = await websocket.receive_text()
            
            # 处理消息并生成回复
            response = await dialog_service.handle_websocket_message(
                cdp_id=cdp_id,
                message=data
            )
            
            # 发送回复
            await websocket.send_json(response)
            
    except WebSocketDisconnect:
        # 清理上下文
        await dialog_service.cleanup_context(cdp_id)


@router.post("/tools/tool_2/invoke", response_model=ToolResult)
async def invoke_tool_2(tool_context: ToolContext) -> ToolResult:
    """
    统一的工具调用接口（tool_2：主动问诊工具）
    
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
        uncertainty = cdp_data.get("cdp.uncertainty", {})
        missing_critical_info = uncertainty.get("missing_critical_info", []) if isinstance(uncertainty, dict) else []
        
        # 3. 构建现有服务的请求格式
        question_request = QuestionRequest(
            cdpId=cdp_id,
            patientState=patient_state,
            ddx=ddx,
            missingCriticalInfo=missing_critical_info
        )
        
        # 4. 调用现有业务逻辑
        question_result = await dialog_service.generate_question(question_request)
        
        # 5. 转换为ToolResult格式
        duration_ms = int((time.time() - start_time) * 1000)
        
        # 构建evidence
        evidence_list = [
            Evidence(
                source="llm",
                reference="question_generation_prompt",
                strength="medium",
                evidence_name="智能追问生成"
            )
        ]
        
        # 构建suggested_writes（如果有提取的信息，建议写回CDP）
        suggested_writes = []
        if hasattr(question_result, 'extracted_info') and question_result.extracted_info:
            suggested_writes.append(SuggestedWrite(
                field_path="cdp.patient_state",
                value=question_result.extracted_info,
                reason="更新从追问中提取的患者信息"
            ))
        
        tool_result = ToolResult(
            trace_id=tool_context.trace_id,
            tool_id="tool_2",
            status="success",
            payload={
                "question": question_result.question if hasattr(question_result, 'question') else question_result.get("question", ""),
                "question_type": question_result.questionType if hasattr(question_result, 'questionType') else question_result.get("questionType", ""),
                "reasoning": question_result.reasoning if hasattr(question_result, 'reasoning') else question_result.get("reasoning", ""),
                "extracted_info": question_result.extracted_info if hasattr(question_result, 'extracted_info') else question_result.get("extracted_info", {})
            },
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
        logger.error(f"工具调用失败: tool_id=tool_2, trace_id={tool_context.trace_id}, error={str(e)}", exc_info=True)
        return ToolResult(
            trace_id=tool_context.trace_id,
            tool_id="tool_2",
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

