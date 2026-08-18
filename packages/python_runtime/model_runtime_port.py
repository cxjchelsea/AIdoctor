"""consume-only Model Runtime 适配器：只调用公开 prepare / validate_output。"""

from __future__ import annotations

from collections.abc import Mapping
from typing import Any

from packages.model_runtime import GatewayRequest, ModelGateway, PreparedInvocation
from packages.model_runtime.gateway.models import OutputValidationResult


class ModelRuntimePortError(RuntimeError):
    """Model Runtime 交互失败关闭；不表示发生了真实推理。"""


class GatewayModelRuntimePort:
    """把现有 ModelGateway 公开边界适配为 ModelRuntimePort。"""

    def __init__(self, gateway: ModelGateway) -> None:
        if not isinstance(gateway, ModelGateway):
            raise ModelRuntimePortError("gateway must be packages.model_runtime.ModelGateway")
        self._gateway = gateway

    def prepare(self, request: GatewayRequest) -> PreparedInvocation:
        """只调用 ModelGateway.prepare；不加载凭证，不发起网络。"""

        if not isinstance(request, GatewayRequest):
            raise ModelRuntimePortError("request must be packages.model_runtime.GatewayRequest")
        try:
            return self._gateway.prepare(request)
        except Exception as exc:
            raise ModelRuntimePortError(f"ModelGateway.prepare failed: {exc}") from exc

    def validate_output(
        self,
        prepared: PreparedInvocation,
        candidate: Mapping[str, Any],
    ) -> OutputValidationResult:
        """只调用 ModelGateway.validate_output；候选由调用方提供。"""

        if not isinstance(prepared, PreparedInvocation):
            raise ModelRuntimePortError("prepared must be packages.model_runtime.PreparedInvocation")
        if not isinstance(candidate, Mapping):
            raise ModelRuntimePortError("candidate must be a mapping")
        try:
            return self._gateway.validate_output(prepared, candidate)
        except Exception as exc:
            raise ModelRuntimePortError(f"ModelGateway.validate_output failed: {exc}") from exc
