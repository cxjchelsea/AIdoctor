"""POSTFREEZE-03C WP-1：Runtime HTTP 边界复用 canonical Shared Contracts 语义校验器。

权威路径：HTTP POST → Pydantic → canonical schema/version/semantic_errors
→ 授权 → executor → ToolResult canonical validation → HTTP 响应。
不复制 semantic_errors 规则，不调用 validate_package()，不改变默认授权面。
"""

from __future__ import annotations

import ast
import json
from pathlib import Path

from aidoctor_shared_contracts import ToolResult
from fastapi.testclient import TestClient

from packages.python_runtime.artifacts import (
    StaticAllowlistArtifactPort,
    SYNTHETIC_ARTIFACT_PROBE_ID,
    SYNTHETIC_ARTIFACT_PROBE_VERSION,
)
from packages.python_runtime.executor import DeterministicRuntimeExecutor
from packages.python_runtime.http.app import create_app
from packages.python_runtime.http.contract_validation import (
    _canonical_validator_module,
    validate_runtime_contract_instance,
)
from packages.python_runtime.http.engineering_raw_ocr import (
    ENGINEERING_RAW_OCR_CAPABILITY_ID,
    ENGINEERING_RAW_OCR_CAPABILITY_VERSION,
    create_engineering_raw_ocr_app,
)
from packages.python_runtime.http.transport import (
    AUTHORIZED_SYNTHETIC_CAPABILITY_ID,
    ERROR_CONTRACT_VERSION_MISMATCH,
    ERROR_OPERATION_NOT_AUTHORIZED,
    ERROR_RUNTIME_CONTRACT_OUTPUT_INVALID,
    ERROR_RUNTIME_CONTRACT_SEMANTIC_INVALID,
)
from packages.python_runtime.raw_ocr_adapter import RawOcrToolAdapter
from packages.python_runtime.tool_router import ToolRouter
from packages.python_runtime.tools import FakeToolPort

_REPO_ROOT = Path(__file__).resolve().parents[3]
_FIXTURE_PATH = Path(__file__).resolve().parent / "fixtures" / "runtime_protocol_invoke.json"
_RUNTIME_HTTP = _REPO_ROOT / "packages" / "python_runtime" / "http"
_FORBIDDEN_SEMANTIC_LITERALS = (
    "requested scopes cannot exceed granted scopes",
    "deadline must not precede envelope.created_at",
    "completed_at must not precede started_at",
)


class _DeterministicHelloEngine:
    """注入式合成引擎；不导入 OCR 库。"""

    def recognize_from_bytes(self, image_bytes: bytes) -> str:
        del image_bytes
        return "HELLO OCR 123"


class _SmokeContextTool:
    """把信封 FakeToolPort 接到 ToolContext 路径，仅用于语义校验正向证明。"""

    def invoke(self, context, artifacts):
        del artifacts
        return FakeToolPort().invoke(context.envelope)


class _InvalidTimingTool:
    """故意产出语义非法 ToolResult，证明输出校验失败关闭。"""

    def invoke(self, envelope):
        valid = FakeToolPort().invoke(envelope)
        return valid.model_copy(
            update={
                "started_at": "2026-08-18T00:00:02Z",
                "completed_at": "2026-08-18T00:00:01Z",
            }
        )


def _load_golden_payload() -> dict:
    return json.loads(_FIXTURE_PATH.read_text(encoding="utf-8"))


def _tool_context_payload(*, granted=None, requested=None, created_at=None, deadline=None) -> dict:
    return {
        "contract_version": "1.0.0",
        "envelope": {
            "contract_name": "ToolContext",
            "contract_version": "1.0.0",
            "message_id": "msg-03c-semantic-1",
            "correlation_id": "corr-03c-semantic-1",
            "trace_id": "trace-03c-semantic-1",
            "created_at": created_at or "2026-08-20T09:00:00Z",
            "producer": "python-runtime-03c-semantic",
            "capability_id": AUTHORIZED_SYNTHETIC_CAPABILITY_ID,
            "capability_version": "0.0.1",
        },
        "actor": {"actor_id": "python-runtime-03c-semantic", "actor_type": "SERVICE"},
        "identifiers": {
            "contract_version": "1.0.0",
            "cdp_id": "synthetic-cdp-03c-semantic-1",
        },
        "capability": {
            "capability_id": AUTHORIZED_SYNTHETIC_CAPABILITY_ID,
            "capability_version": "0.0.1",
        },
        "current_state_ref": {
            "cdp_id": "synthetic-cdp-03c-semantic-1",
            "version": 1,
            "read_fields": [],
        },
        "authorization_scope": {
            "granted": granted if granted is not None else [],
            "requested": requested if requested is not None else [],
        },
        "deadline": deadline or "2026-08-20T09:05:00Z",
        "locale": "und",
        "requested_operation": "SYNTHETIC_RUNTIME_SMOKE",
        "input_refs": [],
    }


