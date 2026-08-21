"""POSTFREEZE-03D：规范 Runtime 受控 Raw OCR opt-in。

组合/HTTP 负例可用 TestClient。关阶段正向证明必须是
engineering/raw_ocr_runtime_process.py subprocess + 真实 socket HTTP。
合成夹具 only；不声称 OCR 精度、临床或 Production。
"""

from __future__ import annotations

import ast
import hashlib
import io
import json
import os
import shutil
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from packages.python_runtime.artifacts import (
    SYNTHETIC_ARTIFACT_PROBE_ID,
    SYNTHETIC_ARTIFACT_PROBE_VERSION,
    StaticAllowlistArtifactPort,
)
from packages.python_runtime.http.app import app as default_uvicorn_app
from packages.python_runtime.http.app import create_app
from packages.python_runtime.http.controlled_composition import (
    CONTROLLED_RAW_OCR_CAPABILITY_ID,
    CONTROLLED_RAW_OCR_CAPABILITY_VERSION,
    ControlledCompositionError,
    ControlledRawOcrComposition,
    create_controlled_raw_ocr_app,
)
from packages.python_runtime.http.transport import (
    AUTHORIZED_SYNTHETIC_CAPABILITY_ID,
    ERROR_OPERATION_NOT_AUTHORIZED,
)
from packages.python_runtime.tools import FakeToolPort

_REPO_ROOT = Path(__file__).resolve().parents[3]
_LAUNCHER_PATH = _REPO_ROOT / "engineering" / "raw_ocr_runtime_process.py"
_CONTROLLED_COMPOSITION_PATH = (
    _REPO_ROOT / "packages" / "python_runtime" / "http" / "controlled_composition.py"
)
_REQUIRE_REAL_TESSERACT = os.environ.get("AIDOCTOR_REQUIRE_REAL_TESSERACT") == "1"
_REQUIRED_TESSERACT_LANGUAGES = ("eng", "chi_sim")
_SYNTHETIC_TEXT = "HELLO OCR 123"
_PROCESS_ARTIFACT_ID = "artifact-engineering-raw-ocr-process-1"
_PROCESS_ARTIFACT_VERSION = 1
_HEALTH_TIMEOUT_SECONDS = 20.0


class _RecordingContextTool:
    """记录是否被执行；信封结果复用 FakeToolPort。"""

    def __init__(self) -> None:
        self.invocations = 0

    def invoke(self, context, artifacts):
        del artifacts
        self.invocations += 1
        return FakeToolPort().invoke(context.envelope)


def _raw_ocr_context_payload() -> dict:
    return {
        "contract_version": "1.0.0",
        "envelope": {
            "contract_name": "ToolContext",
            "contract_version": "1.0.0",
            "message_id": "msg-03d-controlled-1",
            "correlation_id": "corr-03d-controlled-1",
            "trace_id": "trace-03d-controlled-1",
            "created_at": "2026-08-21T02:00:00Z",
            "producer": "python-runtime-03d-controlled",
            "capability_id": CONTROLLED_RAW_OCR_CAPABILITY_ID,
            "capability_version": CONTROLLED_RAW_OCR_CAPABILITY_VERSION,
        },
        "actor": {"actor_id": "python-runtime-03d-controlled", "actor_type": "SERVICE"},
        "identifiers": {
            "contract_version": "1.0.0",
            "cdp_id": "synthetic-cdp-03d-controlled-1",
        },
        "capability": {
            "capability_id": CONTROLLED_RAW_OCR_CAPABILITY_ID,
            "capability_version": CONTROLLED_RAW_OCR_CAPABILITY_VERSION,
        },
        "current_state_ref": {
            "cdp_id": "synthetic-cdp-03d-controlled-1",
            "version": 1,
            "read_fields": [],
        },
        "authorization_scope": {"granted": [], "requested": []},
        "deadline": "2026-08-21T02:05:00Z",
        "locale": "und",
        "requested_operation": "RAW_OCR_RECOGNIZE",
        "input_refs": [
            {
                "ref_type": "ARTIFACT",
                "ref_id": SYNTHETIC_ARTIFACT_PROBE_ID,
                "ref_version": SYNTHETIC_ARTIFACT_PROBE_VERSION,
            }
        ],
    }


