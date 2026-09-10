"""POSTFREEZE-03B-B：RawOcrToolAdapter 单元测试。

本套件在 python-safety 中运行，禁止依赖 OpenCV / NumPy / Pillow / pytesseract。
工程能力标识仅允许出现在测试，不得进入生产适配器源码。
"""

from __future__ import annotations

from aidoctor_shared_contracts import SourceArtifact, ToolContext, ToolResult

from packages.python_runtime.artifacts import ResolvedArtifact, sha256_hex
from packages.python_runtime.raw_ocr_adapter import (
    ERROR_RAW_OCR_ADAPTER_INTERNAL,
    ERROR_RAW_OCR_ENGINE_UNAVAILABLE,
    ERROR_RAW_OCR_EXECUTION_FAILED,
    ERROR_RAW_OCR_INPUT_CARDINALITY_INVALID,
    ERROR_RAW_OCR_OUTPUT_TOO_LONG,
    RawOcrToolAdapter,
)

# 测试注入专用能力标识；不得复制进生产适配器
_RAW_OCR_CAPABILITY_ID = "engineering.ocr.raw"
_RAW_OCR_CAPABILITY_VERSION = "0.0.1"
_SYNTHETIC_OCR_BYTES = b"SYNTHETIC-OCR-PROBE-BYTES"
_SYNTHETIC_OCR_TEXT = "HELLO OCR 123"
_FIXED_STARTED_AT = "2026-08-19T12:00:00Z"
_FIXED_COMPLETED_AT = "2026-08-19T12:00:01Z"


class _RecordingEngine:
    """记录传入字节并返回预定文本；不触碰真实 OCR。"""

    def __init__(self, raw_text: str) -> None:
        self.received_bytes: bytes | None = None
        self._raw_text = raw_text

    def recognize_from_bytes(self, image_bytes: bytes) -> str:
        self.received_bytes = image_bytes
        return self._raw_text


class _CodedEngineError(Exception):
    """带稳定 error_code 的合成引擎错误，模拟真实引擎边界。"""

    def __init__(self, error_code: str) -> None:
        super().__init__("synthetic engine error")
        self.error_code = error_code


class _CodedFailureEngine:
    """按稳定 error_code 失败的注入引擎。"""

    def __init__(self, error_code: str) -> None:
        self._error_code = error_code

    def recognize_from_bytes(self, image_bytes: bytes) -> str:
        del image_bytes
        raise _CodedEngineError(self._error_code)


class _UnexpectedFailureEngine:
    """无 error_code 的意外失败。"""

    def recognize_from_bytes(self, image_bytes: bytes) -> str:
        del image_bytes
        raise RuntimeError("unexpected boom")


def _synthetic_context() -> ToolContext:
    """构造冻结 schema 合法的合成 ToolContext。"""

    return ToolContext.model_validate(
        {
            "contract_version": "1.0.0",
            "envelope": {
                "contract_name": "ToolContext",
                "contract_version": "1.0.0",
                "message_id": "msg-raw-ocr-adapter-1",
                "correlation_id": "corr-raw-ocr-adapter-1",
                "trace_id": "trace-raw-ocr-adapter-1",
                "created_at": "2026-08-19T12:00:00Z",
                "producer": "python-runtime-raw-ocr-test",
                "capability_id": _RAW_OCR_CAPABILITY_ID,
                "capability_version": _RAW_OCR_CAPABILITY_VERSION,
            },
            "actor": {"actor_id": "python-runtime-raw-ocr-test", "actor_type": "SERVICE"},
            "identifiers": {
                "contract_version": "1.0.0",
                "cdp_id": "synthetic-cdp-raw-ocr-1",
            },
            "capability": {
                "capability_id": _RAW_OCR_CAPABILITY_ID,
                "capability_version": _RAW_OCR_CAPABILITY_VERSION,
            },
            "current_state_ref": {
                "cdp_id": "synthetic-cdp-raw-ocr-1",
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
                    "ref_id": "artifact-synthetic-raw-ocr-1",
                    "ref_version": 1,
                }
            ],
        }
    )


