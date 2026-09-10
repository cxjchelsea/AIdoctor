"""POSTFREEZE-03B-A：真实工具输入 / 工件解析基础的确定性测试。

本文件只证明结构绑定与静态允许名单解析，不声称完整 Shared Contracts 语义校验器，
也不启用 RAW OCR / PHI / 临床 Runtime。
"""

from __future__ import annotations

import hashlib
import inspect
import json
from pathlib import Path

from aidoctor_shared_contracts import ContractEnvelope, SourceArtifact, ToolContext, ToolResult
from fastapi.testclient import TestClient

from packages.python_runtime.artifacts import (
    ERROR_ARTIFACT_CHECKSUM_MISMATCH,
    ERROR_ARTIFACT_ID_MISMATCH,
    ERROR_ARTIFACT_PHI_REJECTED,
    ERROR_ARTIFACT_SIZE_MISMATCH,
    ERROR_ARTIFACT_UNKNOWN,
    ERROR_ARTIFACT_VERSION_MISMATCH,
    SYNTHETIC_ARTIFACT_PROBE_BYTES,
    SYNTHETIC_ARTIFACT_PROBE_ID,
    SYNTHETIC_ARTIFACT_PROBE_STORAGE_REF,
    SYNTHETIC_ARTIFACT_PROBE_VERSION,
    ArtifactResolutionError,
    StaticAllowlistArtifactPort,
    StaticArtifactRecord,
    build_synthetic_probe_metadata,
    sha256_hex,
)
from packages.python_runtime.executor import DeterministicRuntimeExecutor
from packages.python_runtime.http.app import create_app
from packages.python_runtime.http.transport import (
    AUTHORIZED_SYNTHETIC_CAPABILITY_ID,
    ERROR_CONTRACT_VERSION_MISMATCH,
    ERROR_IDENTITY_MISMATCH,
    ERROR_OPERATION_NOT_AUTHORIZED,
    ERROR_TOOL_CONTEXT_INVALID,
    parse_runtime_invoke_payload,
)
from packages.python_runtime.tool_router import (
    ARTIFACT_PROBE_CAPABILITY_ID,
    ARTIFACT_PROBE_CAPABILITY_VERSION,
    ERROR_INPUT_REF_UNSUPPORTED,
    SyntheticArtifactProbeTool,
    ToolRouter,
)
from packages.python_runtime.tools import FakeToolPort

_FIXTURE_PATH = Path(__file__).resolve().parent / "fixtures" / "runtime_protocol_invoke.json"


def _load_golden_payload() -> dict:
    """加载未改动的 POSTFREEZE-02 合成信封夹具。"""

    return json.loads(_FIXTURE_PATH.read_text(encoding="utf-8"))


def _artifact_probe_context_payload() -> dict:
    """构造合成 ToolContext；标识全部不透明，不含患者或临床文本。"""

    return {
        "contract_version": "1.0.0",
        "envelope": {
            "contract_name": "ToolContext",
            "contract_version": "1.0.0",
            "message_id": "msg-artifact-probe-1",
            "correlation_id": "corr-artifact-probe-1",
            "trace_id": "trace-artifact-probe-1",
            "created_at": "2026-08-19T00:00:00Z",
            "producer": "python-runtime-artifact-probe",
            "capability_id": ARTIFACT_PROBE_CAPABILITY_ID,
            "capability_version": ARTIFACT_PROBE_CAPABILITY_VERSION,
        },
        "actor": {
            "actor_id": "python-runtime-artifact-probe",
            "actor_type": "SERVICE",
        },
        "identifiers": {
            "contract_version": "1.0.0",
            "cdp_id": "synthetic-cdp-artifact-probe-1",
        },
        "capability": {
            "capability_id": ARTIFACT_PROBE_CAPABILITY_ID,
            "capability_version": ARTIFACT_PROBE_CAPABILITY_VERSION,
        },
        "current_state_ref": {
            "cdp_id": "synthetic-cdp-artifact-probe-1",
            "version": 1,
            "read_fields": [],
        },
        "authorization_scope": {
            "granted": [],
            "requested": [],
        },
        "deadline": "2026-08-19T00:05:00Z",
        "locale": "und",
        "requested_operation": "PROBE_ARTIFACT_INPUT",
        "input_refs": [
            {
                "ref_type": "ARTIFACT",
                "ref_id": SYNTHETIC_ARTIFACT_PROBE_ID,
                "ref_version": SYNTHETIC_ARTIFACT_PROBE_VERSION,
            }
        ],
    }


