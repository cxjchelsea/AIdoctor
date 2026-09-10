"""规范 Python Runtime HTTP 应用工厂。

分类：NON_PRODUCTION_ENGINEERING_PROTOCOL_PROOF

导入本模块不得启动网络监听，不得加载凭证、provider、数据库或遗留服务。
uvicorn 入口：packages.python_runtime.http.app:app
"""

from __future__ import annotations

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from packages.python_runtime.executor import DeterministicRuntimeExecutor
from packages.python_runtime.ports import RuntimeExecutor

from .routes import runtime_router
from .transport import AUTHORIZED_SYNTHETIC_CAPABILITY_ID, TransportError

PROTOCOL_MODE = "NON_PRODUCTION_ENGINEERING_PROTOCOL_PROOF"


def create_app(
    runtime_executor: RuntimeExecutor | None = None,
    *,
    authorized_capability_ids: frozenset[str] | None = None,
) -> FastAPI:
    """构造薄 HTTP 门面；默认只授权合成 smoke，不启用 artifact_probe。"""

    application = FastAPI(
        title="Python Runtime HTTP Protocol Proof",
        version="0.0.1",
        docs_url=None,
        redoc_url=None,
        openapi_url=None,
    )
    application.state.protocol_mode = PROTOCOL_MODE
    application.state.runtime_executor = (
        runtime_executor if runtime_executor is not None else DeterministicRuntimeExecutor()
    )
    application.state.authorized_capability_ids = (
        frozenset({AUTHORIZED_SYNTHETIC_CAPABILITY_ID})
        if authorized_capability_ids is None
        else frozenset(authorized_capability_ids)
    )
    application.include_router(runtime_router)
    application.add_exception_handler(TransportError, _handle_transport_error)
    return application


def _handle_transport_error(request: Request, exc: TransportError) -> JSONResponse:
    """将传输错误归一化为 4xx 机器可读信封。"""

    del request
    response_headers = {}
    if exc.trace_id:
        response_headers["X-Trace-Id"] = exc.trace_id
    if exc.cdp_id:
        response_headers["X-CDP-Id"] = exc.cdp_id
    return JSONResponse(
        status_code=exc.http_status,
        content=exc.to_payload(),
        headers=response_headers,
    )


# 模块级应用对象供 uvicorn 加载；构造本身不绑定套接字
app = create_app()