def _resolved_artifact(
    content: bytes,
    original_filename: str = "synthetic-raw-ocr-probe.bin",
) -> ResolvedArtifact:
    """构造非 PHI 合成工件；original_filename 不得被当成路径。"""

    return ResolvedArtifact(
        metadata=SourceArtifact(
            contract_version="1.0.0",
            artifact_id="artifact-synthetic-raw-ocr-1",
            artifact_type="UPLOAD",
            owner_ref="synthetic-owner-raw-ocr-1",
            content_type="application/octet-stream",
            original_filename=original_filename,
            size_bytes=len(content),
            checksum={"algorithm": "SHA-256", "value": sha256_hex(content)},
            storage_ref="artifact://engineering-synthetic/artifact-synthetic-raw-ocr-1",
            created_at="2026-08-19T12:00:00Z",
            processing_status="RECEIVED",
            derived_artifacts=[],
            sensitivity="INTERNAL",
            retention_class="ENGINEERING_SYNTHETIC",
        ),
        content=content,
    )


def _adapter(engine) -> RawOcrToolAdapter:
    """固定时间戳，避免墙钟竞态。"""

    return RawOcrToolAdapter(
        engine,
        started_at=_FIXED_STARTED_AT,
        completed_at=_FIXED_COMPLETED_AT,
    )


def test_non_empty_raw_text_succeeds_with_exact_output():
    """单工件 + 非空原文必须 SUCCEEDED，且不携带临床结构。"""

    engine = _RecordingEngine(_SYNTHETIC_OCR_TEXT)
    result = _adapter(engine).invoke(_synthetic_context(), [_resolved_artifact(_SYNTHETIC_OCR_BYTES)])
    assert isinstance(result, ToolResult)
    assert result.status == "SUCCEEDED"
    assert result.retryable is False
    assert result.reason_code == "RAW_OCR_OK"
    assert result.tool_name == "raw-ocr"
    assert result.tool_version == _RAW_OCR_CAPABILITY_VERSION
    assert result.model_dump()["output"] == [{"name": "raw_text", "value": _SYNTHETIC_OCR_TEXT}]
    assert result.suggested_patches == []
    assert result.evidence_refs == []
    assert result.errors == []
    assert result.started_at == _FIXED_STARTED_AT
    assert result.completed_at == _FIXED_COMPLETED_AT
    dumped = result.model_dump()
    assert "patient" not in str(dumped).lower()
    assert "diagnosis" not in str(dumped).lower()
    assert "indicator" not in str(dumped).lower()


def test_empty_engine_text_is_no_result_not_technical_failure():
    """引擎成功返回空串必须是 NO_RESULT，不是技术失败。"""

    result = _adapter(_RecordingEngine("")).invoke(
        _synthetic_context(),
        [_resolved_artifact(_SYNTHETIC_OCR_BYTES)],
    )
    assert result.status == "NO_RESULT"
    assert result.retryable is False
    assert result.reason_code == "RAW_OCR_NO_RESULT"
    assert result.output == []
    assert result.errors == []
    assert result.suggested_patches == []
    assert result.evidence_refs == []


def test_raw_text_length_4000_is_accepted():
    """正好 4000 字符的原文必须被接受。"""

    raw_text = "A" * 4000
    result = _adapter(_RecordingEngine(raw_text)).invoke(
        _synthetic_context(),
        [_resolved_artifact(_SYNTHETIC_OCR_BYTES)],
    )
    assert result.status == "SUCCEEDED"
    assert result.model_dump()["output"] == [{"name": "raw_text", "value": raw_text}]


def test_raw_text_length_4001_fails_without_truncation_or_leak():
    """超过 4000 不得截断，也不得把超长原文写入错误。"""

    raw_text = "B" * 4001
    result = _adapter(_RecordingEngine(raw_text)).invoke(
        _synthetic_context(),
        [_resolved_artifact(_SYNTHETIC_OCR_BYTES)],
    )
    assert result.status == "NON_RETRYABLE_FAILURE"
    assert result.retryable is False
    assert result.reason_code == ERROR_RAW_OCR_OUTPUT_TOO_LONG
    assert result.output == []
    assert result.errors[0].category == "VALIDATION"
    assert result.errors[0].code == ERROR_RAW_OCR_OUTPUT_TOO_LONG
    serialized = str(result.model_dump())
    assert raw_text not in serialized
    assert "B" * 20 not in serialized