def _artifact_probe_app():
    """仅测试注入：授权 artifact_probe，并装配静态工件解析器。"""

    router = ToolRouter()
    router.register_envelope_tool(AUTHORIZED_SYNTHETIC_CAPABILITY_ID, FakeToolPort())
    router.register_context_tool(
        ARTIFACT_PROBE_CAPABILITY_ID,
        ARTIFACT_PROBE_CAPABILITY_VERSION,
        SyntheticArtifactProbeTool(),
    )
    executor = DeterministicRuntimeExecutor(
        tool_router=router,
        artifact_port=StaticAllowlistArtifactPort.for_synthetic_probe(),
    )
    return create_app(
        runtime_executor=executor,
        authorized_capability_ids=frozenset(
            {AUTHORIZED_SYNTHETIC_CAPABILITY_ID, ARTIFACT_PROBE_CAPABILITY_ID}
        ),
    )


def _probe_client() -> TestClient:
    """输入承载路径的测试客户端；不是默认模块级 app。"""

    return TestClient(_artifact_probe_app())


def _default_client() -> TestClient:
    """默认 create_app()：只授权 runtime_smoke。"""

    return TestClient(create_app())


def _output_map(tool_result: ToolResult) -> dict[str, object]:
    """把 NamedValue 列表收成字典，便于确定性断言。"""

    return {item.name: item.value for item in tool_result.output}


def test_golden_fixture_file_is_unchanged_envelope_shape():
    """既有夹具不得被改写，且顶层没有 envelope 对象。"""

    payload = _load_golden_payload()
    assert "envelope" not in payload
    assert payload["contract_name"] == "ToolContext"
    assert payload["capability_id"] == AUTHORIZED_SYNTHETIC_CAPABILITY_ID


def test_golden_fixture_still_parses_as_contract_envelope():
    """contract_name=ToolContext 的信封夹具必须仍按 ContractEnvelope 解析。"""

    parsed = parse_runtime_invoke_payload(_load_golden_payload())
    assert isinstance(parsed, ContractEnvelope)
    assert not isinstance(parsed, ToolContext)
    assert parsed.contract_name == "ToolContext"


def test_golden_fixture_still_invokes_synthetic_echo():
    """默认应用上的金色夹具仍走 synthetic-echo，逻辑结果不变。"""

    payload = _load_golden_payload()
    response = _default_client().post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["trace_id"]},
    )
    assert response.status_code == 200
    tool_result = ToolResult.model_validate(response.json())
    assert tool_result.tool_name == "synthetic-echo"
    assert tool_result.status == "SUCCEEDED"
    assert tool_result.reason_code == "SYNTHETIC_RUNTIME_OK"
    assert tool_result.model_dump()["output"] == [{"name": "runtime_status", "value": "ok"}]
    assert tool_result.suggested_patches == []
    assert response.headers.get("x-trace-id") == payload["trace_id"]


def test_top_level_envelope_object_is_tool_context_candidate():
    """顶层含 envelope 对象时按 ToolContext 候选解析，而不是看 contract_name。"""

    parsed = parse_runtime_invoke_payload(_artifact_probe_context_payload())
    assert isinstance(parsed, ToolContext)
    assert parsed.envelope.contract_name == "ToolContext"
    assert parsed.input_refs[0].ref_type == "ARTIFACT"


def test_tool_context_binds_shared_contract_model():
    """完整 ToolContext 必须走公开 Shared Contracts 绑定，而不是 Runtime 本地 DTO。"""

    context = ToolContext.model_validate(_artifact_probe_context_payload())
    assert context.capability.capability_id == context.envelope.capability_id
    assert context.capability.capability_version == context.envelope.capability_version
    assert context.input_refs[0].ref_type == "ARTIFACT"
    assert context.input_refs[0].ref_id == SYNTHETIC_ARTIFACT_PROBE_ID
    assert context.requested_operation == "PROBE_ARTIFACT_INPUT"
    assert context.requested_operation != "probe_artifact_input"


