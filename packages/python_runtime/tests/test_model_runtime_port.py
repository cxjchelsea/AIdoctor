"""证明 Python Runtime 只消费现有 Model Runtime 公开 prepare/validate 边界。"""

from __future__ import annotations

import pytest

from packages.model_runtime import GatewayRequest
from packages.model_runtime.tests.conftest_p3 import build_gateway, gateway_request
from packages.python_runtime import GatewayModelRuntimePort, ModelRuntimePortError
from packages.python_runtime.tools import FakeToolPort
from packages.python_runtime.tests.test_runtime_foundation import _synthetic_envelope


def test_adapter_rejects_non_gateway():
    """非 ModelGateway 实例失败关闭。"""

    with pytest.raises(ModelRuntimePortError):
        GatewayModelRuntimePort(object())  # type: ignore[arg-type]


def test_prepare_consumes_public_gateway_without_provider():
    """prepare 走公开 ModelGateway；不声称发生真实推理。"""

    adapter = GatewayModelRuntimePort(build_gateway())
    prepared = adapter.prepare(gateway_request())
    assert prepared.request_id == "req-classify-1"
    assert prepared.output_contract_id == "tool-result"
    assert prepared.rendered_prompt.prompt_id == "classify-color"


def test_invalid_prepare_request_fails_explicitly():
    """非法请求显式失败。"""

    adapter = GatewayModelRuntimePort(build_gateway())
    with pytest.raises(ModelRuntimePortError):
        adapter.prepare(object())  # type: ignore[arg-type]
    with pytest.raises(ModelRuntimePortError):
        adapter.prepare(
            GatewayRequest(
                **{
                    **gateway_request().model_dump(),
                    "route_id": "unknown-route",
                }
            )
        )


def test_validate_output_accepts_synthetic_tool_result():
    """调用方提供的合成 ToolResult 可走 validate_output；不是推理成功声明。"""

    adapter = GatewayModelRuntimePort(build_gateway())
    prepared = adapter.prepare(gateway_request())
    candidate = FakeToolPort().invoke(_synthetic_envelope()).model_dump(mode="json")
    result = adapter.validate_output(prepared, candidate)
    assert result.contract_id == "tool-result"
    assert result.valid is True


def test_validate_output_rejects_invalid_candidate():
    """非法候选显式失败或判定 invalid。"""

    adapter = GatewayModelRuntimePort(build_gateway())
    prepared = adapter.prepare(gateway_request())
    with pytest.raises(ModelRuntimePortError):
        adapter.validate_output(prepared, "not-a-mapping")  # type: ignore[arg-type]
    invalid = FakeToolPort().invoke(_synthetic_envelope()).model_dump(mode="json")
    del invalid["status"]
    result = adapter.validate_output(prepared, invalid)
    assert result.valid is False
