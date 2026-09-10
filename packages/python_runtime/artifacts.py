"""工程静态工件解析：有界允许名单或有界 sandbox，不是生产对象存储，也不读取 PHI。"""

from __future__ import annotations

import hashlib
import os
import stat
from collections.abc import Mapping
from dataclasses import dataclass
from pathlib import Path, PurePosixPath
from urllib.parse import urlsplit

from aidoctor_shared_contracts import SourceArtifact

MAX_ARTIFACT_BYTES = 2 * 1024 * 1024

# 合成探测工件：不透明标识 + 确定性字节，不含患者/临床文本
SYNTHETIC_ARTIFACT_PROBE_ID = "artifact-synthetic-probe-1"
SYNTHETIC_ARTIFACT_PROBE_VERSION = 1
SYNTHETIC_ARTIFACT_PROBE_BYTES = bytes((0x00, 0x01, 0x02, 0x03, 0xFE, 0xED, 0xFA, 0xCE)) + b"OPAQUE"
# 不透明逻辑引用：必须落在冻结 source-artifact.schema.json 允许的 scheme 内
SYNTHETIC_ARTIFACT_PROBE_STORAGE_REF = "artifact://engineering-synthetic/artifact-synthetic-probe-1"
SYNTHETIC_ARTIFACT_PROBE_FILENAME = "synthetic-artifact-probe.bin"

ERROR_ARTIFACT_UNKNOWN = "ARTIFACT_UNKNOWN"
ERROR_ARTIFACT_VERSION_MISMATCH = "ARTIFACT_VERSION_MISMATCH"
ERROR_ARTIFACT_CHECKSUM_MISMATCH = "ARTIFACT_CHECKSUM_MISMATCH"
ERROR_ARTIFACT_SIZE_MISMATCH = "ARTIFACT_SIZE_MISMATCH"
ERROR_ARTIFACT_PHI_REJECTED = "ARTIFACT_PHI_REJECTED"
ERROR_ARTIFACT_ID_MISMATCH = "ARTIFACT_ID_MISMATCH"
ERROR_ARTIFACT_SANDBOX_ROOT_INVALID = "ARTIFACT_SANDBOX_ROOT_INVALID"
ERROR_ARTIFACT_PATH_INVALID = "ARTIFACT_PATH_INVALID"
ERROR_ARTIFACT_PATH_ESCAPE = "ARTIFACT_PATH_ESCAPE"
ERROR_ARTIFACT_SYMLINK_REJECTED = "ARTIFACT_SYMLINK_REJECTED"
ERROR_ARTIFACT_FILE_MISSING = "ARTIFACT_FILE_MISSING"
ERROR_ARTIFACT_NOT_REGULAR_FILE = "ARTIFACT_NOT_REGULAR_FILE"
ERROR_ARTIFACT_STORAGE_REF_UNSUPPORTED = "ARTIFACT_STORAGE_REF_UNSUPPORTED"
ERROR_ARTIFACT_TOO_LARGE = "ARTIFACT_TOO_LARGE"
ERROR_ARTIFACT_IO_FAILED = "ARTIFACT_IO_FAILED"

_ALLOWED_SANDBOX_STORAGE_SCHEME = "artifact"


class ArtifactResolutionError(Exception):
    """允许名单解析失败；不得被解释为已访问外部存储。"""

    def __init__(self, error_code: str, message: str) -> None:
        super().__init__(message)
        self.error_code = error_code
        self.message = message


@dataclass(frozen=True)
class ResolvedArtifact:
    """解析结果：复用 Shared Contracts SourceArtifact 元数据，外加字节内容。"""

    metadata: SourceArtifact
    content: bytes


@dataclass(frozen=True)
class StaticArtifactRecord:
    """内存允许名单记录；storage_ref / original_filename 不得当路径解释。"""

    metadata: SourceArtifact
    content: bytes


def sha256_hex(content: bytes) -> str:
    """计算确定性 SHA-256 十六进制摘要；调用方不得记录原始字节。"""

    return hashlib.sha256(content).hexdigest()


