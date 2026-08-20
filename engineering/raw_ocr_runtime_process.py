"""非生产 engineering-only Raw OCR Runtime localhost 进程入口。

分类：NON_PRODUCTION_ENGINEERING_LOCALHOST_PROCESS_ENTRYPOINT

本模块是显式 composition root：把具体 RawOcrEngine 接到已审查的
create_engineering_raw_ocr_app(...) 工厂。
禁止模块级 FastAPI 应用实例，禁止 import 时绑定套接字或启动 uvicorn。
禁止 sys.path 改写。禁止公共监听。
禁止重建内部 Runtime 组合对象或默认应用工厂。
"""

from __future__ import annotations

import base64
import os

import uvicorn
from aidoctor_shared_contracts import SourceArtifact
from app.services.raw_ocr import RawOcrEngine
from fastapi import FastAPI

from packages.python_runtime.artifacts import (
    StaticAllowlistArtifactPort,
    StaticArtifactRecord,
    sha256_hex,
)
from packages.python_runtime.http.engineering_raw_ocr import create_engineering_raw_ocr_app
from packages.python_runtime.raw_ocr_adapter import RawOcrToolAdapter

PROCESS_HOST = "127.0.0.1"
PORT_ENV_NAME = "AIDOCTOR_ENGINEERING_RAW_OCR_PORT"
ARTIFACT_BASE64_ENV_NAME = "AIDOCTOR_ENGINEERING_RAW_OCR_ARTIFACT_BASE64"
MAX_ARTIFACT_BYTES = 2 * 1024 * 1024
MIN_PORT = 1024
MAX_PORT = 65535

_ARTIFACT_ID = "artifact-engineering-raw-ocr-process-1"
_ARTIFACT_VERSION = 1
_ARTIFACT_STORAGE_REF = (
    "artifact://engineering-runtime-process/artifact-engineering-raw-ocr-process-1"
)
_ARTIFACT_OWNER_REF = "synthetic-owner-engineering-raw-ocr-process-1"
_ARTIFACT_FILENAME = "synthetic-engineering-raw-ocr-process.png"
_ARTIFACT_CREATED_AT = "2026-08-20T05:00:00Z"


class EngineeringProcessBootstrapError(Exception):
    """工程进程启动配置失败；不得被解释为已访问外部存储。"""


def parse_engineering_process_port(raw_port: str | None) -> int:
    """解析工程端口；缺失或越界必须失败关闭。host 不由本函数决定。"""

    if raw_port is None or raw_port.strip() == "":
        raise EngineeringProcessBootstrapError(
            f"{PORT_ENV_NAME} is required for the engineering Raw OCR process"
        )
    try:
        port = int(raw_port, 10)
    except ValueError as exc:
        raise EngineeringProcessBootstrapError(
            f"{PORT_ENV_NAME} must be an integer"
        ) from exc
    if port < MIN_PORT or port > MAX_PORT:
        raise EngineeringProcessBootstrapError(
            f"{PORT_ENV_NAME} must be between {MIN_PORT} and {MAX_PORT}"
        )
    return port


def decode_engineering_artifact_bytes(raw_base64: str | None) -> bytes:
    """从环境 bootstrap 解码合成工件字节；不打开文件、URL 或 storage_ref。"""

    if raw_base64 is None or raw_base64.strip() == "":
        raise EngineeringProcessBootstrapError(
            f"{ARTIFACT_BASE64_ENV_NAME} is required for the engineering Raw OCR process"
        )
    try:
        decoded = base64.b64decode(raw_base64, validate=True)
    except (ValueError, TypeError) as exc:
        raise EngineeringProcessBootstrapError(
            "engineering artifact bootstrap is not valid base64"
        ) from exc
    if decoded == b"":
        raise EngineeringProcessBootstrapError(
            "engineering artifact bootstrap decoded to empty bytes"
        )
    if len(decoded) > MAX_ARTIFACT_BYTES:
        raise EngineeringProcessBootstrapError(
            f"engineering artifact bootstrap exceeds {MAX_ARTIFACT_BYTES} bytes"
        )
    return decoded


def _build_static_artifact_record(content: bytes) -> StaticArtifactRecord:
    """按实际字节计算 size/SHA-256，使用固定非 PHI 工程元数据。"""

    metadata = SourceArtifact.model_validate(
        {
            "contract_version": "1.0.0",
            "artifact_id": _ARTIFACT_ID,
            "artifact_type": "UPLOAD",
            "owner_ref": _ARTIFACT_OWNER_REF,
            "content_type": "image/png",
            "original_filename": _ARTIFACT_FILENAME,
            "size_bytes": len(content),
            "checksum": {"algorithm": "SHA-256", "value": sha256_hex(content)},
            "storage_ref": _ARTIFACT_STORAGE_REF,
            "created_at": _ARTIFACT_CREATED_AT,
            "processing_status": "RECEIVED",
            "derived_artifacts": [],
            "sensitivity": "INTERNAL",
            "retention_class": "ENGINEERING_SYNTHETIC",
        }
    )
    return StaticArtifactRecord(metadata=metadata, content=content)


def build_engineering_raw_ocr_process_app_from_env(
    environ: dict[str, str] | None = None,
) -> FastAPI:
    """
    从工程环境构造进程应用：解码 bootstrap 字节，注入已有工厂。

    不启动网络，不覆盖 OCR language，不打开 storage_ref。
    """

    source = os.environ if environ is None else environ
    content = decode_engineering_artifact_bytes(source.get(ARTIFACT_BASE64_ENV_NAME))
    artifact_port = StaticAllowlistArtifactPort(
        {(_ARTIFACT_ID, _ARTIFACT_VERSION): _build_static_artifact_record(content)}
    )
    return create_engineering_raw_ocr_app(
        raw_ocr_tool_port=RawOcrToolAdapter(RawOcrEngine()),
        artifact_port=artifact_port,
    )


def main() -> None:
    """显式启动 localhost-only uvicorn；import 本模块不会进入此函数。"""

    try:
        port = parse_engineering_process_port(os.environ.get(PORT_ENV_NAME))
        application = build_engineering_raw_ocr_process_app_from_env()
    except EngineeringProcessBootstrapError as exc:
        raise SystemExit(str(exc)) from exc

    print("ENGINEERING_RAW_OCR_RUNTIME_PROCESS_HOST=127.0.0.1", flush=True)
    print("ENGINEERING_RAW_OCR_RUNTIME_PROCESS_LOCALHOST_ONLY=YES", flush=True)
    uvicorn.run(application, host=PROCESS_HOST, port=port, log_level="warning")


if __name__ == "__main__":
    main()
