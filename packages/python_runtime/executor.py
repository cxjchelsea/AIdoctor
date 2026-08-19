"""确定性、引擎中立的 RuntimeExecutor；不是 LangGraph / AgentLoop / 临床工作流。"""

from __future__ import annotations

from aidoctor_shared_contracts import ContractEnvelope, ToolResult

from .checkpoint import CheckpointRecord
from .ports import CheckpointPort, ToolPort
from .protocol import validate_envelope
from .tools import FakeToolPort


class DeterministicRuntimeExecutor:
    """显式协调协议校验、可选 checkpoint 与 ToolPort；无隐式重试或自主循环。"""

    def __init__(
        self,
        tool_port: ToolPort | None = None,
        checkpoint_port: CheckpointPort | None = None,
    ) -> None:
        self._tool_port = FakeToolPort() if tool_port is None else tool_port
        self._checkpoint_port = checkpoint_port

    def execute(self, envelope: ContractEnvelope) -> ToolResult:
        """校验合成信封后调用一次 ToolPort，并可选写入执行元数据。"""

        validated = validate_envelope(envelope)
        run_id = validated.correlation_id
        if self._checkpoint_port is not None:
            self._checkpoint_port.save(
                run_id,
                CheckpointRecord(run_id=run_id, step_name="accepted", status="ACCEPTED"),
            )
        result = self._tool_port.invoke(validated)
        if self._checkpoint_port is not None:
            self._checkpoint_port.save(
                run_id,
                CheckpointRecord(run_id=run_id, step_name="completed", status=result.status),
            )
        return result
