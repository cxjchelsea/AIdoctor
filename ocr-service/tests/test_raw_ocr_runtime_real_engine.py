"""POSTFREEZE-03B-D：注入式 Runtime + 真实 Tesseract 端到端组合证明。

本文件是测试专用组合边界：可同时导入 RawOcrEngine 与 python_runtime。
必须实际调用 Tesseract 二进制；禁止 mock pytesseract / subprocess / 二进制发现。
夹具仅为内存合成 PNG 与工程文本，不含患者或临床内容。
不得覆盖 language；必须走 DEFAULT_OCR_LANGUAGE = chi_sim+eng。
"""

from __future__ import annotations

import io
import os
import shutil
import subprocess
import typing

import pytest
from PIL import Image, ImageDraw, ImageFont
from fastapi.testclient import TestClient

from app.services.raw_ocr import DEFAULT_OCR_LANGUAGE, RawOcrEngine

# 禁止在模块顶层导入 packages.python_runtime / aidoctor_shared_contracts。
# 历史 isolation 测试会扫描 sys.modules；组合导入必须留在函数体内。
#
# ocr-separation / 本批 CI 锁定 Python 3.10。packages.model_runtime 在导入期执行
# `from typing import Self`，而 typing.Self 只存在于 3.11+。
# 不得安装 model_runtime/requirements-ci.txt：它会改变 OCR pydantic 表面。
# 不得改 model_runtime 或 Runtime 生产文件。
# 因此仅在本测试组合边界补一个导入期名字，让 3.10 能加载 Runtime。
if not hasattr(typing, "Self"):
    typing.Self = typing.TypeVar("Self")

# 仅权威 CI 置 1；本地缺二进制时允许 skip，CI 缺二进制必须失败。
REQUIRE_REAL_TESSERACT = os.environ.get("AIDOCTOR_REQUIRE_REAL_TESSERACT") == "1"
REQUIRED_TESSERACT_LANGUAGES = ("eng", "chi_sim")

_RAW_OCR_CAPABILITY_ID = "engineering.ocr.raw"
_RAW_OCR_CAPABILITY_VERSION = "0.0.1"
_SMOKE_CAPABILITY_ID = "engineering.synthetic.runtime_smoke"
_SYNTHETIC_ENGINEERING_TEXT = "HELLO OCR 123"
_ARTIFACT_ID = "artifact-synthetic-runtime-real-ocr-1"
_ARTIFACT_VERSION = 1


