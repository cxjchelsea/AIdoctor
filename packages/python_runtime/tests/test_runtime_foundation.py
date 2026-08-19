"""Runtime foundation：导入、契约 EXACT、checkpoint/tool、确定性 smoke。"""

from __future__ import annotations

import inspect

import packages.python_runtime as python_runtime
from aidoctor_shared_contracts import CONTRACT_VERSION, VERSION_NEGOTIATION, ContractEnvelope, ToolContext
from packages.python_runtime import (
    DeterministicRuntimeExecutor,
    ProtocolValidationError,
    require_exact_contract_version,
    validate_envelope,
)
from packages.python_runtime.checkpoint import CheckpointRecord, InMemoryCheckpointPort
from packages.python_runtime.tools import FakeToolPort


def _synthetic_envelope(*, message_id: str = "msg-runtime-1") -> ContractEnvelope:
    """构造无患者标识的合成信封；capability 仅为工程 smoke 标记。"""

    return ContractEnvelope(
        contract_name="ToolContext",
        contract_version="1.0.0",
        message_id=message_id,
        correlation_id="corr-runtime-1",
        trace_id="trace-runtime-1",
        created_at="2026-08-18T00:00:00Z",
        producer="python-runtime-smoke",
        capability_id="engineering.synthetic.runtime_smoke",
        capability_version="0.0.1",
    )


def test_package_import_and_public_exports():
    """规范包可导入，公开面保持最小。"""

    assert python_runtime.DeterministicRuntimeExecutor is DeterministicRuntimeExecutor
    assert "InMemoryCheckpointPort" not in python_runtime.__all__
    assert "FakeToolPort" not in python_runtime.__all__


def test_shared_contract_exact_version_accepted():
    """冻结绑定版本被接受。"""

    assert CONTRACT_VERSION == "1.0.0"
    assert VERSION_NEGOTIATION == "EXACT"
    assert require_exact_contract_version("1.0.0") == "1.0.0"
    envelope = validate_envelope(_synthetic_envelope())
    assert envelope.producer == "python-runtime-smoke"
    assert envelope.capability_id == "engineering.synthetic.runtime_smoke"


def test_wrong_contract_version_fails_closed():
    """错误版本失败关闭。"""

    try:
        require_exact_contract_version("2.0.0")
    except ProtocolValidationError as exc:
        assert "1.0.0" in str(exc)
    else:
        raise AssertionError("expected ProtocolValidationError")


def test_non_envelope_fails_closed():
    """非 ContractEnvelope 输入失败关闭。"""

    try:
        validate_envelope({"contract_version": "1.0.0"})  # type: ignore[arg-type]
    except ProtocolValidationError:
        return
    raise AssertionError("expected ProtocolValidationError")


def test_inmemory_checkpoint_is_deterministic_and_execution_only():
    """内存 checkpoint 只保存执行元数据，读写确定。"""

    port = InMemoryCheckpointPort()
    record = CheckpointRecord(run_id="run-1", step_name="accepted", status="ACCEPTED")
    port.save("run-1", record)
    loaded = port.load("run-1")
    assert loaded == record
    assert port.load("missing") is None
    assert loaded is not None
    assert loaded.step_name == "accepted"


def test_fake_tool_port_is_deterministic():
    """相同合成信封产生相同逻辑 ToolResult。"""

    port = FakeToolPort()
    first = port.invoke(_synthetic_envelope())
    second = port.invoke(_synthetic_envelope())
    assert first.model_dump() == second.model_dump()
    assert first.status == "SUCCEEDED"
    assert first.suggested_patches == []
    assert first.model_dump()["output"] == [{"name": "runtime_status", "value": "ok"}]
    assert first.envelope.producer == "python-runtime-smoke"


def test_runtime_executor_deterministic_smoke():
    """确定性 smoke：无 provider、无网络、无 PHI、无临床语义。"""

    checkpoint = InMemoryCheckpointPort()
    executor = DeterministicRuntimeExecutor(checkpoint_port=checkpoint)
    envelope = _synthetic_envelope()
    first = executor.execute(envelope)
    second = executor.execute(envelope)
    assert first.model_dump() == second.model_dump()
    stored = checkpoint.load(envelope.correlation_id)
    assert stored is not None
    assert stored.status == "SUCCEEDED"
    assert stored.step_name == "completed"
    assert first.errors == []
    dumped = first.model_dump()
    assert "patient_id" not in dumped
    assert "PHI" not in str(dumped)


def test_fake_tool_port_signature_stays_envelope_only():
    """POSTFREEZE-02 ToolPort 不得被改成消费 ToolContext。"""

    parameters = list(inspect.signature(FakeToolPort.invoke).parameters)
    assert parameters == ["self", "envelope"]
    annotations = inspect.get_annotations(FakeToolPort.invoke, eval_str=True)
    assert annotations["envelope"] is ContractEnvelope
    assert "context" not in parameters


def test_default_executor_has_no_context_router():
    """默认执行器未注入路由时，ToolContext 路径必须失败关闭。"""

    executor = DeterministicRuntimeExecutor()
    try:
        executor.execute_context(
            ToolContext.model_validate(
                {
                    "contract_version": "1.0.0",
                    "envelope": _synthetic_envelope().model_dump(),
                    "actor": {"actor_id": "python-runtime-smoke", "actor_type": "SERVICE"},
                    "identifiers": {
                        "contract_version": "1.0.0",
                        "cdp_id": "synthetic-cdp-runtime-1",
                    },
                    "capability": {
                        "capability_id": "engineering.synthetic.runtime_smoke",
                        "capability_version": "0.0.1",
                    },
                    "current_state_ref": {
                        "cdp_id": "synthetic-cdp-runtime-1",
                        "version": 1,
                        "read_fields": [],
                    },
                    "authorization_scope": {"granted": [], "requested": []},
                    "deadline": "2026-08-19T00:05:00Z",
                    "locale": "und",
                    "requested_operation": "synthetic_echo",
                    "input_refs": [],
                }
            )
        )
    except Exception as exc:
        assert exc.__class__.__name__ == "ToolRoutingError"
    else:
        raise AssertionError("expected ToolRoutingError")