def _invoke(application, payload: dict, extra_headers: dict | None = None):
    headers = {"X-Trace-Id": payload["envelope"]["trace_id"]}
    if extra_headers:
        headers.update(extra_headers)
    return TestClient(application).post(
        "/api/v1/runtime/tools/invoke",
        json=payload,
        headers=headers,
    )


def test_default_create_app_rejects_raw_ocr() -> None:
    application = create_app()
    payload = _raw_ocr_context_payload()
    response = _invoke(application, payload)
    assert response.status_code == 403
    assert response.json()["error_code"] == ERROR_OPERATION_NOT_AUTHORIZED
    assert CONTROLLED_RAW_OCR_CAPABILITY_ID not in application.state.authorized_capability_ids
    print("CANONICAL_DEFAULT_RAW_OCR_REJECTED=YES", flush=True)


def test_controlled_full_opt_in_succeeds() -> None:
    tool = _RecordingContextTool()
    application = create_controlled_raw_ocr_app(
        ControlledRawOcrComposition(register_raw_ocr=True, authorize_raw_ocr=True),
        raw_ocr_tool_port=tool,
        artifact_port=StaticAllowlistArtifactPort.for_synthetic_probe(),
    )
    response = _invoke(application, _raw_ocr_context_payload())
    assert response.status_code == 200
    assert response.json()["status"] == "SUCCEEDED"
    assert tool.invocations == 1
    print("CANONICAL_CONTROLLED_OPT_IN_REGISTERED=YES", flush=True)
    print("CANONICAL_CONTROLLED_OPT_IN_AUTHORIZED=YES", flush=True)
    print("CANONICAL_CONTROLLED_OPT_IN_HTTP=PASS", flush=True)


def test_registered_but_unauthorized_rejected() -> None:
    tool = _RecordingContextTool()
    application = create_controlled_raw_ocr_app(
        ControlledRawOcrComposition(register_raw_ocr=True, authorize_raw_ocr=False),
        raw_ocr_tool_port=tool,
        artifact_port=StaticAllowlistArtifactPort.for_synthetic_probe(),
    )
    response = _invoke(application, _raw_ocr_context_payload())
    assert response.status_code == 403
    assert response.json()["error_code"] == ERROR_OPERATION_NOT_AUTHORIZED
    assert tool.invocations == 0
    print("REGISTERED_BUT_UNAUTHORIZED_REJECTED=YES", flush=True)


def test_authorized_but_unregistered_rejected() -> None:
    application = create_controlled_raw_ocr_app(
        ControlledRawOcrComposition(register_raw_ocr=False, authorize_raw_ocr=True),
    )
    response = _invoke(application, _raw_ocr_context_payload())
    assert response.status_code == 400
    assert response.json()["error_code"] == "TOOL_UNREGISTERED"
    print("AUTHORIZED_BUT_UNREGISTERED_REJECTED=YES", flush=True)


def test_invalid_controlled_composition_fails_closed() -> None:
    with pytest.raises(ControlledCompositionError):
        create_controlled_raw_ocr_app(
            ControlledRawOcrComposition(register_raw_ocr=True, authorize_raw_ocr=True),
            raw_ocr_tool_port=None,
            artifact_port=None,
        )
    print("INVALID_CONTROLLED_COMPOSITION_FAILS_CLOSED=YES", flush=True)


def test_client_cannot_enable_raw_ocr_on_default_app() -> None:
    application = create_app()
    payload = _raw_ocr_context_payload()
    client = TestClient(application)
    response = client.post(
        "/api/v1/runtime/tools/invoke",
        params={"enable_raw_ocr": "true"},
        json=payload,
        headers={
            "X-Trace-Id": payload["envelope"]["trace_id"],
            "X-Runtime-Profile": "engineering_raw_ocr",
        },
    )
    assert response.status_code == 403
    assert response.json()["error_code"] == ERROR_OPERATION_NOT_AUTHORIZED
    print("REQUEST_CONTROLLED_RAW_OCR_ENABLEMENT=NO", flush=True)


