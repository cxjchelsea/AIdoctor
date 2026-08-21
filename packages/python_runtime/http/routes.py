"""规范 Runtime HTTP 路由：仅 health 与 invoke，不是遗留服务网关。"""

from __future__ import annotations

from typing import Any, Optional

from aidoctor_shared_contracts import CONTRACT_VERSION, ToolContext
from fastapi import APIRouter, Body, Header, Request
from fastapi.responses import JSONResponse

from packages.python_runtime.artifacts import ArtifactResolutionError
from packages.python_runtime.tool_router import ToolRoutingError

from .transport import (
    HEADER_CDP_ID,
    HEADER_TRACE_ID,
    TransportError,
    parse_runtime_invoke_payload,
    require_authorized_capability,
    require_canonical_contract_instance,
    require_tool_context_cross_identity,
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
def invoke_runtime_tool(
    request: Request,
    raw_payload: Any = Body(...),
    x_trace_id: Optional[str] = Header(default=None, alias=HEADER_TRACE_ID),
    x_cdp_id: Optional[str] = Header(default=None, alias=HEADER_CDP_ID),
) -> JSONResponse:
    """同一路由兼容信封与 ToolContext；经执行器端口，不直连 Model Runtime。"""

    parsed_request = parse_runtime_invoke_payload(raw_payload)
    authorized_capability_ids = request.app.state.authorized_capability_ids
    runtime_executor = request.app.state.runtime_executor
    try:
        if isinstance(parsed_request, ToolContext):
            require_trace_identity(x_trace_id, parsed_request.envelope)
            require_tool_context_cross_identity(parsed_request)
            require_canonical_contract_instance(
                "ToolContext",
                parsed_request.model_dump(mode="json"),
                correlation_id=parsed_request.envelope.correlation_id,
                trace_id=parsed_request.envelope.trace_id,
            )
            require_authorized_capability(
                parsed_request.envelope.capability_id,
                authorized_capability_ids,
                correlation_id=parsed_request.envelope.correlation_id,
                trace_id=parsed_request.envelope.trace_id,
            )
            tool_result = runtime_executor.execute_context(parsed_request)
            response_trace_id = parsed_request.envelope.trace_id
        else:
            require_trace_identity(x_trace_id, parsed_request)
            require_canonical_contract_instance(
                "ContractEnvelope",
                parsed_request.model_dump(mode="json"),
                correlation_id=parsed_request.correlation_id,
                trace_id=parsed_request.trace_id,
            )
            require_authorized_capability(
                parsed_request.capability_id,
                authorized_capability_ids,
                correlation_id=parsed_request.correlation_id,
                trace_id=parsed_request.trace_id,
            )
            tool_result = runtime_executor.execute(parsed_request)
            response_trace_id = parsed_request.trace_id
        require_canonical_contract_instance(
            "ToolResult",
            tool_result.model_dump(mode="json"),
            output=True,
            correlation_id=tool_result.envelope.correlation_id,
            trace_id=tool_result.envelope.trace_id,
        )
    except ArtifactResolutionError as exc:
        raise TransportError(
            exc.error_code,
            exc.message,
            http_status=400,
        ) from exc
    except ToolRoutingError as exc:
        raise TransportError(
            exc.error_code,
            exc.message,
            http_status=400,
        ) from exc
    response_headers = {HEADER_TRACE_ID: response_trace_id}
    if x_cdp_id:
        # X-CDP-Id 仅作不透明合成相关元数据回显，不做患者权威解释
        response_headers[HEADER_CDP_ID] = x_cdp_id
    return JSONResponse(
        status_code=200,
        content=tool_result.model_dump(mode="json"),
        headers=response_headers,
    )
