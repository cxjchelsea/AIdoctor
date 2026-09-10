"""确定性、引擎中立的 RuntimeExecutor；不是 LangGraph / AgentLoop / 临床工作流。"""

from __future__ import annotations

from aidoctor_shared_contracts import ContractEnvelope, ToolContext, ToolResult

from .checkpoint import CheckpointRecord
from .ports import ArtifactPort, CheckpointPort, ToolPort
from .protocol import validate_envelope
from .tool_router import ERROR_TOOL_UNREGISTERED, ToolRouter, ToolRoutingError
from .tools import FakeToolPort


class DeterministicRuntimeExecutor:
    """显式协调协议校验、可选 checkpoint 与 Tool 端口；无隐式重试或自主循环。"""

    def __init__(
        self,
        tool_port: ToolPort | None = None,
        checkpoint_port: CheckpointPort | None = None,
        *,
        tool_router: ToolRouter | None = None,
        artifact_port: ArtifactPort | None = None,
    ) -> None:
        self._tool_port = FakeToolPort() if tool_port is None else tool_port
        self._checkpoint_port = checkpoint_port
        self._tool_router = tool_router
        self._artifact_port = artifact_port

    def execute(self, envelope: ContractEnvelope) -> ToolResult:
        """信封路径：默认仍直连 ToolPort，保持 POSTFREEZE-02 逻辑结果。"""

        validated = validate_envelope(envelope)
        run_id = validated.correlation_id
        self._save_checkpoint(run_id, "accepted", "ACCEPTED")
        if self._tool_router is not None:
            result = self._tool_router.invoke_envelope(validated)
        else:
            result = self._tool_port.invoke(validated)
        self._save_checkpoint(run_id, "completed", result.status)
        return result

    def execute_context(self, context: ToolContext) -> ToolResult:
        """输入承载路径：单次路由 + 工件解析；未配置路由则失败关闭。"""

        if self._tool_router is None:
            raise ToolRoutingError(
                ERROR_TOOL_UNREGISTERED,
                "no ToolRouter configured for ToolContext invocation",
            )
        validated_envelope = validate_envelope(context.envelope)
        run_id = validated_envelope.correlation_id
        self._save_checkpoint(run_id, "accepted", "ACCEPTED")
        result = self._tool_router.invoke_context(context, self._artifact_port)
        self._save_checkpoint(run_id, "completed", result.status)
        return result

    def _save_checkpoint(self, run_id: str, step_name: str, status: str) -> None:
        """只写执行元数据，不得写入工件字节或源内容。"""

        if self._checkpoint_port is None:
            return
        self._checkpoint_port.save(
            run_id,
            CheckpointRecord(run_id=run_id, step_name=step_name, status=status),
        )
