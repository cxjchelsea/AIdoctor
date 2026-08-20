"""非生产 engineering-only Raw OCR Runtime localhost 进程入口。

分类：NON_PRODUCTION_ENGINEERING_LOCALHOST_PROCESS_ENTRYPOINT

本模块是显式 composition root：把具体 RawOcrEngine 接到已审查的
create_engineering_raw_ocr_app(...) 工厂。
禁止模块级 FastAPI 应用实例，禁止 import 时绑定套接字或启动 uvicorn。
禁止 sys.path 改写。禁止公共监听。
禁止重建内部 Runtime 组合对象或默认应用工厂。
禁止读取 artifact payload 字节；payload 只由 ArtifactPort.resolve 打开。
"""

from __future__ import annotations

import os
import re

import uvicorn
from aidoctor_shared_contracts import SourceArtifact
from app.services.raw_ocr import RawOcrEngine
from fastapi import FastAPI

from packages.python_runtime.artifacts import (
    MAX_ARTIFACT_BYTES,
    SandboxedArtifactRecord,
    SandboxedFilesystemArtifactPort,
)
from packages.python_runtime.http.engineering_raw_ocr import create_engineering_raw_ocr_app
from packages.python_runtime.raw_ocr_adapter import RawOcrToolAdapter

PROCESS_HOST = "127.0.0.1"
PORT_ENV_NAME = "AIDOCTOR_ENGINEERING_RAW_OCR_PORT"
SANDBOX_ROOT_ENV_NAME = "AIDOCTOR_ENGINEERING_RAW_OCR_ARTIFACT_SANDBOX_ROOT"
EXPECTED_SIZE_ENV_NAME = "AIDOCTOR_ENGINEERING_RAW_OCR_ARTIFACT_EXPECTED_SIZE_BYTES"
EXPECTED_SHA256_ENV_NAME = "AIDOCTOR_ENGINEERING_RAW_OCR_ARTIFACT_EXPECTED_SHA256"
MIN_PORT = 1024
MAX_PORT = 65535
_SHA256_HEX_RE = re.compile(r"^[0-9a-f]{64}$")

_ARTIFACT_ID = "artifact-engineering-raw-ocr-process-1"
_ARTIFACT_VERSION = 1
_ARTIFACT_STORAGE_REF = (
    "artifact://engineering-runtime-process/artifact-engineering-raw-ocr-process-1"
)
_ARTIFACT_OWNER_REF = "synthetic-owner-engineering-raw-ocr-process-1"
_ARTIFACT_FILENAME = "synthetic-engineering-raw-ocr-process.png"
_ARTIFACT_CREATED_AT = "2026-08-20T05:00:00Z"
_ENGINEERING_ARTIFACT_RELATIVE_PATH = "raw-ocr/input/artifact.png"


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


def parse_engineering_sandbox_root(raw_root: str | None) -> str:
    """解析绝对且已存在的 sandbox 根目录；不得回退 cwd / 仓库根 / 临时目录。"""

    if raw_root is None or raw_root.strip() == "":
        raise EngineeringProcessBootstrapError(
            f"{SANDBOX_ROOT_ENV_NAME} is required for the engineering Raw OCR process"
        )
    if not os.path.isabs(raw_root):
        raise EngineeringProcessBootstrapError(
            f"{SANDBOX_ROOT_ENV_NAME} must be an absolute directory"
        )
    if not os.path.isdir(raw_root) or os.path.islink(raw_root):
        raise EngineeringProcessBootstrapError(
            f"{SANDBOX_ROOT_ENV_NAME} must be an existing directory"
        )
    return raw_root