def _checksum_fields(checksum: object) -> tuple[str, str]:
    """读取 SourceArtifact.checksum 的算法与摘要，兼容模型实例与映射。"""

    if isinstance(checksum, Mapping):
        return str(checksum.get("algorithm", "")), str(checksum.get("value", ""))
    algorithm = getattr(checksum, "algorithm", "")
    value = getattr(checksum, "value", "")
    return str(algorithm), str(value)


def build_synthetic_probe_metadata() -> SourceArtifact:
    """构造非 PHI 合成 SourceArtifact；标识与文件名均为不透明工程值。"""

    content = SYNTHETIC_ARTIFACT_PROBE_BYTES
    return SourceArtifact(
        contract_version="1.0.0",
        artifact_id=SYNTHETIC_ARTIFACT_PROBE_ID,
        artifact_type="UPLOAD",
        owner_ref="synthetic-owner-artifact-probe-1",
        content_type="application/octet-stream",
        original_filename=SYNTHETIC_ARTIFACT_PROBE_FILENAME,
        size_bytes=len(content),
        checksum={"algorithm": "SHA-256", "value": sha256_hex(content)},
        storage_ref=SYNTHETIC_ARTIFACT_PROBE_STORAGE_REF,
        created_at="2026-08-19T00:00:00Z",
        processing_status="RECEIVED",
        derived_artifacts=[],
        sensitivity="INTERNAL",
        retention_class="ENGINEERING_SYNTHETIC",
    )


class StaticAllowlistArtifactPort:
    """内存静态允许名单 ArtifactPort；不访问 S3 / 数据库 / HTTP / 任意文件系统。"""

    def __init__(self, records: Mapping[tuple[str, int], StaticArtifactRecord]) -> None:
        self._records = dict(records)

    @classmethod
    def for_synthetic_probe(cls) -> StaticAllowlistArtifactPort:
        """返回仅含合成探测记录的解析器，供测试组合注入。"""

        metadata = build_synthetic_probe_metadata()
        record = StaticArtifactRecord(metadata=metadata, content=SYNTHETIC_ARTIFACT_PROBE_BYTES)
        return cls({(SYNTHETIC_ARTIFACT_PROBE_ID, SYNTHETIC_ARTIFACT_PROBE_VERSION): record})

    def resolve(self, ref_id: str, ref_version: int) -> ResolvedArtifact:
        """按 (ref_id, ref_version) 精确解析，并校验大小、校验和、非 PHI 与标识一致。"""

        known_versions = [version for (record_id, version) in self._records if record_id == ref_id]
        if not known_versions:
            raise ArtifactResolutionError(
                ERROR_ARTIFACT_UNKNOWN,
                "artifact ref is not on the engineering allowlist",
            )
        record = self._records.get((ref_id, ref_version))
        if record is None:
            raise ArtifactResolutionError(
                ERROR_ARTIFACT_VERSION_MISMATCH,
                "requested artifact version is not allowlisted",
            )

        # 明确不把 original_filename / storage_ref 当作可打开路径
        metadata = record.metadata
        content = record.content
        if metadata.artifact_id != ref_id:
            raise ArtifactResolutionError(
                ERROR_ARTIFACT_ID_MISMATCH,
                "artifact_id does not match requested ref_id",
            )
        if metadata.sensitivity == "PHI":
            raise ArtifactResolutionError(
                ERROR_ARTIFACT_PHI_REJECTED,
                "PHI sensitivity is rejected in POSTFREEZE-03B-A",
            )
        if metadata.size_bytes != len(content):
            raise ArtifactResolutionError(
                ERROR_ARTIFACT_SIZE_MISMATCH,
                "size_bytes does not match actual content length",
            )
        expected_digest = sha256_hex(content)
        checksum_algorithm, checksum_value = _checksum_fields(metadata.checksum)
        if checksum_algorithm != "SHA-256" or checksum_value != expected_digest:
            raise ArtifactResolutionError(
                ERROR_ARTIFACT_CHECKSUM_MISMATCH,
                "SHA-256 checksum does not match actual content",
            )
        return ResolvedArtifact(metadata=metadata, content=content)


@dataclass(frozen=True)
class SandboxedArtifactRecord:
    """有界 sandbox 记录；relative_path 来自 composition，不得由 InputRef 推导。"""

    metadata: SourceArtifact
    relative_path: str


