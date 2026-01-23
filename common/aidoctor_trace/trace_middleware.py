"""
执行追踪中间件
用于FastAPI自动提取和设置CDP ID
"""
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response
from typing import Callable
from .trace_decorator import set_cdp_id


class TraceMiddleware(BaseHTTPMiddleware):
    """追踪中间件，自动传递CDP ID"""
    
    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        # 从请求头或请求体获取CDP ID
        cdp_id = request.headers.get("X-CDP-Id")
        
        # 如果没有从请求头获取，尝试从请求体获取（仅POST请求）
        if not cdp_id and request.method == "POST":
            try:
                body = await request.json()
                cdp_id = body.get("cdpId") or body.get("cdp_id")
            except:
                pass
        
        # 设置到上下文
        if cdp_id:
            set_cdp_id(cdp_id)
        
        try:
            response = await call_next(request)
            return response
        finally:
            # 清理上下文
            set_cdp_id(None)