def test_default_uvicorn_app_surface_unchanged() -> None:
    authorized = default_uvicorn_app.state.authorized_capability_ids
    assert authorized == frozenset({AUTHORIZED_SYNTHETIC_CAPABILITY_ID})
    assert CONTROLLED_RAW_OCR_CAPABILITY_ID not in authorized
    print("DEFAULT_UVICORN_APP_RAW_OCR_ENABLED=NO", flush=True)
    print("DEFAULT_RUNTIME_CAPABILITY_SURFACE_UNCHANGED=YES", flush=True)
    print("CANONICAL_RUNTIME_RAW_OCR_ENABLED=NO", flush=True)


def test_canonical_create_app_path_is_used() -> None:
    source = _CONTROLLED_COMPOSITION_PATH.read_text(encoding="utf-8")
    tree = ast.parse(source)
    called: set[str] = set()
    for node in ast.walk(tree):
        if isinstance(node, ast.Call) and isinstance(node.func, ast.Name):
            called.add(node.func.id)
    assert "create_app" in called
    assert "APIRouter" not in called
    assert "uvicorn" not in source
    launcher_source = _LAUNCHER_PATH.read_text(encoding="utf-8")
    assert "create_controlled_raw_ocr_app" in launcher_source
    assert "ToolRouter(" not in launcher_source
    assert "DeterministicRuntimeExecutor(" not in launcher_source
    assert "create_app(" not in launcher_source
    print("CANONICAL_CREATE_APP_USED=YES", flush=True)
    print("CANONICAL_RUNTIME_PATH_USED=YES", flush=True)


def _list_tesseract_languages() -> set[str]:
    completed = subprocess.run(
        ["tesseract", "--list-langs"],
        check=False,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        timeout=30,
    )
    combined = "\n".join(part for part in (completed.stdout, completed.stderr) if part)
    languages: set[str] = set()
    for raw_line in combined.splitlines():
        name = raw_line.strip()
        if not name or name.lower().startswith("list of"):
            continue
        languages.add(name)
    return languages


def _require_real_tesseract_or_skip() -> str:
    binary = shutil.which("tesseract")
    if binary is None:
        if _REQUIRE_REAL_TESSERACT:
            pytest.fail("AIDOCTOR_REQUIRE_REAL_TESSERACT=1 但未找到 tesseract")
        pytest.skip("本地未安装 tesseract；非强制环境允许跳过真实进程证明")
    missing = [name for name in _REQUIRED_TESSERACT_LANGUAGES if name not in _list_tesseract_languages()]
    if missing:
        if _REQUIRE_REAL_TESSERACT:
            pytest.fail(f"AIDOCTOR_REQUIRE_REAL_TESSERACT=1 但缺少 traineddata: {missing}")
        pytest.skip(f"本地缺少 Tesseract traineddata: {missing}")
    return binary


