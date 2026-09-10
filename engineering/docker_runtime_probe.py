"""POSTFREEZE-03C container-internal engineering Runtime proof.

Starts engineering/raw_ocr_runtime_process.py inside the current container,
then performs localhost health + ToolContext invoke against a sandbox file.
Not a TestClient proof. Not a production / canonical / host-publish proof.
Fixture text is synthetic HELLO OCR; REAL_IMAGE_OCR_VERIFIED remains NO.
"""

from __future__ import annotations

import hashlib
import io
import json
import os
import shutil
import subprocess
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

PROCESS_HOST = "127.0.0.1"
DEFAULT_PORT = "8099"
REQUIRED_LANGUAGES = ("eng", "chi_sim")
REQUIRED_TOKENS = ("HELLO", "OCR")
SYNTHETIC_TEXT = "HELLO OCR 123"
ARTIFACT_ID = "artifact-engineering-raw-ocr-process-1"
ARTIFACT_VERSION = 1
HEALTH_TIMEOUT_SECONDS = 30.0
INVOKE_TIMEOUT_SECONDS = 30.0
_LAUNCHER = Path(__file__).resolve().parent / "raw_ocr_runtime_process.py"


def _fail(message: str) -> None:
    print(f"ENGINEERING_DOCKER_RUNTIME_PROTOCOL=FAIL reason={message}", flush=True)
    raise SystemExit(1)


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
    if completed.returncode != 0:
        _fail(f"tesseract --list-langs failed rc={completed.returncode}")
    combined = "\n".join(part for part in (completed.stdout, completed.stderr) if part)
    languages: set[str] = set()
    for raw_line in combined.splitlines():
        name = raw_line.strip()
        if not name or name.lower().startswith("list of"):
            continue
        languages.add(name)
    return languages


