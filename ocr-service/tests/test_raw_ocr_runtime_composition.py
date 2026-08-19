"""POSTFREEZE-03B-B：注入式 Runtime + 真实 RawOcrEngine 组合测试。

本文件是测试专用组合边界：可同时导入 RawOcrEngine 与 python_runtime。
真实解码/预处理必须执行；pytesseract 必须被替换，禁止调用真实 Tesseract。
夹具仅为内存合成 PNG 与工程文本，不含患者或临床内容。
"""

from __future__ import annotations

import io
import typing

from PIL import Image
from pytesseract.pytesseract import TesseractNotFoundError

from app.services.raw_ocr import RawOcrEngine
from fastapi.testclient import TestClient

# 禁止在模块顶层导入 packages.python_runtime / aidoctor_shared_contracts。
# 历史 isolation 测试会扫描 sys.modules；组合导入必须留在函数体内。
#
# ocr-separation 锁定 Python 3.10。packages.model_runtime 在导入期执行
# `from typing import Self`，而 typing.Self 只存在于 3.11+。
# 不得安装 model_runtime/requirements-ci.txt：它会把 OCR pydantic
# 从 2.5.0 升到 2.10.3。不得改 model_runtime 或 Runtime 生产文件。
# 因此仅在本测试组合边界补一个导入期名字，让 3.10 能加载 Runtime。
if not hasattr(typing, "Self"):
    typing.Self = typing.TypeVar("Self")

_RAW_OCR_CAPABILITY_ID = "engineering.ocr.raw"
_RAW_OCR_CAPABILITY_VERSION = "0.0.1"
_SMOKE_CAPABILITY_ID = "engineering.synthetic.runtime_smoke"
_SYNTHETIC_OCR_TEXT = "HELLO OCR 123"
_ARTIFACT_ID = "artifact-synthetic-raw-ocr-compose-1"
_ARTIFACT_VERSION = 1


def _synthetic_png_bytes() -> bytes:
    """生成内存合成 PNG；不是医学影像，也不是仓库内二进制夹具。"""

    image = Image.new("RGB", (32, 16), color=(255, 255, 255))
    buffer = io.BytesIO()
    image.save(buffer, format="PNG")
    return buffer.getvalue()


def _artifact_record(content: bytes):
    """构造与实字节一致的非 PHI SourceArtifact 记录。"""

    from aidoctor_shared_contracts import SourceArtifact
    from packages.python_runtime.artifacts import StaticArtifactRecord, sha256_hex

    metadata = {
        "contract_version": "1.0.0",
        "artifact_id": _ARTIFACT_ID,
        "artifact_type": "UPLOAD",
        "owner_ref": "synthetic-owner-raw-ocr-compose-1",
        "content_type": "image/png",
        "original_filename": "synthetic-raw-ocr-compose.png",
        "size_bytes": len(content),
        "checksum": {"algorithm": "SHA-256", "value": sha256_hex(content)},
        "storage_ref": "artifact://engineering-synthetic/artifact-synthetic-raw-ocr-compose-1",
        "created_at": "2026-08-19T12:00:00Z",
        "processing_status": "RECEIVED",
        "derived_artifacts": [],
        "sensitivity": "INTERNAL",
        "retention_class": "ENGINEERING_SYNTHETIC",
    }
    return StaticArtifactRecord(metadata=SourceArtifact.model_validate(metadata), content=content)


def _raw_ocr_context_payload() -> dict:
    """冻结 schema 合法的合成 ToolContext。"""

    return {
        "contract_version": "1.0.0",
        "envelope": {
            "contract_name": "ToolContext",
            "contract_version": "1.0.0",
            "message_id": "msg-raw-ocr-compose-1",
            "correlation_id": "corr-raw-ocr-compose-1",
            "trace_id": "trace-raw-ocr-compose-1",
            "created_at": "2026-08-19T12:00:00Z",
            "producer": "python-runtime-raw-ocr-compose",
            "capability_id": _RAW_OCR_CAPABILITY_ID,
            "capability_version": _RAW_OCR_CAPABILITY_VERSION,
        },
        "actor": {"actor_id": "python-runtime-raw-ocr-compose", "actor_type": "SERVICE"},
        "identifiers": {
            "contract_version": "1.0.0",
            "cdp_id": "synthetic-cdp-raw-ocr-compose-1",
        },
        "capability": {
            "capability_id": _RAW_OCR_CAPABILITY_ID,
            "capability_version": _RAW_OCR_CAPABILITY_VERSION,
        },
        "current_state_ref": {
            "cdp_id": "synthetic-cdp-raw-ocr-compose-1",
            "version": 1,
            "read_fields": [],
        },
        "authorization_scope": {"granted": [], "requested": []},
        "deadline": "2026-08-19T12:05:00Z",
        "locale": "und",
        "requested_operation": "RAW_OCR_RECOGNIZE",
        "input_refs": [
            {
                "ref_type": "ARTIFACT",
                "ref_id": _ARTIFACT_ID,
                "ref_version": _ARTIFACT_VERSION,
            }
        ],
    }