def _render_synthetic_png() -> bytes:
    from PIL import Image, ImageDraw, ImageFont

    canvas = Image.new("RGB", (1600, 400), color=(255, 255, 255))
    drawer = ImageDraw.Draw(canvas)
    font = ImageFont.load_default(size=96)
    bbox = drawer.textbbox((0, 0), _SYNTHETIC_TEXT, font=font)
    left = max((1600 - (bbox[2] - bbox[0])) // 2, 40)
    top = max((400 - (bbox[3] - bbox[1])) // 2, 40)
    drawer.text((left, top), _SYNTHETIC_TEXT, fill=(0, 0, 0), font=font)
    buffer = io.BytesIO()
    canvas.save(buffer, format="PNG")
    return buffer.getvalue()


def _allocate_localhost_port() -> int:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as listener:
        listener.bind(("127.0.0.1", 0))
        return int(listener.getsockname()[1])


def test_controlled_opt_in_real_localhost_process(tmp_path: Path) -> None:
    tesseract_binary = _require_real_tesseract_or_skip()
    assert _LAUNCHER_PATH.is_file()
    image_bytes = _render_synthetic_png()
    sandbox_root = tmp_path / "aidoctor-03d-artifact-sandbox"
    payload_path = sandbox_root / "raw-ocr" / "input" / "artifact.png"
    payload_path.parent.mkdir(parents=True)
    payload_path.write_bytes(image_bytes)
    port = _allocate_localhost_port()
    child_env = os.environ.copy()
    child_env.pop("AIDOCTOR_ENGINEERING_RAW_OCR_ARTIFACT_BASE64", None)
    child_env["PYTHONPATH"] = os.pathsep.join(
        [
            str(_REPO_ROOT / "ocr-service"),
            str(_REPO_ROOT / "contracts" / "v1" / "bindings" / "python"),
            str(_REPO_ROOT),
        ]
    )
    child_env["AIDOCTOR_ENGINEERING_RAW_OCR_PORT"] = str(port)
    child_env["AIDOCTOR_ENGINEERING_RAW_OCR_ARTIFACT_SANDBOX_ROOT"] = str(sandbox_root.resolve())
    child_env["AIDOCTOR_ENGINEERING_RAW_OCR_ARTIFACT_EXPECTED_SIZE_BYTES"] = str(len(image_bytes))
    child_env["AIDOCTOR_ENGINEERING_RAW_OCR_ARTIFACT_EXPECTED_SHA256"] = hashlib.sha256(
        image_bytes
    ).hexdigest()

    process = subprocess.Popen(
        [sys.executable, str(_LAUNCHER_PATH)],
        env=child_env,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
    )
    try:
        health_url = f"http://127.0.0.1:{port}/api/v1/runtime/health"
        deadline = time.monotonic() + _HEALTH_TIMEOUT_SECONDS
        last_error: Exception | None = None
        while time.monotonic() < deadline:
            if process.poll() is not None:
                log = process.stdout.read() if process.stdout is not None else ""
                raise AssertionError(f"process exited before health: {log}")
            try:
                with urllib.request.urlopen(health_url, timeout=1.0) as response:
                    body = json.loads(response.read().decode("utf-8"))
                if response.status == 200 and body.get("status") == "engineering_ready":
                    break
            except (urllib.error.URLError, TimeoutError, json.JSONDecodeError, OSError) as exc:
                last_error = exc
            time.sleep(0.2)
        else:
            raise AssertionError(f"health timeout: {last_error}")

        payload = _raw_ocr_context_payload()
        payload["input_refs"] = [
            {
                "ref_type": "ARTIFACT",
                "ref_id": _PROCESS_ARTIFACT_ID,
                "ref_version": _PROCESS_ARTIFACT_VERSION,
            }
        ]
        request = urllib.request.Request(
            f"http://127.0.0.1:{port}/api/v1/runtime/tools/invoke",
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Content-Type": "application/json",
                "X-Trace-Id": payload["envelope"]["trace_id"],
            },
            method="POST",
        )
        with urllib.request.urlopen(request, timeout=30) as response:
            raw_body = response.read().decode("utf-8")
            status_code = response.status
        assert status_code == 200
        body = json.loads(raw_body)
        assert body["status"] == "SUCCEEDED"
        output_text = " ".join(str(item.get("value", "")) for item in body.get("output", []))
        assert "HELLO" in output_text
        assert "OCR" in output_text
        print(f"CANONICAL_CONTROLLED_OPT_IN_TESSERACT_BINARY={tesseract_binary}", flush=True)
        print("CANONICAL_CONTROLLED_OPT_IN_REAL_PROCESS=YES", flush=True)
        print("CANONICAL_CONTROLLED_OPT_IN_REAL_HTTP=YES", flush=True)
        print("CANONICAL_CONTROLLED_OPT_IN_REAL_TESSERACT=YES", flush=True)
        print("CANONICAL_CONTROLLED_OPT_IN_SANDBOX_ARTIFACT_BACKEND=YES", flush=True)
        print("CANONICAL_CONTROLLED_OPT_IN_INPUTREF_ONLY=YES", flush=True)
        print("CANONICAL_RUNTIME_RAW_OCR_CONTROLLED_OPT_IN_PROTOCOL=PASS", flush=True)
    finally:
        if process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=5)