def test_injected_tool_context_artifact_probe_succeeds_without_returning_bytes():
    """注入组合下，ARTIFACT InputRef 对输入承载路径可见，且 HTTP 不回传字节。"""

    payload = _artifact_probe_context_payload()
    response = _probe_client().post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 200
    tool_result = ToolResult.model_validate(response.json())
    assert tool_result.tool_name == "synthetic-artifact-probe"
    assert tool_result.status == "SUCCEEDED"
    output_map = _output_map(tool_result)
    assert output_map["artifact_id"] == SYNTHETIC_ARTIFACT_PROBE_ID
    assert output_map["artifact_version"] == SYNTHETIC_ARTIFACT_PROBE_VERSION
    assert output_map["size_bytes"] == len(SYNTHETIC_ARTIFACT_PROBE_BYTES)
    assert output_map["checksum_sha256"] == sha256_hex(SYNTHETIC_ARTIFACT_PROBE_BYTES)
    assert output_map["sensitivity"] == "INTERNAL"
    assert output_map["bytes_resolved"] is True
    serialized = json.dumps(response.json())
    assert "content" not in response.json()
    assert SYNTHETIC_ARTIFACT_PROBE_BYTES.hex() not in serialized
    assert "OPAQUE" not in serialized
    assert "patient" not in serialized.lower()
    assert "PHI" not in serialized


def test_static_artifact_port_resolves_exact_bytes_and_metadata():
    """已知合成工件必须解析到精确字节，并校验校验和、大小、标识与非 PHI。"""

    resolver = StaticAllowlistArtifactPort.for_synthetic_probe()
    resolved = resolver.resolve(SYNTHETIC_ARTIFACT_PROBE_ID, SYNTHETIC_ARTIFACT_PROBE_VERSION)
    assert resolved.content == SYNTHETIC_ARTIFACT_PROBE_BYTES
    assert resolved.metadata.artifact_id == SYNTHETIC_ARTIFACT_PROBE_ID
    assert resolved.metadata.size_bytes == len(SYNTHETIC_ARTIFACT_PROBE_BYTES)
    assert resolved.metadata.checksum.algorithm == "SHA-256"
    assert resolved.metadata.checksum.value == hashlib.sha256(SYNTHETIC_ARTIFACT_PROBE_BYTES).hexdigest()
    assert resolved.metadata.sensitivity != "PHI"
    assert resolved.metadata.sensitivity == "INTERNAL"
    assert isinstance(resolved.metadata, SourceArtifact)


def test_unknown_artifact_ref_is_rejected():
    """未知引用失败关闭。"""

    resolver = StaticAllowlistArtifactPort.for_synthetic_probe()
    try:
        resolver.resolve("artifact-synthetic-unknown-9", 1)
    except ArtifactResolutionError as exc:
        assert exc.error_code == ERROR_ARTIFACT_UNKNOWN
    else:
        raise AssertionError("expected ArtifactResolutionError")


def test_wrong_artifact_version_is_rejected():
    """错误版本失败关闭。"""

    resolver = StaticAllowlistArtifactPort.for_synthetic_probe()
    try:
        resolver.resolve(SYNTHETIC_ARTIFACT_PROBE_ID, 2)
    except ArtifactResolutionError as exc:
        assert exc.error_code == ERROR_ARTIFACT_VERSION_MISMATCH
    else:
        raise AssertionError("expected ArtifactResolutionError")


def test_checksum_mismatch_is_rejected():
    """允许名单记录的校验和与实字节不一致时失败关闭。"""

    metadata = build_synthetic_probe_metadata().model_copy(
        update={"checksum": {"algorithm": "SHA-256", "value": "0" * 64}}
    )
    resolver = StaticAllowlistArtifactPort(
        {
            (SYNTHETIC_ARTIFACT_PROBE_ID, SYNTHETIC_ARTIFACT_PROBE_VERSION): StaticArtifactRecord(
                metadata=metadata,
                content=SYNTHETIC_ARTIFACT_PROBE_BYTES,
            )
        }
    )
    try:
        resolver.resolve(SYNTHETIC_ARTIFACT_PROBE_ID, SYNTHETIC_ARTIFACT_PROBE_VERSION)
    except ArtifactResolutionError as exc:
        assert exc.error_code == ERROR_ARTIFACT_CHECKSUM_MISMATCH
    else:
        raise AssertionError("expected ArtifactResolutionError")


def test_size_mismatch_is_rejected():
    """声明大小与实字节长度不一致时失败关闭。"""

    metadata = build_synthetic_probe_metadata().model_copy(update={"size_bytes": 999})
    resolver = StaticAllowlistArtifactPort(
        {
            (SYNTHETIC_ARTIFACT_PROBE_ID, SYNTHETIC_ARTIFACT_PROBE_VERSION): StaticArtifactRecord(
                metadata=metadata,
                content=SYNTHETIC_ARTIFACT_PROBE_BYTES,
            )
        }
    )
    try:
        resolver.resolve(SYNTHETIC_ARTIFACT_PROBE_ID, SYNTHETIC_ARTIFACT_PROBE_VERSION)
    except ArtifactResolutionError as exc:
        assert exc.error_code == ERROR_ARTIFACT_SIZE_MISMATCH
    else:
        raise AssertionError("expected ArtifactResolutionError")


