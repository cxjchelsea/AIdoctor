"""
对话服务路由
按照《dialog-service - 服务实现方案.md》实现
"""
from fastapi import APIRouter, WebSocket, WebSocketDisconnect
from typing import Dict
from app.models.request import QuestionRequest, UserInputRequest, IdentifyGapsRequest
from app.models.response import (
    QuestionResponse,
    UnderstandingResponse,
    IdentifyGapsResponse,
    FieldConfigResponse
)
from app.services.dialog_service import DialogService
from app.utils.response import success_response

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

