"""Phase A Engineering Baseline V1 规范 Python Runtime foundation。

本包不是生产编排启用，不是临床 Agent runtime，也不是 provider 推理。
"""

from .executor import DeterministicRuntimeExecutor
from .model_runtime_port import GatewayModelRuntimePort, ModelRuntimePortError
from .protocol import (
    ProtocolValidationError,
    require_exact_contract_version,
    validate_envelope,
)

__all__ = [
    "DeterministicRuntimeExecutor",
    "GatewayModelRuntimePort",
    "ModelRuntimePortError",
    "ProtocolValidationError",
    "require_exact_contract_version",
    "validate_envelope",
]