def _render_synthetic_png() -> bytes:
    canvas = Image.new("RGB", (1600, 400), color=(255, 255, 255))
    drawer = ImageDraw.Draw(canvas)
    font = ImageFont.load_default(size=96)
    bbox = drawer.textbbox((0, 0), SYNTHETIC_TEXT, font=font)
    width = bbox[2] - bbox[0]
    height = bbox[3] - bbox[1]
    left = max((1600 - width) // 2, 40)
    top = max((400 - height) // 2, 40)
    drawer.text((left, top), SYNTHETIC_TEXT, fill=(0, 0, 0), font=font)
    buffer = io.BytesIO()
    canvas.save(buffer, format="PNG")
    return buffer.getvalue()


def _tool_context_payload() -> dict:
    return {
        "contract_version": "1.0.0",
        "envelope": {
            "contract_name": "ToolContext",
            "contract_version": "1.0.0",
            "message_id": "msg-engineering-docker-runtime-1",
            "correlation_id": "corr-engineering-docker-runtime-1",
            "trace_id": "trace-engineering-docker-runtime-1",
            "created_at": "2026-08-20T09:00:00Z",
            "producer": "python-runtime-engineering-docker-runtime",
            "capability_id": "engineering.ocr.raw",
            "capability_version": "0.0.1",
        },
        "actor": {
            "actor_id": "python-runtime-engineering-docker-runtime",
            "actor_type": "SERVICE",
        },
        "identifiers": {
            "contract_version": "1.0.0",
            "cdp_id": "synthetic-cdp-engineering-docker-runtime-1",
        },
        "capability": {
            "capability_id": "engineering.ocr.raw",
            "capability_version": "0.0.1",
        },
        "current_state_ref": {
            "cdp_id": "synthetic-cdp-engineering-docker-runtime-1",
            "version": 1,
            "read_fields": [],
        },
        "authorization_scope": {"granted": [], "requested": []},
        "deadline": "2026-08-20T09:05:00Z",
        "locale": "und",
        "requested_operation": "RAW_OCR_RECOGNIZE",
        "input_refs": [
            {
                "ref_type": "ARTIFACT",
                "ref_id": ARTIFACT_ID,
                "ref_version": ARTIFACT_VERSION,
            }
        ],
    }


def _wait_health(port: str, process: subprocess.Popen[str]) -> None:
    url = f"http://{PROCESS_HOST}:{port}/api/v1/runtime/health"
    deadline = time.monotonic() + HEALTH_TIMEOUT_SECONDS
    last_error: Exception | None = None
    while time.monotonic() < deadline:
        if process.poll() is not None:
            log = ""
            if process.stdout is not None:
                log = process.stdout.read()
            _fail(f"engineering Runtime process exited before health: {log}")
        try:
            with urllib.request.urlopen(url, timeout=1.0) as response:
                body = json.loads(response.read().decode("utf-8"))
            if response.status == 200 and body.get("status") == "engineering_ready":
                print("ENGINEERING_DOCKER_RUNTIME_HEALTH=PASS", flush=True)
                return
        except (urllib.error.URLError, TimeoutError, json.JSONDecodeError, OSError) as exc:
            last_error = exc
        time.sleep(0.2)
    _fail(f"health timeout at {url}: {last_error}")


def main() -> None:
    if not _LAUNCHER.is_file():
        _fail("engineering/raw_ocr_runtime_process.py is missing")
    if "0.0.0.0" in _LAUNCHER.read_text(encoding="utf-8"):
        _fail("launcher contains public bind")

    python_version = sys.version.split()[0]
    if not python_version.startswith("3.11."):
        _fail(f"expected Python 3.11.x, got {python_version}")
    print(f"ENGINEERING_DOCKER_RUNTIME_PYTHON_VERSION={python_version}", flush=True)

    tesseract_binary = shutil.which("tesseract")
    if tesseract_binary is None:
        _fail("tesseract binary not found")
    languages = _list_tesseract_languages()
    missing = [name for name in REQUIRED_LANGUAGES if name not in languages]
    if missing:
        _fail(f"missing tesseract languages: {missing}")
    print(f"ENGINEERING_DOCKER_RUNTIME_TESSERACT_BINARY={tesseract_binary}", flush=True)
    print("ENGINEERING_DOCKER_RUNTIME_LANG_ENG=YES", flush=True)
    print("ENGINEERING_DOCKER_RUNTIME_LANG_CHI_SIM=YES", flush=True)

    sandbox_root = Path("/tmp/aidoctor-03c-artifact-sandbox")
    payload_path = sandbox_root / "raw-ocr" / "input" / "artifact.png"
    payload_path.parent.mkdir(parents=True, exist_ok=True)
    content = _render_synthetic_png()
    payload_path.write_bytes(content)

    port = os.environ.get("AIDOCTOR_ENGINEERING_RAW_OCR_PORT", DEFAULT_PORT)
    child_env = os.environ.copy()
    child_env.pop("AIDOCTOR_ENGINEERING_RAW_OCR_ARTIFACT_BASE64", None)
    child_env["AIDOCTOR_ENGINEERING_RAW_OCR_PORT"] = port
    child_env["AIDOCTOR_ENGINEERING_RAW_OCR_ARTIFACT_SANDBOX_ROOT"] = str(sandbox_root)
    child_env["AIDOCTOR_ENGINEERING_RAW_OCR_ARTIFACT_EXPECTED_SIZE_BYTES"] = str(len(content))
    child_env["AIDOCTOR_ENGINEERING_RAW_OCR_ARTIFACT_EXPECTED_SHA256"] = hashlib.sha256(
        content
    ).hexdigest()

    print("ENGINEERING_DOCKER_RUNTIME_ARTIFACT_BACKEND=SANDBOXED_FILESYSTEM", flush=True)
    print("ENGINEERING_DOCKER_RUNTIME_ENV_BASE64_BOOTSTRAP_USED=NO", flush=True)
    print("ENGINEERING_DOCKER_RUNTIME_LOCALHOST_ONLY=YES", flush=True)
    print("ENGINEERING_DOCKER_RUNTIME_HOST=127.0.0.1", flush=True)

    process = subprocess.Popen(
        [sys.executable, str(_LAUNCHER)],
        env=child_env,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
    )
    try:
        _wait_health(port, process)
        payload = _tool_context_payload()
        request = urllib.request.Request(
            f"http://{PROCESS_HOST}:{port}/api/v1/runtime/tools/invoke",
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Content-Type": "application/json",
                "X-Trace-Id": payload["envelope"]["trace_id"],
            },
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=INVOKE_TIMEOUT_SECONDS) as response:
                raw_body = response.read().decode("utf-8")
                status_code = response.status
        except urllib.error.HTTPError as exc:
            _fail(f"invoke HTTP {exc.code}: {exc.read().decode('utf-8', errors='replace')}")
        if status_code != 200:
            _fail(f"invoke HTTP {status_code}: {raw_body}")
        body = json.loads(raw_body)
        if body.get("status") != "SUCCEEDED":
            _fail(f"tool status={body.get('status')}")
        output_text = " ".join(str(item.get("value", "")) for item in body.get("output", []))
        if "HELLO" not in output_text:
            _fail("HELLO token missing from ToolResult")
        if "OCR" not in output_text:
            _fail("OCR token missing from ToolResult")
        print("ENGINEERING_DOCKER_RUNTIME_REAL_HTTP=YES", flush=True)
        print("ENGINEERING_DOCKER_RUNTIME_TOKEN_HELLO=YES", flush=True)
        print("ENGINEERING_DOCKER_RUNTIME_TOKEN_OCR=YES", flush=True)
        print("ENGINEERING_DOCKER_RUNTIME_TOOL_RESULT=PASS", flush=True)
        print("ENGINEERING_DOCKER_RUNTIME_PROTOCOL=PASS", flush=True)
        print("REAL_IMAGE_OCR_VERIFIED=NO", flush=True)
    finally:
        if process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=5)


if __name__ == "__main__":
    main()
