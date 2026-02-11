"""
异常处理
按照《AI医生系统-错误处理规范.md》定义
"""
import time
import logging
from fastapi import Request, status
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError

logger = logging.getLogger(__name__)


class BusinessException(Exception):
    """业务异常基类"""
    def __init__(self, code: int, message: str):
        self.code = code
        self.message = message
        super().__init__(message)


def get_http_status_code(code: int) -> int:
    """根据错误码获取HTTP状态码"""
    # tool_1：病例理解服务错误码（1100-1199）
    if 1100 <= code < 1200:
        return status.HTTP_500_INTERNAL_SERVER_ERROR
    # 通用错误码（5000-5999）
    elif 5000 <= code < 6000:
        if code in (5001, 5002):
            return status.HTTP_400_BAD_REQUEST
        if code == 5006:
            return status.HTTP_503_SERVICE_UNAVAILABLE
        return status.HTTP_500_INTERNAL_SERVER_ERROR
    # 默认返回500
    return status.HTTP_500_INTERNAL_SERVER_ERROR


def setup_exception_handlers(app):
    """设置异常处理器"""
    
    @app.exception_handler(BusinessException)
    async def business_exception_handler(request: Request, exc: BusinessException):
        """处理业务异常"""
        logger.warning(f"业务异常: code={exc.code}, message={exc.message}, path={request.url.path}")
        
        http_status = get_http_status_code(exc.code)
        
        # 生成traceId（简单实现，实际应该使用分布式追踪系统）
        import uuid
        trace_id = str(uuid.uuid4())[:16]
        
        # 从请求头获取requestId
        request_id = request.headers.get("x-request-id") or request.headers.get("X-Request-Id")
        
        # 从请求中提取cdpId（如果存在）
        cdp_id = None
        try:
            # 尝试从请求体或查询参数中获取cdpId
            if hasattr(request.state, 'cdp_id'):
                cdp_id = request.state.cdp_id
        except:
            pass
        
        # 根据错误类型判断是否可重试
        # tool_1错误码（1100-1199）中的业务错误通常可重试
        retryable = (1100 <= exc.code < 1200) and exc.code not in (1105, 1106)  # 歧义判定和OCR失败可能不可重试
        
        return JSONResponse(
            status_code=http_status,
            content={
                "code": exc.code,
                "message": exc.message,
                "data": None,
                "timestamp": int(time.time() * 1000),
                "traceId": trace_id,
                "requestId": request_id,
                "service": "clinical-parsing-service",
                "agentId": "tool_1",
                "cdpId": cdp_id,
                "retryable": retryable,
                "path": str(request.url.path)
            }
        )
    
    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(request: Request, exc: RequestValidationError):
        """处理参数验证异常"""
        logger.warning(f"参数验证失败: path={request.url.path}")
        
        errors = []
        for error in exc.errors():
            field = ".".join(str(loc) for loc in error["loc"] if loc != "body")
            errors.append({
                "field": field,
                "message": error["msg"]
            })
        
        # 生成traceId
        import uuid
        trace_id = str(uuid.uuid4())[:16]
        
        # 从请求头获取requestId
        request_id = request.headers.get("x-request-id") or request.headers.get("X-Request-Id")
        
        return JSONResponse(
            status_code=status.HTTP_400_BAD_REQUEST,
            content={
                "code": 5001,
                "message": "参数验证失败",
                "data": None,
                "errors": errors,
                "timestamp": int(time.time() * 1000),
                "traceId": trace_id,
                "requestId": request_id,
                "service": "clinical-parsing-service",
                "agentId": "tool_1",
                "cdpId": None,
                "retryable": False,
                "path": str(request.url.path)
            }
        )
    
    @app.exception_handler(Exception)
    async def general_exception_handler(request: Request, exc: Exception):
        """处理未知异常"""
        logger.error(f"系统异常: path={request.url.path}", exc_info=True)
        
        # 生成traceId
        import uuid
        trace_id = str(uuid.uuid4())[:16]
        
        # 从请求头获取requestId
        request_id = request.headers.get("x-request-id") or request.headers.get("X-Request-Id")
        
        return JSONResponse(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            content={
                "code": 500,
                "message": "系统内部错误",
                "data": None,
                "timestamp": int(time.time() * 1000),
                "traceId": trace_id,
                "requestId": request_id,
                "service": "clinical-parsing-service",
                "agentId": "tool_1",
                "cdpId": None,
                "retryable": False,
                "path": str(request.url.path)
            }
        )

