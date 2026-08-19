"""POSTFREEZE-02 Python Runtime HTTP 适配器：FastAPI TestClient，无真实网络推理。"""

from __future__ import annotations

import json
from pathlib import Path

from aidoctor_shared_contracts import ContractEnvelope, ToolResult
from fastapi.testclient import TestClient

from packages.python_runtime.http.app import create_app
from packages.python_runtime.http.transport import (
    AUTHORIZED_SYNTHETIC_CAPABILITY_ID,
    ERROR_CONTRACT_VERSION_MISMATCH,
    ERROR_ENVELOPE_INVALID,
    ERROR_IDENTITY_MISMATCH,
    ERROR_OPERATION_NOT_AUTHORIZED,
)

_FIXTURE_PATH = (
    Path(__file__).resolve().parent / "fixtures" / "runtime_protocol_invoke.json"
)


def _load_golden_payload() -> dict:
    """加载 Java/Python 共用的合成互操作夹具。"""

    return json.loads(_FIXTURE_PATH.read_text(encoding="utf-8"))


def _client() -> TestClient:
    """构造不监听套接字的工程协议证明客户端。"""

    return TestClient(create_app())


def test_golden_fixture_is_valid_shared_contract_and_synthetic():
    """夹具必须通过真实 ContractEnvelope 绑定，且不含患者/临床内容。"""

    payload = _load_golden_payload()
    envelope = ContractEnvelope.model_validate(payload)
    assert envelope.contract_version == "1.0.0"
    assert envelope.capability_id == AUTHORIZED_SYNTHETIC_CAPABILITY_ID
    serialized = json.dumps(payload)
    assert "patient" not in serialized.lower()
    assert "PHI" not in serialized
    assert "adult_respiratory" not in serialized


def test_health_returns_bounded_engineering_ready_response():
    """health 只返回工程就绪元数据，不声称生产或临床就绪。"""

    response = _client().get("/api/v1/runtime/health")
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "engineering_ready"
    assert body["contract_version"] == "1.0.0"
    assert body["mode"] == "NON_PRODUCTION_ENGINEERING_PROTOCOL_PROOF"
    serialized = json.dumps(body)
    assert "production ready" not in serialized.lower()
    assert "clinical ready" not in serialized.lower()
    assert "repository runtime verified" not in serialized.lower()


def test_valid_synthetic_invoke_returns_succeeded_tool_result():
    """合法 1.0.0 合成调用返回 Shared Contracts ToolResult SUCCEEDED。"""

    payload = _load_golden_payload()
    response = _client().post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["trace_id"]},
    )
    assert response.status_code == 200
    tool_result = ToolResult.model_validate(response.json())
    assert tool_result.status == "SUCCEEDED"
    assert tool_result.tool_name == "synthetic-echo"
    assert tool_result.retryable is False
    assert tool_result.suggested_patches == []
    assert tool_result.errors == []
    assert response.headers.get("x-trace-id") == payload["trace_id"]


def test_same_deterministic_request_same_logical_result():
    """相同合成请求产生相同逻辑结果。"""

    payload = _load_golden_payload()
    headers = {"X-Trace-Id": payload["trace_id"]}
    client = _client()
    first = client.post("/api/v1/runtime/tools/invoke", json=payload, headers=headers)
    second = client.post("/api/v1/runtime/tools/invoke", json=payload, headers=headers)
    assert first.status_code == 200
    assert second.status_code == 200
    assert first.json() == second.json()


def test_wrong_contract_version_fails_closed():
    """2.0.0 必须失败关闭，且可与通用信封错误区分。"""

    payload = _load_golden_payload()
    payload["contract_version"] = "2.0.0"
    response = _client().post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["trace_id"]},
    )
    assert response.status_code == 400
    body = response.json()
    assert body["error_code"] == ERROR_CONTRACT_VERSION_MISMATCH
    assert body["retryable"] is False
    assert "tool_name" not in body


def test_malformed_envelope_fails_closed():
    """残缺信封失败关闭为 ENVELOPE_INVALID。"""

    response = _client().post(
        "/api/v1/runtime/tools/invoke",
        json={"contract_name": "ToolContext"},
        headers={"X-Trace-Id": "trace-protocol-1"},
    )
    assert response.status_code == 400
    body = response.json()
    assert body["error_code"] == ERROR_ENVELOPE_INVALID
    assert body["retryable"] is False


def test_missing_trace_header_fails_closed():
    """缺少 X-Trace-Id 失败关闭。"""

    payload = _load_golden_payload()
    response = _client().post("/api/v1/runtime/tools/invoke", json=payload)
    assert response.status_code == 400
    assert response.json()["error_code"] == ERROR_IDENTITY_MISMATCH


def test_trace_header_body_mismatch_fails_closed():
    """X-Trace-Id 与信封 trace_id 不一致失败关闭。"""

    payload = _load_golden_payload()
    response = _client().post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": "trace-other-1"},
    )
    assert response.status_code == 400
    assert response.json()["error_code"] == ERROR_IDENTITY_MISMATCH


def test_valid_trace_header_is_echoed():
    """合法 X-Trace-Id 在成功响应中回显。"""

    payload = _load_golden_payload()
    response = _client().post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["trace_id"]},
    )
    assert response.status_code == 200
    assert response.headers.get("x-trace-id") == payload["trace_id"]


def test_optional_synthetic_cdp_header_is_echoed():
    """可选合成 X-CDP-Id 仅回显，不做患者权威解释。"""

    payload = _load_golden_payload()
    synthetic_cdp_id = "synthetic-cdp-protocol-1"
    response = _client().post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={
            "X-Trace-Id": payload["trace_id"],
            "X-CDP-Id": synthetic_cdp_id,
        },
    )
    assert response.status_code == 200
    assert response.headers.get("x-cdp-id") == synthetic_cdp_id
    assert "patient_id" not in json.dumps(response.json())


def test_unauthorized_capability_fails_closed():
    """未授权能力身份失败关闭，且不执行工具。"""

    payload = _load_golden_payload()
    payload["capability_id"] = "engineering.synthetic.unauthorized"
    response = _client().post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["trace_id"]},
    )
    assert response.status_code == 403
    body = response.json()
    assert body["error_code"] == ERROR_OPERATION_NOT_AUTHORIZED
    assert "synthetic-echo" not in json.dumps(body)


def test_success_has_empty_suggested_patches_and_no_phi():
    """成功响应不得携带补丁、PHI、临床或 provider 语义。"""

    payload = _load_golden_payload()
    response = _client().post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["trace_id"]},
    )
    assert response.status_code == 200
    body = response.json()
    assert body["suggested_patches"] == []
    serialized = json.dumps(body)
    assert "patient_id" not in serialized
    assert "PHI" not in serialized
    assert "diagnosis" not in serialized.lower()
    assert "openai" not in serialized.lower()
    assert "langgraph" not in serialized.lower()