def _injected_client(content: bytes, monkeypatch, pytesseract_impl) -> TestClient:
    """仅测试注入：真实 RawOcrEngine + 替换 pytesseract + 授权 raw OCR。"""

    import app.services.raw_ocr as raw_ocr_module
    from packages.python_runtime.artifacts import StaticAllowlistArtifactPort
    from packages.python_runtime.executor import DeterministicRuntimeExecutor
    from packages.python_runtime.http.app import create_app
    from packages.python_runtime.raw_ocr_adapter import RawOcrToolAdapter
    from packages.python_runtime.tool_router import ToolRouter
    from packages.python_runtime.tools import FakeToolPort

    monkeypatch.setattr(raw_ocr_module.pytesseract, "image_to_string", pytesseract_impl)
    router = ToolRouter()
    router.register_envelope_tool(_SMOKE_CAPABILITY_ID, FakeToolPort())
    router.register_context_tool(
        _RAW_OCR_CAPABILITY_ID,
        _RAW_OCR_CAPABILITY_VERSION,
        RawOcrToolAdapter(RawOcrEngine()),
    )
    executor = DeterministicRuntimeExecutor(
        tool_router=router,
        artifact_port=StaticAllowlistArtifactPort(
            {(_ARTIFACT_ID, _ARTIFACT_VERSION): _artifact_record(content)}
        ),
    )
    application = create_app(
        runtime_executor=executor,
        authorized_capability_ids=frozenset({_SMOKE_CAPABILITY_ID, _RAW_OCR_CAPABILITY_ID}),
    )
    return TestClient(application)


def test_injected_runtime_composition_preserves_mocked_raw_text(monkeypatch):
    """HTTP ToolContext -> ArtifactPort -> 适配器 -> 真引擎解码/预处理 -> mock Tesseract。"""

    observed_language = {}

    def fake_image_to_string(image, lang="eng"):
        # 证明预处理后的图像到达了 pytesseract 替换点
        assert image is not None
        observed_language["lang"] = lang
        return _SYNTHETIC_OCR_TEXT

    payload = _raw_ocr_context_payload()
    client = _injected_client(_synthetic_png_bytes(), monkeypatch, fake_image_to_string)
    response = client.post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "SUCCEEDED"
    assert body["reason_code"] == "RAW_OCR_OK"
    assert body["tool_name"] == "raw-ocr"
    assert body["output"] == [{"name": "raw_text", "value": _SYNTHETIC_OCR_TEXT}]
    assert body["suggested_patches"] == []
    assert body["evidence_refs"] == []
    assert body["errors"] == []
    assert observed_language["lang"] == "chi_sim+eng"
    serialized = str(body)
    assert "patient" not in serialized.lower()
    assert "diagnosis" not in serialized.lower()


def test_non_image_bytes_become_tool_result_failure_not_http_500(monkeypatch):
    """允许名单解析成功后，无法解码的字节必须变成 ToolResult 失败，而不是 500 或空串成功。"""

    def unused_image_to_string(image, lang="eng"):
        raise AssertionError("pytesseract must not run for undecodable bytes")

    payload = _raw_ocr_context_payload()
    client = _injected_client(b"not-an-image", monkeypatch, unused_image_to_string)
    response = client.post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "NON_RETRYABLE_FAILURE"
    assert body["retryable"] is False
    assert body["reason_code"] == "RAW_OCR_EXECUTION_FAILED"
    assert body["output"] == []
    assert body["errors"][0]["category"] == "INTERNAL"
    assert body["errors"][0]["code"] == "RAW_OCR_EXECUTION_FAILED"
    assert "" != body["status"]  # 不是遗留空串折叠
    assert "raw_text" not in str(body.get("output"))


def test_engine_unavailable_maps_through_injected_http(monkeypatch):
    """真实引擎 UNAVAILABLE 必须经适配器变成 RETRYABLE_FAILURE / DEPENDENCY。"""

    def missing_binary(image, lang="eng"):
        raise TesseractNotFoundError()

    payload = _raw_ocr_context_payload()
    client = _injected_client(_synthetic_png_bytes(), monkeypatch, missing_binary)
    response = client.post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "RETRYABLE_FAILURE"
    assert body["retryable"] is True
    assert body["reason_code"] == "RAW_OCR_ENGINE_UNAVAILABLE"
    assert body["errors"][0]["category"] == "DEPENDENCY"
    assert body["errors"][0]["code"] == "RAW_OCR_ENGINE_UNAVAILABLE"
    assert body["output"] == []