def parse_engineering_expected_size(raw_size: str | None) -> int:
    """解析非负且不超过 MAX_ARTIFACT_BYTES 的期望大小。"""

    if raw_size is None or raw_size.strip() == "":
        raise EngineeringProcessBootstrapError(
            f"{EXPECTED_SIZE_ENV_NAME} is required for the engineering Raw OCR process"
        )
    try:
        size = int(raw_size, 10)
    except ValueError as exc:
        raise EngineeringProcessBootstrapError(
            f"{EXPECTED_SIZE_ENV_NAME} must be an integer"
        ) from exc
    if size < 0 or size > MAX_ARTIFACT_BYTES:
        raise EngineeringProcessBootstrapError(
            f"{EXPECTED_SIZE_ENV_NAME} must be between 0 and {MAX_ARTIFACT_BYTES}"
        )
    return size


def parse_engineering_expected_sha256(raw_digest: str | None) -> str:
    """解析规范化小写 64 位十六进制 SHA-256。"""

    if raw_digest is None or raw_digest.strip() == "":
        raise EngineeringProcessBootstrapError(
            f"{EXPECTED_SHA256_ENV_NAME} is required for the engineering Raw OCR process"
        )
    digest = raw_digest.strip().lower()
    if _SHA256_HEX_RE.fullmatch(digest) is None:
        raise EngineeringProcessBootstrapError(
            f"{EXPECTED_SHA256_ENV_NAME} must be a 64-character hex SHA-256 digest"
        )
    return digest


def _build_sandboxed_artifact_record(
    expected_size: int,
    expected_sha256: str,
) -> SandboxedArtifactRecord:
    """只用 metadata env 构造记录；不得打开 payload。"""

    metadata = SourceArtifact.model_validate(
        {
            "contract_version": "1.0.0",
            "artifact_id": _ARTIFACT_ID,
            "artifact_type": "UPLOAD",
            "owner_ref": _ARTIFACT_OWNER_REF,
            "content_type": "image/png",
            "original_filename": _ARTIFACT_FILENAME,
            "size_bytes": expected_size,
            "checksum": {"algorithm": "SHA-256", "value": expected_sha256},
            "storage_ref": _ARTIFACT_STORAGE_REF,
            "created_at": _ARTIFACT_CREATED_AT,
            "processing_status": "RECEIVED",
            "derived_artifacts": [],
            "sensitivity": "INTERNAL",
            "retention_class": "ENGINEERING_SYNTHETIC",
        }
    )
    return SandboxedArtifactRecord(
        metadata=metadata,
        relative_path=_ENGINEERING_ARTIFACT_RELATIVE_PATH,
    )


def build_engineering_raw_ocr_process_app_from_env(
    environ: dict[str, str] | None = None,
) -> FastAPI:
    """
    从工程环境构造进程应用：只读 metadata / sandbox 配置，注入已有工厂。

    不启动网络，不覆盖 OCR language，不打开 storage_ref，不读取 payload 字节。
    """

    source = os.environ if environ is None else environ
    sandbox_root = parse_engineering_sandbox_root(source.get(SANDBOX_ROOT_ENV_NAME))
    expected_size = parse_engineering_expected_size(source.get(EXPECTED_SIZE_ENV_NAME))
    expected_sha256 = parse_engineering_expected_sha256(
        source.get(EXPECTED_SHA256_ENV_NAME)
    )
    artifact_port = SandboxedFilesystemArtifactPort(
        sandbox_root,
        {
            (_ARTIFACT_ID, _ARTIFACT_VERSION): _build_sandboxed_artifact_record(
                expected_size,
                expected_sha256,
            )
        },
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
    print("ENGINEERING_RAW_OCR_ARTIFACT_BACKEND=SANDBOXED_FILESYSTEM", flush=True)
    print("ENGINEERING_RAW_OCR_ARTIFACT_SANDBOXED=YES", flush=True)
    print("ENGINEERING_RAW_OCR_ENV_BASE64_BOOTSTRAP_USED=NO", flush=True)
    uvicorn.run(application, host=PROCESS_HOST, port=port, log_level="warning")


if __name__ == "__main__":
    main()
