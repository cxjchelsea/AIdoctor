"""Python Runtime 公开端口契约（临床中立，不复制 Shared Contracts 语义）。"""

from __future__ import annotations

from collections.abc import Mapping
from typing import Any, Optional, Protocol

from aidoctor_shared_contracts import ContractEnvelope, ToolResult

from packages.model_runtime import GatewayRequest, PreparedInvocation
from packages.model_runtime.gateway.models import OutputValidationResult

from .checkpoint import CheckpointRecord


class CheckpointPort(Protocol):
    """执行态 checkpoint 端口；不是临床 SoR，也不是 State Committer。"""

    def save(self, run_id: str, record: CheckpointRecord) -> None:
        """保存一条执行元数据。"""

    def load(self, run_id: str) -> Optional[CheckpointRecord]:
        """按 run_id 读取执行元数据；不存在则返回 None。"""


class ToolPort(Protocol):
    """工具调用端口；实现不得包装遗留 FastAPI 服务。"""

    def invoke(self, envelope: ContractEnvelope) -> ToolResult:
        """对已校验信封执行一次确定性工具调用。"""


class ModelRuntimePort(Protocol):
    """只消费 packages.model_runtime 公开 prepare/validate 边界。"""

    def prepare(self, request: GatewayRequest) -> PreparedInvocation:
        """委托 ModelGateway.prepare；不得发起 provider / 网络调用。"""

    def validate_output(
        self,
        prepared: PreparedInvocation,
        candidate: Mapping[str, Any],
    ) -> OutputValidationResult:
        """委托 ModelGateway.validate_output；候选由调用方提供。"""


class RuntimeExecutor(Protocol):
    """引擎中立的确定性执行器协议；不是 LangGraph，也不是临床工作流。"""

    def execute(self, envelope: ContractEnvelope) -> ToolResult:
        """校验信封后协调显式端口，返回合成/工具结果。"""
