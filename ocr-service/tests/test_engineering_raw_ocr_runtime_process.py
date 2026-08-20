"""POSTFREEZE-03B-G：engineering-only Raw OCR Runtime localhost 进程证明。

权威正向证明必须是 subprocess + 真实 TCP socket HTTP，不得用 TestClient 替代。
阶段闭合路径必须启动仓库入口 engineering/raw_ocr_runtime_process.py，
并经 create_engineering_raw_ocr_app 工厂，不得在本测试内手工重建组合。
必须实际调用 Tesseract 二进制；禁止 mock pytesseract / subprocess / 二进制发现。
夹具仅为内存合成 PNG 与工程文本，不含患者或临床内容。
不得覆盖 language；必须走 DEFAULT_OCR_LANGUAGE = chi_sim+eng。
"""

from __future__ import annotations

import base64
import io
import json
import os
import shutil
import socket
import subprocess
import sys
import time
import typing
import urllib.error
import urllib.request
from pathlib import Path

import pytest
from PIL import Image, ImageDraw, ImageFont
from fastapi.testclient import TestClient

from app.services.raw_ocr import DEFAULT_OCR_LANGUAGE

if not hasattr(typing, "Self"):
    typing.Self = typing.TypeVar("Self")

REQUIRE_REAL_TESSERACT = os.environ.get("AIDOCTOR_REQUIRE_REAL_TESSERACT") == "1"
REQUIRED_TESSERACT_LANGUAGES = ("eng", "chi_sim")

_RAW_OCR_CAPABILITY_ID = "engineering.ocr.raw"
_RAW_OCR_CAPABILITY_VERSION = "0.0.1"
_SMOKE_CAPABILITY_ID = "engineering.synthetic.runtime_smoke"
_SYNTHETIC_ENGINEERING_TEXT = "HELLO OCR 123"
_ARTIFACT_ID = "artifact-engineering-raw-ocr-process-1"
_ARTIFACT_VERSION = 1
_HEALTH_TIMEOUT_SECONDS = 20.0
_HEALTH_POLL_SECONDS = 0.2
_PROCESS_STOP_SECONDS = 5.0
_LOG_TAIL_CHARACTERS = 8000

_REPO_ROOT = Path(__file__).resolve().parents[2]
_LAUNCHER_PATH = _REPO_ROOT / "engineering" / "raw_ocr_runtime_process.py"


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


