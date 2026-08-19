"""工程静态工件解析：仅允许名单，不是生产对象存储，也不读取 PHI。"""

from __future__ import annotations

import hashlib
from collections.abc import Mapping
from dataclasses import dataclass

from aidoctor_shared_contracts import SourceArtifact

# 合成探测工件：不透明标识 + 确定性字节，不含患者/临床文本
SYNTHETIC_ARTIFACT_PROBE_ID = "artifact-synthetic-probe-1"
SYNTHETIC_ARTIFACT_PROBE_VERSION = 1
SYNTHETIC_ARTIFACT_PROBE_BYTES = bytes((0x00, 0x01, 0x02, 0x03, 0xFE, 0xED, 0xFA, 0xCE)) + b"OPAQUE"
SYNTHETIC_ARTIFACT_PROBE_STORAGE_REF = "logical://engineering.synthetic.artifact_probe/v1"
SYNTHETIC_ARTIFACT_PROBE_FILENAME = "synthetic-artifact-probe.bin"

ERROR_ARTIFACT_UNKNOWN = "ARTIFACT_UNKNOWN"
ERROR_ARTIFACT_VERSION_MISMATCH = "ARTIFACT_VERSION_MISMATCH"
ERROR_ARTIFACT_CHECKSUM_MISMATCH = "ARTIFACT_CHECKSUM_MISMATCH"
ERROR_ARTIFACT_SIZE_MISMATCH = "ARTIFACT_SIZE_MISMATCH"
ERROR_ARTIFACT_PHI_REJECTED = "ARTIFACT_PHI_REJECTED"
ERROR_ARTIFACT_ID_MISMATCH = "ARTIFACT_ID_MISMATCH"


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