def _raise_resolution(error_code: str, message: str) -> None:
    """抛出不含主机路径的解析错误。"""

    raise ArtifactResolutionError(error_code, message)


def _posix_relative_parts(relative_path: str) -> tuple[str, ...]:
    """解析内部 POSIX 相对路径；拒绝绝对、遍历、盘符、URI 与反斜杠。"""

    if not isinstance(relative_path, str) or relative_path.strip() == "":
        _raise_resolution(ERROR_ARTIFACT_PATH_INVALID, "artifact relative path is invalid")
    if "\\" in relative_path or relative_path.startswith("//"):
        _raise_resolution(ERROR_ARTIFACT_PATH_INVALID, "artifact relative path is invalid")
    if "://" in relative_path:
        _raise_resolution(ERROR_ARTIFACT_PATH_INVALID, "artifact relative path is invalid")
    if len(relative_path) >= 2 and relative_path[1] == ":":
        _raise_resolution(ERROR_ARTIFACT_PATH_INVALID, "artifact relative path is invalid")
    if relative_path.startswith("/"):
        _raise_resolution(ERROR_ARTIFACT_PATH_INVALID, "artifact relative path is invalid")
    posix_path = PurePosixPath(relative_path)
    if posix_path.is_absolute() or posix_path.anchor:
        _raise_resolution(ERROR_ARTIFACT_PATH_INVALID, "artifact relative path is invalid")
    parts = posix_path.parts
    if not parts or any(part in {"", ".", ".."} for part in parts):
        _raise_resolution(ERROR_ARTIFACT_PATH_INVALID, "artifact relative path is invalid")
    return parts


def _require_sandbox_root(sandbox_root: str | os.PathLike[str]) -> Path:
    """要求绝对、已存在、非符号链接目录；不得回退到 cwd / 仓库根 / 临时默认。"""

    raw_root = os.fspath(sandbox_root)
    if raw_root.strip() == "":
        _raise_resolution(ERROR_ARTIFACT_SANDBOX_ROOT_INVALID, "sandbox root is invalid")
    root_path = Path(raw_root)
    if not root_path.is_absolute():
        _raise_resolution(ERROR_ARTIFACT_SANDBOX_ROOT_INVALID, "sandbox root is invalid")
    try:
        if root_path.is_symlink():
            _raise_resolution(ERROR_ARTIFACT_SYMLINK_REJECTED, "sandbox symlink is rejected")
        if not root_path.exists() or not root_path.is_dir():
            _raise_resolution(ERROR_ARTIFACT_SANDBOX_ROOT_INVALID, "sandbox root is invalid")
        return root_path
    except ArtifactResolutionError:
        raise
    except OSError:
        _raise_resolution(ERROR_ARTIFACT_SANDBOX_ROOT_INVALID, "sandbox root is invalid")


def _storage_ref_scheme(storage_ref: object) -> str:
    """读取 storage_ref scheme 供策略检查；绝不打开该引用。"""

    return urlsplit(str(storage_ref)).scheme.lower()