def test_phi_sensitivity_is_rejected():
    """本批拒绝 PHI 敏感度，即使字节本身仍是合成不透明内容。"""

    metadata = build_synthetic_probe_metadata().model_copy(update={"sensitivity": "PHI"})
    resolver = StaticAllowlistArtifactPort(
        {
            (SYNTHETIC_ARTIFACT_PROBE_ID, SYNTHETIC_ARTIFACT_PROBE_VERSION): StaticArtifactRecord(
                metadata=metadata,
                content=SYNTHETIC_ARTIFACT_PROBE_BYTES,
            )
        }
    )
    try:
        resolver.resolve(SYNTHETIC_ARTIFACT_PROBE_ID, SYNTHETIC_ARTIFACT_PROBE_VERSION)
    except ArtifactResolutionError as exc:
        assert exc.error_code == ERROR_ARTIFACT_PHI_REJECTED
    else:
        raise AssertionError("expected ArtifactResolutionError")


def test_artifact_id_inconsistency_is_rejected():
    """记录 artifact_id 与请求 ref_id 不一致时失败关闭。"""

    metadata = build_synthetic_probe_metadata().model_copy(
        update={"artifact_id": "artifact-synthetic-other-1"}
    )
    resolver = StaticAllowlistArtifactPort(
        {
            (SYNTHETIC_ARTIFACT_PROBE_ID, SYNTHETIC_ARTIFACT_PROBE_VERSION): StaticArtifactRecord(
                metadata=metadata,
                content=SYNTHETIC_ARTIFACT_PROBE_BYTES,
            )
        }
    )
    try:
        resolver.resolve(SYNTHETIC_ARTIFACT_PROBE_ID, SYNTHETIC_ARTIFACT_PROBE_VERSION)
    except ArtifactResolutionError as exc:
        assert exc.error_code == ERROR_ARTIFACT_ID_MISMATCH
    else:
        raise AssertionError("expected ArtifactResolutionError")


def test_http_unknown_artifact_ref_fails_closed():
    """HTTP 未知工件引用失败关闭。"""

    payload = _artifact_probe_context_payload()
    payload["input_refs"][0]["ref_id"] = "artifact-synthetic-unknown-9"
    response = _probe_client().post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 400
    assert response.json()["error_code"] == ERROR_ARTIFACT_UNKNOWN


def test_http_wrong_artifact_version_fails_closed():
    """HTTP 错误工件版本失败关闭。"""

    payload = _artifact_probe_context_payload()
    payload["input_refs"][0]["ref_version"] = 2
    response = _probe_client().post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 400
    assert response.json()["error_code"] == ERROR_ARTIFACT_VERSION_MISMATCH


def test_missing_artifact_input_ref_rejected_for_artifact_probe():
    """artifact_probe 在缺少 ARTIFACT 输入时失败关闭。"""

    payload = _artifact_probe_context_payload()
    payload["input_refs"] = []
    response = _probe_client().post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 400
    assert response.json()["error_code"] == ERROR_INPUT_REF_UNSUPPORTED


def test_non_artifact_input_ref_rejected_for_artifact_probe():
    """artifact_probe 拒绝非 ARTIFACT 输入。"""

    payload = _artifact_probe_context_payload()
    payload["input_refs"] = [
        {
            "ref_type": "EVIDENCE_PACK",
            "ref_id": "evidence-synthetic-1",
            "ref_version": 1,
        }
    ]
    response = _probe_client().post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 400
    assert response.json()["error_code"] == ERROR_INPUT_REF_UNSUPPORTED


def test_capability_identity_mismatch_fails_closed():
    """context.capability 与嵌套信封能力标识必须一致。"""

    payload = _artifact_probe_context_payload()
    payload["capability"]["capability_id"] = AUTHORIZED_SYNTHETIC_CAPABILITY_ID
    response = _probe_client().post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 400
    assert response.json()["error_code"] == ERROR_IDENTITY_MISMATCH


def test_capability_version_mismatch_fails_closed():
    """context.capability 与嵌套信封能力版本必须一致。"""

    payload = _artifact_probe_context_payload()
    payload["capability"]["capability_version"] = "9.9.9"
    response = _probe_client().post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 400
    assert response.json()["error_code"] == ERROR_IDENTITY_MISMATCH