def _raw_ocr_context_payload() -> dict:
    payload = _tool_context_payload()
    payload["envelope"]["capability_id"] = ENGINEERING_RAW_OCR_CAPABILITY_ID
    payload["envelope"]["capability_version"] = ENGINEERING_RAW_OCR_CAPABILITY_VERSION
    payload["capability"]["capability_id"] = ENGINEERING_RAW_OCR_CAPABILITY_ID
    payload["capability"]["capability_version"] = ENGINEERING_RAW_OCR_CAPABILITY_VERSION
    payload["requested_operation"] = "RAW_OCR_RECOGNIZE"
    payload["input_refs"] = [
        {
            "ref_type": "ARTIFACT",
            "ref_id": SYNTHETIC_ARTIFACT_PROBE_ID,
            "ref_version": SYNTHETIC_ARTIFACT_PROBE_VERSION,
        }
    ]
    return payload


def _context_app():
    router = ToolRouter()
    router.register_context_tool(
        AUTHORIZED_SYNTHETIC_CAPABILITY_ID,
        "0.0.1",
        _SmokeContextTool(),
        require_artifact=False,
        artifact_only=True,
    )
    return create_app(
        runtime_executor=DeterministicRuntimeExecutor(
            tool_router=router,
            artifact_port=StaticAllowlistArtifactPort.for_synthetic_probe(),
        ),
        authorized_capability_ids=frozenset({AUTHORIZED_SYNTHETIC_CAPABILITY_ID}),
    )


def test_canonical_validator_is_single_source_and_not_package_oracle() -> None:
    module = _canonical_validator_module()
    source = Path(module.__file__).read_text(encoding="utf-8")
    tree = ast.parse(source)
    function_names = {
        node.name for node in tree.body if isinstance(node, ast.FunctionDef)
    }
    assert "validate_contract_instance" in function_names
    assert "semantic_errors" in function_names
    assert "validate_package" in function_names
    validate_fn = [
        node
        for node in tree.body
        if isinstance(node, ast.FunctionDef) and node.name == "validate_contract_instance"
    ][0]
    called = [
        child.func.id
        for child in ast.walk(validate_fn)
        if isinstance(child, ast.Call) and isinstance(child.func, ast.Name)
    ]
    assert "validate_package" not in called
    assert "semantic_errors" in called
    print("RUNTIME_FULL_SEMANTIC_VALIDATOR_SINGLE_SOURCE_OF_TRUTH=YES", flush=True)
    print("RUNTIME_FULL_SEMANTIC_VALIDATOR_SCHEMA_CHANGE=NO", flush=True)


def test_schema_invalid_instance_fails_closed() -> None:
    errors = validate_runtime_contract_instance(
        "ToolContext",
        {"contract_version": "1.0.0"},
    )
    assert errors
    assert any(item.startswith("schema:") for item in errors)
    print("RUNTIME_FULL_SEMANTIC_VALIDATOR_REQUEST_FAIL_CLOSED=YES", flush=True)


def test_valid_toolcontext_succeeds_over_http() -> None:
    payload = _tool_context_payload()
    response = TestClient(_context_app()).post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 200
    tool_result = ToolResult.model_validate(response.json())
    assert tool_result.status == "SUCCEEDED"
    output_errors = validate_runtime_contract_instance(
        "ToolResult",
        tool_result.model_dump(mode="json"),
    )
    assert output_errors == []
    print("RUNTIME_FULL_SEMANTIC_VALIDATOR_TOOLCONTEXT_VALID=YES", flush=True)
    print("RUNTIME_FULL_SEMANTIC_VALIDATOR_TOOLRESULT_VALID=YES", flush=True)


def test_requested_scope_not_subset_fails_closed_over_http() -> None:
    payload = _tool_context_payload(granted=[], requested=["CDP_READ"])
    response = TestClient(_context_app()).post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 400
    assert response.json()["error_code"] == ERROR_RUNTIME_CONTRACT_SEMANTIC_INVALID
    print("RUNTIME_FULL_SEMANTIC_VALIDATOR_REQUEST_FAIL_CLOSED=YES", flush=True)


def test_deadline_before_created_fails_closed_over_http() -> None:
    payload = _tool_context_payload(
        created_at="2026-08-20T09:05:00Z",
        deadline="2026-08-20T09:00:00Z",
    )
    response = TestClient(_context_app()).post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 400
    assert response.json()["error_code"] == ERROR_RUNTIME_CONTRACT_SEMANTIC_INVALID


def test_exact_version_failure_remains_fail_closed() -> None:
    payload = _load_golden_payload()
    payload["contract_version"] = "2.0.0"
    response = TestClient(create_app()).post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["trace_id"]},
    )
    assert response.status_code == 400
    assert response.json()["error_code"] == ERROR_CONTRACT_VERSION_MISMATCH


