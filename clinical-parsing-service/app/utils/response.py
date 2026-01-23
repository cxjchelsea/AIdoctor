"""
统一响应格式
"""
import time
from typing import Any, Optional, List, Dict
from fastapi.responses import JSONResponse
from fastapi import status


def success_response(data: Any, message: str = "success") -> JSONResponse:
    """
    成功响应
    
    Args:
        data: 响应数据
        message: 响应消息
    
    Returns:
        JSONResponse
    """
    return JSONResponse(
        status_code=status.HTTP_200_OK,
        content={
            "code": 200,
            "message": message,
            "data": data,
            "timestamp": int(time.time() * 1000)
        }
    )


def error_response(
    code: int,
    message: str,
    errors: Optional[List[Dict[str, str]]] = None,
    trace_id: Optional[str] = None
) -> JSONResponse:
    """
    错误响应
    
    Args:
        code: 错误码
        message: 错误消息
        errors: 错误详情列表
        trace_id: 追踪ID
    
    Returns:
        JSONResponse
    """
    # 根据错误码确定HTTP状态码
    if 400 <= code < 500:
        http_status = status.HTTP_400_BAD_REQUEST
    elif code == 503:
        http_status = status.HTTP_503_SERVICE_UNAVAILABLE
    else:
        http_status = status.HTTP_500_INTERNAL_SERVER_ERROR
    
    content = {
        "code": code,
        "message": message,
        "data": None,
        "timestamp": int(time.time() * 1000)
    }
    
    if errors:
        content["errors"] = errors
    
    if trace_id:
        content["traceId"] = trace_id
    
    return JSONResponse(
        status_code=http_status,
        content=content
    )