class SandboxedFilesystemArtifactPort:
    """有界本地 sandbox ArtifactPort；只按 (ref_id, ref_version) 读取预先登记的文件。"""

    def __init__(
        self,
        sandbox_root: str | os.PathLike[str],
        records: Mapping[tuple[str, int], SandboxedArtifactRecord],
    ) -> None:
        self._root = _require_sandbox_root(sandbox_root)
        self._records = dict(records)

    def resolve(self, ref_id: str, ref_version: int) -> ResolvedArtifact:
        """按不透明引用解析，并从 sandbox 读取真实常规文件字节。"""

        known_versions = [version for (record_id, version) in self._records if record_id == ref_id]
        if not known_versions:
            _raise_resolution(
                ERROR_ARTIFACT_UNKNOWN,
                "artifact ref is not on the engineering allowlist",
            )
        record = self._records.get((ref_id, ref_version))
        if record is None:
            _raise_resolution(
                ERROR_ARTIFACT_VERSION_MISMATCH,
                "requested artifact version is not allowlisted",
            )

        metadata = record.metadata
        if _storage_ref_scheme(metadata.storage_ref) != _ALLOWED_SANDBOX_STORAGE_SCHEME:
            _raise_resolution(
                ERROR_ARTIFACT_STORAGE_REF_UNSUPPORTED,
                "storage_ref scheme is not allowed for this ArtifactPort",
            )
        if metadata.artifact_id != ref_id:
            _raise_resolution(
                ERROR_ARTIFACT_ID_MISMATCH,
                "artifact_id does not match requested ref_id",
            )
        if metadata.sensitivity == "PHI":
            _raise_resolution(
                ERROR_ARTIFACT_PHI_REJECTED,
                "PHI sensitivity is rejected in POSTFREEZE-03B-I",
            )

        parts = _posix_relative_parts(record.relative_path)
        content = self._read_contained_regular_file(parts)
        if len(content) > MAX_ARTIFACT_BYTES:
            _raise_resolution(
                ERROR_ARTIFACT_TOO_LARGE,
                "artifact exceeds the engineering size bound",
            )
        if metadata.size_bytes != len(content):
            _raise_resolution(
                ERROR_ARTIFACT_SIZE_MISMATCH,
                "size_bytes does not match actual content length",
            )
        expected_digest = sha256_hex(content)
        checksum_algorithm, checksum_value = _checksum_fields(metadata.checksum)
        if (
            checksum_algorithm != "SHA-256"
            or checksum_value.lower() != expected_digest
        ):
            _raise_resolution(
                ERROR_ARTIFACT_CHECKSUM_MISMATCH,
                "SHA-256 checksum does not match actual content",
            )
        return ResolvedArtifact(metadata=metadata, content=content)

    def _read_contained_regular_file(self, parts: tuple[str, ...]) -> bytes:
        """在规范化 containment 与 symlink 拒绝后读取最多 MAX+1 字节。"""

        try:
            canonical_root = self._root.resolve(strict=True)
        except OSError:
            _raise_resolution(ERROR_ARTIFACT_SANDBOX_ROOT_INVALID, "sandbox root is invalid")

        cursor = self._root
        for part in parts:
            cursor = cursor.joinpath(part)
            try:
                if cursor.is_symlink():
                    _raise_resolution(
                        ERROR_ARTIFACT_SYMLINK_REJECTED,
                        "sandbox symlink is rejected",
                    )
                if not cursor.exists():
                    _raise_resolution(
                        ERROR_ARTIFACT_FILE_MISSING,
                        "sandboxed artifact file is missing",
                    )
            except ArtifactResolutionError:
                raise
            except OSError:
                _raise_resolution(ERROR_ARTIFACT_IO_FAILED, "sandboxed artifact read failed")

        try:
            file_stat = cursor.lstat()
            if stat.S_ISLNK(file_stat.st_mode):
                _raise_resolution(
                    ERROR_ARTIFACT_SYMLINK_REJECTED,
                    "sandbox symlink is rejected",
                )
            if not stat.S_ISREG(file_stat.st_mode):
                _raise_resolution(
                    ERROR_ARTIFACT_NOT_REGULAR_FILE,
                    "sandboxed artifact is not a regular file",
                )
            if file_stat.st_size > MAX_ARTIFACT_BYTES:
                _raise_resolution(
                    ERROR_ARTIFACT_TOO_LARGE,
                    "artifact exceeds the engineering size bound",
                )
            canonical_candidate = cursor.resolve(strict=True)
            canonical_candidate.relative_to(canonical_root)
        except ArtifactResolutionError:
            raise
        except ValueError:
            _raise_resolution(
                ERROR_ARTIFACT_PATH_ESCAPE,
                "sandboxed artifact escapes the configured root",
            )
        except OSError:
            _raise_resolution(ERROR_ARTIFACT_IO_FAILED, "sandboxed artifact read failed")

        try:
            with cursor.open("rb") as handle:
                content = handle.read(MAX_ARTIFACT_BYTES + 1)
        except OSError:
            _raise_resolution(ERROR_ARTIFACT_IO_FAILED, "sandboxed artifact read failed")
        if len(content) > MAX_ARTIFACT_BYTES:
            _raise_resolution(
                ERROR_ARTIFACT_TOO_LARGE,
                "artifact exceeds the engineering size bound",
            )
        return content