def test_default_synthetic_success_path_remains_valid() -> None:
    payload = _load_golden_payload()
    response = TestClient(create_app()).post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["trace_id"]},
    )
    assert response.status_code == 200
    tool_result = ToolResult.model_validate(response.json())
    assert tool_result.tool_name == "synthetic-echo"
    assert (
        validate_runtime_contract_instance("ToolResult", tool_result.model_dump(mode="json"))
        == []
    )
    print("RUNTIME_FULL_SEMANTIC_VALIDATOR_PROTOCOL=PASS", flush=True)


def test_invalid_toolresult_fails_closed_before_http_success() -> None:
    router = ToolRouter()
    router.register_envelope_tool(AUTHORIZED_SYNTHETIC_CAPABILITY_ID, _InvalidTimingTool())
    application = create_app(
        runtime_executor=DeterministicRuntimeExecutor(tool_router=router),
        authorized_capability_ids=frozenset({AUTHORIZED_SYNTHETIC_CAPABILITY_ID}),
    )
    payload = _load_golden_payload()
    response = TestClient(application).post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["trace_id"]},
    )
    assert response.status_code == 400
    body = response.json()
    assert body["error_code"] == ERROR_RUNTIME_CONTRACT_OUTPUT_INVALID
    assert body.get("status") != "SUCCEEDED"
    print("RUNTIME_FULL_SEMANTIC_VALIDATOR_OUTPUT_FAIL_CLOSED=YES", flush=True)


def test_engineering_raw_ocr_success_path_remains_valid() -> None:
    application = create_engineering_raw_ocr_app(
        raw_ocr_tool_port=RawOcrToolAdapter(_DeterministicHelloEngine()),
        artifact_port=StaticAllowlistArtifactPort.for_synthetic_probe(),
    )
    payload = _raw_ocr_context_payload()
    response = TestClient(application).post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 200
    tool_result = ToolResult.model_validate(response.json())
    assert tool_result.status == "SUCCEEDED"
    raw_text = next(item.value for item in tool_result.output if item.name == "raw_text")
    assert "HELLO" in str(raw_text)
    assert "OCR" in str(raw_text)


def _java_feign_shaped_tool_context(capability_id: str) -> dict:
    """Reproduce Spring Feign wire JSON: unset IdentifierSet fields as null."""

    payload = _raw_ocr_context_payload()
    payload["envelope"]["capability_id"] = capability_id
    payload["envelope"]["capability_version"] = ENGINEERING_RAW_OCR_CAPABILITY_VERSION
    payload["envelope"]["producer"] = "java-diagnosis-real-tool"
    payload["capability"]["capability_id"] = capability_id
    payload["capability"]["capability_version"] = ENGINEERING_RAW_OCR_CAPABILITY_VERSION
    payload["identifiers"] = {
        "contract_version": "1.0.0",
        "cdp_id": "synthetic-cdp-engineering-raw-ocr-java-1",
        "patient_id": None,
        "encounter_id": None,
        "session_id": None,
        "tenant_id": None,
        "review_id": None,
        "delivery_id": None,
    }
    return payload


def test_java_feign_shaped_optional_null_identifiers_succeed_over_http() -> None:
    application = create_engineering_raw_ocr_app(
        raw_ocr_tool_port=RawOcrToolAdapter(_DeterministicHelloEngine()),
        artifact_port=StaticAllowlistArtifactPort.for_synthetic_probe(),
    )
    payload = _java_feign_shaped_tool_context(ENGINEERING_RAW_OCR_CAPABILITY_ID)
    assert validate_runtime_contract_instance("ToolContext", payload) == []
    response = TestClient(application).post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 200
    assert response.json()["status"] == "SUCCEEDED"


def test_java_feign_shaped_unauthorized_capability_remains_403() -> None:
    application = create_engineering_raw_ocr_app(
        raw_ocr_tool_port=RawOcrToolAdapter(_DeterministicHelloEngine()),
        artifact_port=StaticAllowlistArtifactPort.for_synthetic_probe(),
    )
    payload = _java_feign_shaped_tool_context(AUTHORIZED_SYNTHETIC_CAPABILITY_ID)
    response = TestClient(application).post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 403
    assert response.json()["error_code"] == ERROR_OPERATION_NOT_AUTHORIZED


def test_default_capability_surface_unchanged() -> None:
    application = create_app()
    assert application.state.authorized_capability_ids == frozenset(
        {AUTHORIZED_SYNTHETIC_CAPABILITY_ID}
    )
    payload = _raw_ocr_context_payload()
    response = TestClient(application).post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 403
    assert response.json()["error_code"] == ERROR_OPERATION_NOT_AUTHORIZED
    print("RUNTIME_FULL_SEMANTIC_VALIDATOR_DEFAULT_SURFACE_UNCHANGED=YES", flush=True)


def test_runtime_http_does_not_copy_semantic_rule_literals() -> None:
    violations = []
    for path in _RUNTIME_HTTP.glob("*.py"):
        text = path.read_text(encoding="utf-8")
        for literal in _FORBIDDEN_SEMANTIC_LITERALS:
            if literal in text:
                violations.append(f"{path.name}:{literal}")
    assert violations == []