def test_engine_unavailable_maps_to_retryable_dependency_failure():
    """稳定 UNAVAILABLE 码必须映射为可重试依赖失败。"""

    result = _adapter(_CodedFailureEngine(ERROR_RAW_OCR_ENGINE_UNAVAILABLE)).invoke(
        _synthetic_context(),
        [_resolved_artifact(_SYNTHETIC_OCR_BYTES)],
    )
    assert result.status == "RETRYABLE_FAILURE"
    assert result.retryable is True
    assert result.reason_code == ERROR_RAW_OCR_ENGINE_UNAVAILABLE
    assert result.output == []
    assert result.errors[0].category == "DEPENDENCY"
    assert result.errors[0].code == ERROR_RAW_OCR_ENGINE_UNAVAILABLE
    assert result.errors[0].retryable is True


def test_execution_failed_maps_to_non_retryable_internal_failure():
    """稳定 EXECUTION_FAILED 码必须映射为不可重试内部失败。"""

    result = _adapter(_CodedFailureEngine(ERROR_RAW_OCR_EXECUTION_FAILED)).invoke(
        _synthetic_context(),
        [_resolved_artifact(_SYNTHETIC_OCR_BYTES)],
    )
    assert result.status == "NON_RETRYABLE_FAILURE"
    assert result.retryable is False
    assert result.reason_code == ERROR_RAW_OCR_EXECUTION_FAILED
    assert result.output == []
    assert result.errors[0].category == "INTERNAL"
    assert result.errors[0].code == ERROR_RAW_OCR_EXECUTION_FAILED


def test_unexpected_engine_error_maps_to_stable_internal_failure():
    """无稳定码的意外异常不得泄漏堆栈或原文。"""

    result = _adapter(_UnexpectedFailureEngine()).invoke(
        _synthetic_context(),
        [_resolved_artifact(_SYNTHETIC_OCR_BYTES)],
    )
    assert result.status == "NON_RETRYABLE_FAILURE"
    assert result.retryable is False
    assert result.reason_code == ERROR_RAW_OCR_ADAPTER_INTERNAL
    assert result.output == []
    assert result.errors[0].category == "INTERNAL"
    assert result.errors[0].code == ERROR_RAW_OCR_ADAPTER_INTERNAL
    serialized = str(result.model_dump())
    assert "unexpected boom" not in serialized
    assert "Traceback" not in serialized


def test_zero_artifacts_is_validation_failure():
    """零工件必须失败关闭。"""

    result = _adapter(_RecordingEngine(_SYNTHETIC_OCR_TEXT)).invoke(_synthetic_context(), [])
    assert result.status == "NON_RETRYABLE_FAILURE"
    assert result.retryable is False
    assert result.reason_code == ERROR_RAW_OCR_INPUT_CARDINALITY_INVALID
    assert result.errors[0].category == "VALIDATION"


def test_multiple_artifacts_is_validation_failure():
    """多于一个工件必须失败关闭。"""

    first = _resolved_artifact(_SYNTHETIC_OCR_BYTES)
    second = _resolved_artifact(_SYNTHETIC_OCR_BYTES)
    result = _adapter(_RecordingEngine(_SYNTHETIC_OCR_TEXT)).invoke(
        _synthetic_context(),
        [first, second],
    )
    assert result.status == "NON_RETRYABLE_FAILURE"
    assert result.reason_code == ERROR_RAW_OCR_INPUT_CARDINALITY_INVALID


def test_resolved_bytes_are_passed_exactly_to_engine():
    """适配器必须把已解析字节原样交给注入引擎。"""

    engine = _RecordingEngine(_SYNTHETIC_OCR_TEXT)
    _adapter(engine).invoke(_synthetic_context(), [_resolved_artifact(_SYNTHETIC_OCR_BYTES)])
    assert engine.received_bytes == _SYNTHETIC_OCR_BYTES


def test_original_filename_is_never_used_as_filesystem_path():
    """original_filename 即使长得像路径，也只是元数据，不得被打开。"""

    path_like_name = r"C:\must-not-open\synthetic-raw-ocr-probe.png"
    artifact = _resolved_artifact(_SYNTHETIC_OCR_BYTES, original_filename=path_like_name)
    assert artifact.metadata.original_filename == path_like_name
    result = _adapter(_RecordingEngine(_SYNTHETIC_OCR_TEXT)).invoke(
        _synthetic_context(),
        [artifact],
    )
    assert result.status == "SUCCEEDED"
    serialized = str(result.model_dump())
    assert _SYNTHETIC_OCR_BYTES.hex() not in serialized
    assert "SYNTHETIC-OCR-PROBE-BYTES" not in serialized