def _list_tesseract_languages() -> set[str]:
    """读取 tesseract --list-langs，兼容 stdout/stderr 历史差异。"""
    completed_process = subprocess.run(
        ["tesseract", "--list-langs"],
        check=False,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    combined_output = "\n".join(
        output_part
        for output_part in (completed_process.stdout, completed_process.stderr)
        if output_part
    )
    discovered_languages: set[str] = set()
    for raw_line in combined_output.splitlines():
        language_name = raw_line.strip()
        if not language_name or language_name.lower().startswith("list of"):
            continue
        discovered_languages.add(language_name)
    return discovered_languages


def _require_real_tesseract_or_skip() -> None:
    """
    强制环境：缺二进制或缺 traineddata 必须失败。
    非强制本地环境：允许 skip，且不得据此宣称组合已验证。
    """
    tesseract_binary = shutil.which("tesseract")
    if tesseract_binary is None:
        if REQUIRE_REAL_TESSERACT:
            pytest.fail(
                "AIDOCTOR_REQUIRE_REAL_TESSERACT=1 但未找到 tesseract 二进制"
            )
        pytest.skip(
            "本地未安装 tesseract 二进制；非强制环境允许跳过 Runtime 真实引擎组合测试"
        )

    available_languages = _list_tesseract_languages()
    missing_languages = [
        language_name
        for language_name in REQUIRED_TESSERACT_LANGUAGES
        if language_name not in available_languages
    ]
    if missing_languages:
        missing_text = ", ".join(missing_languages)
        if REQUIRE_REAL_TESSERACT:
            pytest.fail(
                f"AIDOCTOR_REQUIRE_REAL_TESSERACT=1 但缺少 traineddata: {missing_text}"
            )
        pytest.skip(
            f"本地缺少 Tesseract traineddata: {missing_text}；非强制环境允许跳过"
        )


def _render_synthetic_hello_ocr_png_bytes() -> bytes:
    """
    用仓库已有 Pillow 在内存渲染高对比度工程图。
    不落盘、不下载字体、不含医学或患者内容。
    """
    canvas_width = 1600
    canvas_height = 400
    canvas = Image.new("RGB", (canvas_width, canvas_height), color=(255, 255, 255))
    drawer = ImageDraw.Draw(canvas)
    # Pillow 10.1.0 支持 load_default(size=)，使用内置可缩放默认字体。
    text_font = ImageFont.load_default(size=96)
    text_bbox = drawer.textbbox((0, 0), _SYNTHETIC_ENGINEERING_TEXT, font=text_font)
    text_width = text_bbox[2] - text_bbox[0]
    text_height = text_bbox[3] - text_bbox[1]
    text_left = max((canvas_width - text_width) // 2, 40)
    text_top = max((canvas_height - text_height) // 2, 40)
    drawer.text(
        (text_left, text_top),
        _SYNTHETIC_ENGINEERING_TEXT,
        fill=(0, 0, 0),
        font=text_font,
    )
    png_buffer = io.BytesIO()
    canvas.save(png_buffer, format="PNG")
    return png_buffer.getvalue()


def _artifact_record(content: bytes):
    """构造与实字节一致的非 PHI SourceArtifact 记录。"""

    from aidoctor_shared_contracts import SourceArtifact
    from packages.python_runtime.artifacts import StaticArtifactRecord, sha256_hex

    metadata = {
        "contract_version": "1.0.0",
        "artifact_id": _ARTIFACT_ID,
        "artifact_type": "UPLOAD",
        "owner_ref": "synthetic-owner-runtime-real-ocr-1",
        "content_type": "image/png",
        "original_filename": "synthetic-runtime-real-ocr.png",
        "size_bytes": len(content),
        "checksum": {"algorithm": "SHA-256", "value": sha256_hex(content)},
        "storage_ref": (
            "artifact://engineering-synthetic/artifact-synthetic-runtime-real-ocr-1"
        ),
        "created_at": "2026-08-20T01:00:00Z",
        "processing_status": "RECEIVED",
        "derived_artifacts": [],
        "sensitivity": "INTERNAL",
        "retention_class": "ENGINEERING_SYNTHETIC",
    }
    return StaticArtifactRecord(
        metadata=SourceArtifact.model_validate(metadata),
        content=content,
    )


def _raw_ocr_context_payload() -> dict:
    """冻结 schema 合法的合成 ToolContext。"""

    return {
        "contract_version": "1.0.0",
        "envelope": {
            "contract_name": "ToolContext",
            "contract_version": "1.0.0",
            "message_id": "msg-runtime-real-ocr-1",
            "correlation_id": "corr-runtime-real-ocr-1",
            "trace_id": "trace-runtime-real-ocr-1",
            "created_at": "2026-08-20T01:00:00Z",
            "producer": "python-runtime-raw-ocr-real-engine",
            "capability_id": _RAW_OCR_CAPABILITY_ID,
            "capability_version": _RAW_OCR_CAPABILITY_VERSION,
        },
        "actor": {
            "actor_id": "python-runtime-raw-ocr-real-engine",
            "actor_type": "SERVICE",
        },
        "identifiers": {
            "contract_version": "1.0.0",
            "cdp_id": "synthetic-cdp-runtime-real-ocr-1",
        },
        "capability": {
            "capability_id": _RAW_OCR_CAPABILITY_ID,
            "capability_version": _RAW_OCR_CAPABILITY_VERSION,
        },
        "current_state_ref": {
            "cdp_id": "synthetic-cdp-runtime-real-ocr-1",
            "version": 1,
            "read_fields": [],
        },
        "authorization_scope": {"granted": [], "requested": []},
        "deadline": "2026-08-20T01:05:00Z",
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


def _injected_real_engine_client(content: bytes) -> TestClient:
    """仅测试注入：真实 RawOcrEngine，不替换 pytesseract，授权 raw OCR。"""

    from packages.python_runtime.artifacts import StaticAllowlistArtifactPort
    from packages.python_runtime.executor import DeterministicRuntimeExecutor
    from packages.python_runtime.http.app import create_app
    from packages.python_runtime.raw_ocr_adapter import RawOcrToolAdapter
    from packages.python_runtime.tool_router import ToolRouter
    from packages.python_runtime.tools import FakeToolPort

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
        authorized_capability_ids=frozenset(
            {_SMOKE_CAPABILITY_ID, _RAW_OCR_CAPABILITY_ID}
        ),
    )
    return TestClient(application)


def test_default_create_app_does_not_authorize_raw_ocr() -> None:
    """默认 create_app() 不得授权 engineering.ocr.raw；smoke 仍保留。"""

    from packages.python_runtime.http.app import create_app

    default_app = create_app()
    authorized = default_app.state.authorized_capability_ids
    assert _SMOKE_CAPABILITY_ID in authorized
    assert _RAW_OCR_CAPABILITY_ID not in authorized


def test_injected_runtime_real_tesseract_composition_recognizes_hello_ocr() -> None:
    """
    HTTP ToolContext -> ArtifactPort -> 适配器 -> 真引擎 -> 真实 Tesseract。
    不覆盖 language；必须走默认 chi_sim+eng。
    """
    _require_real_tesseract_or_skip()

    assert DEFAULT_OCR_LANGUAGE == "chi_sim+eng"

    image_bytes = _render_synthetic_hello_ocr_png_bytes()
    payload = _raw_ocr_context_payload()
    client = _injected_real_engine_client(image_bytes)
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
    assert body["errors"] == []
    assert body["suggested_patches"] == []
    assert body["evidence_refs"] == []

    output_items = body["output"]
    assert isinstance(output_items, list)
    raw_text_item = next(
        item for item in output_items if item.get("name") == "raw_text"
    )
    raw_text = raw_text_item["value"]
    normalized_text = " ".join(str(raw_text).upper().split())
    assert "HELLO" in normalized_text
    assert "OCR" in normalized_text

    serialized = str(body).lower()
    assert "patient" not in serialized
    assert "diagnosis" not in serialized
    assert "medication" not in serialized
    assert "reference_range" not in serialized
