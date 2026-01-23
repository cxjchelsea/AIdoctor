"""
执行追踪装饰器
用于自动追踪Python服务的方法调用
"""
import uuid
import time
import logging
import asyncio
from functools import wraps
from typing import Dict, Any, Optional, Callable
import httpx
from contextvars import ContextVar

logger = logging.getLogger(__name__)

# 使用contextvars存储CDP ID（支持异步）
_cdp_id: ContextVar[Optional[str]] = ContextVar('cdp_id', default=None)


class TraceClient:
    """统一追踪客户端"""
    
    def __init__(self, trace_service_url: str = "http://localhost:8093"):
        self.trace_service_url = trace_service_url
        self.http_client = httpx.AsyncClient(timeout=5.0)
    
    async def record_event(self, event: Dict[str, Any]):
        """发送追踪事件到统一追踪服务"""
        try:
            await self.http_client.post(
                f"{self.trace_service_url}/api/v1/trace/events",
                json=event
            )
        except Exception as e:
            logger.error(f"记录追踪事件失败: {e}")
            # 不影响主业务流程
    
    async def close(self):
        """关闭HTTP客户端"""
        await self.http_client.aclose()


# 全局追踪客户端实例
_trace_client: Optional[TraceClient] = None


def get_trace_client() -> TraceClient:
    """获取追踪客户端实例"""
    global _trace_client
    if _trace_client is None:
        import os
        trace_service_url = os.getenv("TRACE_SERVICE_URL", "http://localhost:8093")
        _trace_client = TraceClient(trace_service_url)
    return _trace_client


def set_cdp_id(cdp_id: Optional[str]):
    """设置CDP ID到上下文"""
    _cdp_id.set(cdp_id)


def get_cdp_id() -> Optional[str]:
    """从上下文获取CDP ID"""
    return _cdp_id.get()


def trace_execution(service: str, module: str = ""):
    """
    追踪装饰器
    
    Args:
        service: 服务名称
        module: 模块名称
    
    Usage:
        @trace_execution(service="clinical-parsing-service", module="concept_normalizer")
        async def normalize_concepts(self, text: str) -> Dict:
            pass
    """
    def decorator(func: Callable) -> Callable:
        @wraps(func)
        async def async_wrapper(*args, **kwargs):
            cdp_id = get_cdp_id()
            if not cdp_id:
                return await func(*args, **kwargs)
            
            trace_id = str(uuid.uuid4())
            start_time = int(time.time() * 1000)
            
            try:
                # 发送开始事件
                await get_trace_client().record_event({
                    "cdpId": cdp_id,
                    "traceId": trace_id,
                    "type": "SERVICE_CALL_START",
                    "service": service,
                    "module": module,
                    "method": func.__name__,
                    "timestamp": start_time,
                    "status": "IN_PROGRESS"
                })
                
                # 执行函数
                result = await func(*args, **kwargs)
                
                # 发送结束事件
                duration = int(time.time() * 1000) - start_time
                await get_trace_client().record_event({
                    "cdpId": cdp_id,
                    "traceId": trace_id,
                    "type": "SERVICE_CALL_END",
                    "service": service,
                    "module": module,
                    "method": func.__name__,
                    "duration": duration,
                    "status": "SUCCESS",
                    "timestamp": int(time.time() * 1000)
                })
                
                return result
                
            except Exception as e:
                # 发送错误事件
                duration = int(time.time() * 1000) - start_time
                await get_trace_client().record_event({
                    "cdpId": cdp_id,
                    "traceId": trace_id,
                    "type": "SERVICE_CALL_ERROR",
                    "service": service,
                    "module": module,
                    "method": func.__name__,
                    "duration": duration,
                    "status": "ERROR",
                    "errorMessage": str(e),
                    "timestamp": int(time.time() * 1000)
                })
                raise
        
        @wraps(func)
        def sync_wrapper(*args, **kwargs):
            # 同步函数包装器
            import asyncio
            try:
                loop = asyncio.get_event_loop()
            except RuntimeError:
                loop = asyncio.new_event_loop()
                asyncio.set_event_loop(loop)
            return loop.run_until_complete(async_wrapper(*args, **kwargs))
        
        # 判断是异步还是同步函数
        if asyncio.iscoroutinefunction(func):
            return async_wrapper
        else:
            return sync_wrapper
    
    return decorator