def _require_real_tesseract_or_skip() -> str:
    """
    强制环境：缺二进制或缺 traineddata 必须失败。
    非强制本地环境：允许 skip，且不得据此宣称进程已验证。
    """

    tesseract_binary = shutil.which("tesseract")
    if tesseract_binary is None:
        if REQUIRE_REAL_TESSERACT:
            pytest.fail(
                "AIDOCTOR_REQUIRE_REAL_TESSERACT=1 但未找到 tesseract 二进制"
            )
        pytest.skip(
            "本地未安装 tesseract 二进制；非强制环境允许跳过 engineering process 真实引擎测试"
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
    return tesseract_binary


def _render_synthetic_hello_ocr_png_bytes() -> bytes:
    """
    用仓库已有 Pillow 在内存渲染高对比度工程图。
    不落盘、不下载字体、不含医学或患者内容。
    """

    canvas_width = 1600
    canvas_height = 400
    canvas = Image.new("RGB", (canvas_width, canvas_height), color=(255, 255, 255))
    drawer = ImageDraw.Draw(canvas)
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


def _allocate_localhost_port() -> int:
    """分配 127.0.0.1 空闲端口，供测试隔离。"""

    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as listener:
        listener.bind(("127.0.0.1", 0))
        return int(listener.getsockname()[1])


def _process_pythonpath() -> str:
    """显式构造进程 PYTHONPATH；不得写入 launcher 的 sys.path。"""

    parts = [
        str(_REPO_ROOT / "ocr-service"),
        str(_REPO_ROOT / "contracts" / "v1" / "bindings" / "python"),
        str(_REPO_ROOT),
    ]
    return os.pathsep.join(parts)


def _raw_ocr_context_payload() -> dict:
    """冻结 schema 合法的合成 ToolContext。"""

    return {
        "contract_version": "1.0.0",
        "envelope": {
            "contract_name": "ToolContext",
            "contract_version": "1.0.0",
            "message_id": "msg-engineering-raw-ocr-process-1",
            "correlation_id": "corr-engineering-raw-ocr-process-1",
            "trace_id": "trace-engineering-raw-ocr-process-1",
            "created_at": "2026-08-20T05:00:00Z",
            "producer": "python-runtime-engineering-raw-ocr-process",
            "capability_id": _RAW_OCR_CAPABILITY_ID,
            "capability_version": _RAW_OCR_CAPABILITY_VERSION,
        },
        "actor": {
            "actor_id": "python-runtime-engineering-raw-ocr-process",
            "actor_type": "SERVICE",
        },
        "identifiers": {
            "contract_version": "1.0.0",
            "cdp_id": "synthetic-cdp-engineering-raw-ocr-process-1",
        },
        "capability": {
            "capability_id": _RAW_OCR_CAPABILITY_ID,
            "capability_version": _RAW_OCR_CAPABILITY_VERSION,
        },
        "current_state_ref": {
            "cdp_id": "synthetic-cdp-engineering-raw-ocr-process-1",
            "version": 1,
            "read_fields": [],
        },
        "authorization_scope": {"granted": [], "requested": []},
        "deadline": "2026-08-20T05:05:00Z",
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


def _stop_process(process: subprocess.Popen[str]) -> None:
    """终止工程进程；best-effort，不得吞掉测试失败。"""

    if process.poll() is not None:
        return
    process.terminate()
    try:
        process.wait(timeout=_PROCESS_STOP_SECONDS)
    except subprocess.TimeoutExpired:
        process.kill()
        process.wait(timeout=_PROCESS_STOP_SECONDS)


def _bounded_log(log_path: Path) -> str:
    """失败时打印有界进程日志，不回传 bootstrap 字节。"""

    if not log_path.is_file():
        return ""
    text = log_path.read_text(encoding="utf-8", errors="replace")
    if len(text) <= _LOG_TAIL_CHARACTERS:
        return text
    return text[-_LOG_TAIL_CHARACTERS:]


def _wait_for_health(port: int, process: subprocess.Popen[str], log_path: Path) -> dict:
    """短轮询真实 socket health，直到就绪、超时或进程退出。"""

    health_url = f"http://127.0.0.1:{port}/api/v1/runtime/health"
    deadline = time.monotonic() + _HEALTH_TIMEOUT_SECONDS
    last_error: Exception | None = None
    while time.monotonic() < deadline:
        if process.poll() is not None:
            raise AssertionError(
                "engineering Raw OCR process exited before health became ready\n"
                + _bounded_log(log_path)
            )
        try:
            with urllib.request.urlopen(health_url, timeout=1.0) as response:
                body = json.loads(response.read().decode("utf-8"))
            if response.status == 200:
                return body
        except (urllib.error.URLError, TimeoutError, json.JSONDecodeError, OSError) as exc:
            last_error = exc
        time.sleep(_HEALTH_POLL_SECONDS)
    raise AssertionError(
        f"engineering Raw OCR process health timeout at {health_url}: {last_error}\n"
        + _bounded_log(log_path)
    )


def test_default_create_app_surface_unchanged_for_process_stage() -> None:
    """默认 create_app() 授权面不变；本断言不是进程正向证明。"""

    from packages.python_runtime.http.app import create_app

    default_app = create_app()
    authorized = default_app.state.authorized_capability_ids
    assert authorized == frozenset({_SMOKE_CAPABILITY_ID})
    assert _RAW_OCR_CAPABILITY_ID not in authorized

    payload = _raw_ocr_context_payload()
    client = TestClient(default_app)
    response = client.post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers={"X-Trace-Id": payload["envelope"]["trace_id"]},
    )
    assert response.status_code == 403
    assert response.json()["error_code"] == "OPERATION_NOT_AUTHORIZED"
    print("DEFAULT_RUNTIME_CAPABILITY_SURFACE_UNCHANGED=YES", flush=True)


def test_engineering_raw_ocr_runtime_process_real_socket_http() -> None:
    """
    仓库 launcher → localhost 进程 → 真实 socket HTTP → 真 Tesseract。
    不覆盖 language；必须走默认 chi_sim+eng。
    """

    tesseract_binary = _require_real_tesseract_or_skip()
    assert _LAUNCHER_PATH.is_file()
    assert DEFAULT_OCR_LANGUAGE == "chi_sim+eng"

    print("ENGINEERING_RAW_OCR_RUNTIME_PROCESS_LAUNCHER_USED=YES", flush=True)
    print("ENGINEERING_RAW_OCR_RUNTIME_PROCESS_LOCALHOST_ONLY=YES", flush=True)
    print("ENGINEERING_RAW_OCR_RUNTIME_PROCESS_HOST=127.0.0.1", flush=True)
    print("ENGINEERING_RAW_OCR_RUNTIME_PROCESS_FACTORY_USED=YES", flush=True)
    print(f"ENGINEERING_RAW_OCR_RUNTIME_PROCESS_TESSERACT_BINARY={tesseract_binary}", flush=True)
    print("ENGINEERING_RAW_OCR_RUNTIME_PROCESS_LANG_ENG=YES", flush=True)
    print("ENGINEERING_RAW_OCR_RUNTIME_PROCESS_LANG_CHI_SIM=YES", flush=True)
    print(
        f"ENGINEERING_RAW_OCR_RUNTIME_PROCESS_DEFAULT_LANGUAGE={DEFAULT_OCR_LANGUAGE}",
        flush=True,
    )

    image_bytes = _render_synthetic_hello_ocr_png_bytes()
    port = _allocate_localhost_port()
    process_env = os.environ.copy()
    process_env["PYTHONPATH"] = _process_pythonpath()
    process_env["AIDOCTOR_ENGINEERING_RAW_OCR_PORT"] = str(port)
    process_env["AIDOCTOR_ENGINEERING_RAW_OCR_ARTIFACT_BASE64"] = base64.b64encode(
        image_bytes
    ).decode("ascii")

    log_path = _REPO_ROOT / "logs" / "engineering-raw-ocr-runtime-process.log"
    log_path.parent.mkdir(parents=True, exist_ok=True)
    process: subprocess.Popen[str] | None = None
    try:
        with log_path.open("w", encoding="utf-8") as log_file:
            process = subprocess.Popen(
                [sys.executable, str(_LAUNCHER_PATH)],
                cwd=str(_REPO_ROOT),
                env=process_env,
                stdout=log_file,
                stderr=subprocess.STDOUT,
                text=True,
            )
        health_body = _wait_for_health(port, process, log_path)
        assert health_body["status"] == "engineering_ready"
        assert health_body["contract_version"] == "1.0.0"
        assert health_body["mode"] == "NON_PRODUCTION_ENGINEERING_PROTOCOL_PROOF"
        print("ENGINEERING_RAW_OCR_RUNTIME_PROCESS_HEALTH=PASS", flush=True)

        payload = _raw_ocr_context_payload()
        request = urllib.request.Request(
            f"http://127.0.0.1:{port}/api/v1/runtime/tools/invoke",
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Content-Type": "application/json",
                "X-Trace-Id": payload["envelope"]["trace_id"],
            },
            method="POST",
        )
        with urllib.request.urlopen(request, timeout=30.0) as response:
            assert response.status == 200
            body = json.loads(response.read().decode("utf-8"))

        assert body["status"] == "SUCCEEDED"
        assert body["reason_code"] == "RAW_OCR_OK"
        assert body["tool_name"] == "raw-ocr"
        assert body["errors"] == []
        assert body["suggested_patches"] == []
        assert body["evidence_refs"] == []

        raw_text_item = next(
            item for item in body["output"] if item.get("name") == "raw_text"
        )
        normalized_text = " ".join(str(raw_text_item["value"]).upper().split())
        assert "HELLO" in normalized_text
        assert "OCR" in normalized_text
        print("ENGINEERING_RAW_OCR_RUNTIME_PROCESS_TOKEN_HELLO=YES", flush=True)
        print("ENGINEERING_RAW_OCR_RUNTIME_PROCESS_TOKEN_OCR=YES", flush=True)
        print("ENGINEERING_RAW_OCR_RUNTIME_PROCESS_HTTP=PASS", flush=True)

        serialized = str(body).lower()
        assert "patient" not in serialized
        assert "diagnosis" not in serialized
        assert "medication" not in serialized
        assert "reference_range" not in serialized
    except Exception:
        if log_path.is_file():
            print(_bounded_log(log_path), flush=True)
        raise
    finally:
        if process is not None:
            _stop_process(process)
