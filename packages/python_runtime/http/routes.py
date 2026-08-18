"""规范 Runtime HTTP 路由：仅 health 与合成 invoke，不是遗留服务网关。"""

from __future__ import annotations

from typing import Any, Optional

from aidoctor_shared_contracts import CONTRACT_VERSION
from fastapi import APIRouter, Body, Header, Request
from fastapi.responses import JSONResponse

from .transport import (
    HEADER_CDP_ID,
    HEADER_TRACE_ID,
    parse_contract_envelope,
    require_authorized_synthetic_operation,
    require_trace_identity,
)

runtime_router = APIRouter(prefix="/api/v1/runtime")


@runtime_router.get("/health")
def health() -> dict[str, str]:
    """CI 启动就绪探测；不声称生产、临床或仓库 Runtime 已验证。"""

    return {
        "status": "engineering_ready",
        "contract_version": CONTRACT_VERSION,
        "mode": "NON_PRODUCTION_ENGINEERING_PROTOCOL_PROOF",
    }


@runtime_router.post("/tools/invoke")
def invoke_synthetic_tool(
    request: Request,
    raw_payload: Any = Body(...),
    x_trace_id: Optional[str] = Header(default=None, alias=HEADER_TRACE_ID),
    x_cdp_id: Optional[str] = Header(default=None, alias=HEADER_CDP_ID),
) -> JSONResponse:
    """仅执行授权合成能力；经 RuntimeExecutor 端口，不直连 Model Runtime。"""

    envelope = parse_contract_envelope(raw_payload)
    require_trace_identity(x_trace_id, envelope)
    require_authorized_synthetic_operation(envelope)
    runtime_executor = request.app.state.runtime_executor
    tool_result = runtime_executor.execute(envelope)
    response_headers = {HEADER_TRACE_ID: envelope.trace_id}
    if x_cdp_id:
        # X-CDP-Id 仅作不透明合成相关元数据回显，不做患者权威解释
        response_headers[HEADER_CDP_ID] = x_cdp_id
    return JSONResponse(
        status_code=200,
        content=tool_result.model_dump(mode="json"),
        headers=response_headers,
    )