def test_nested_envelope_contract_name_must_be_tool_context():
    """嵌套 envelope.contract_name 必须是 ToolContext。"""

    payload = _artifact_probe_context_payload()
    payload["envelope"]["contract_name"] = "ToolResult"
    response = _probe_client().post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 400
    assert response.json()["error_code"] == ERROR_TOOL_CONTEXT_INVALID


def test_tool_context_version_mismatch_fails_closed():
    """ToolContext 顶层版本不匹配失败关闭。"""

    payload = _artifact_probe_context_payload()
    payload["contract_version"] = "2.0.0"
    response = _probe_client().post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 400
    assert response.json()["error_code"] == ERROR_CONTRACT_VERSION_MISMATCH


def test_invalid_tool_context_structure_fails_closed():
    """残缺 ToolContext 结构失败关闭。"""

    payload = _artifact_probe_context_payload()
    del payload["actor"]
    response = _probe_client().post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 400
    assert response.json()["error_code"] == ERROR_TOOL_CONTEXT_INVALID


def test_missing_trace_header_on_tool_context_fails_closed():
    """ToolContext 路径同样要求 X-Trace-Id。"""

    payload = _artifact_probe_context_payload()
    response = _probe_client().post("/api/v1/runtime/tools/invoke", json=payload)
    assert response.status_code == 400
    assert response.json()["error_code"] == ERROR_IDENTITY_MISMATCH


def test_mismatched_trace_header_on_tool_context_fails_closed():
    """X-Trace-Id 必须等于嵌套 envelope.trace_id。"""

    payload = _artifact_probe_context_payload()
    response = _probe_client().post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": "trace-other-artifact-1"},
    )
    assert response.status_code == 400
    assert response.json()["error_code"] == ERROR_IDENTITY_MISMATCH


def test_unknown_capability_on_injected_app_fails_closed():
    """即使测试注入组合，未知能力仍 403。"""

    payload = _artifact_probe_context_payload()
    payload["envelope"]["capability_id"] = "engineering.synthetic.unauthorized"
    payload["capability"]["capability_id"] = "engineering.synthetic.unauthorized"
    response = _probe_client().post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 403
    assert response.json()["error_code"] == ERROR_OPERATION_NOT_AUTHORIZED


def test_default_create_app_does_not_authorize_artifact_probe():
    """默认 create_app() 不得授权 artifact_probe。"""

    application = create_app()
    assert application.state.authorized_capability_ids == frozenset(
        {AUTHORIZED_SYNTHETIC_CAPABILITY_ID}
    )
    assert ARTIFACT_PROBE_CAPABILITY_ID not in application.state.authorized_capability_ids
    payload = _artifact_probe_context_payload()
    response = _default_client().post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 403
    assert response.json()["error_code"] == ERROR_OPERATION_NOT_AUTHORIZED


def test_tool_port_remains_envelope_only_and_context_port_is_distinct():
    """既有 ToolPort 仍只消费信封；输入承载协议必须是独立端口。"""

    envelope_parameters = list(inspect.signature(FakeToolPort.invoke).parameters)
    assert envelope_parameters == ["self", "envelope"]
    envelope_annotations = inspect.get_annotations(FakeToolPort.invoke, eval_str=True)
    assert envelope_annotations["envelope"] is ContractEnvelope

    context_parameters = list(inspect.signature(SyntheticArtifactProbeTool.invoke).parameters)
    assert context_parameters == ["self", "context", "artifacts"]
    assert "envelope" not in context_parameters
    context_annotations = inspect.get_annotations(SyntheticArtifactProbeTool.invoke, eval_str=True)
    assert context_annotations["context"] is ToolContext


def test_resolver_does_not_treat_storage_ref_or_filename_as_path():
    """storage_ref 是冻结 v1 允许的不透明逻辑引用，不得当文件系统路径打开。"""

    metadata = build_synthetic_probe_metadata()
    assert not metadata.storage_ref.startswith("logical://")
    assert metadata.storage_ref.startswith("artifact://")
    assert metadata.storage_ref == SYNTHETIC_ARTIFACT_PROBE_STORAGE_REF
    assert metadata.storage_ref == "artifact://engineering-synthetic/artifact-synthetic-probe-1"
    assert not metadata.storage_ref.startswith("/")
    assert not metadata.storage_ref.startswith("\\")
    assert "\\" not in metadata.storage_ref
    assert ".." not in metadata.storage_ref
    assert "/" not in metadata.original_filename
    assert "\\" not in metadata.original_filename
