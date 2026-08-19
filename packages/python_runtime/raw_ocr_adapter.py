"""注入式 RAW OCR 工具适配器：只消费已解析字节，不加载 OCR 库或遗留服务。"""

from __future__ import annotations

from collections.abc import Sequence
from typing import Protocol

from aidoctor_shared_contracts import ToolContext, ToolResult

from .artifacts import ResolvedArtifact

# 稳定错误码：与注入引擎 error_code 对齐，不在此写入测试专用能力标识
ERROR_RAW_OCR_INPUT_CARDINALITY_INVALID = "RAW_OCR_INPUT_CARDINALITY_INVALID"
ERROR_RAW_OCR_OUTPUT_TOO_LONG = "RAW_OCR_OUTPUT_TOO_LONG"
ERROR_RAW_OCR_ENGINE_UNAVAILABLE = "RAW_OCR_ENGINE_UNAVAILABLE"
ERROR_RAW_OCR_EXECUTION_FAILED = "RAW_OCR_EXECUTION_FAILED"
ERROR_RAW_OCR_ADAPTER_INTERNAL = "RAW_OCR_ADAPTER_INTERNAL"
REASON_RAW_OCR_OK = "RAW_OCR_OK"
REASON_RAW_OCR_NO_RESULT = "RAW_OCR_NO_RESULT"

_MAX_NAMED_VALUE_STRING_LENGTH = 4000
_DEFAULT_STARTED_AT = "2026-08-19T12:00:00Z"
_DEFAULT_COMPLETED_AT = "2026-08-19T12:00:01Z"
_RESULT_PRODUCER = "python-runtime-raw-ocr"


class RawOcrEnginePort(Protocol):
    """窄引擎端口：只接收已解析字节，不解释存储引用或文件名。"""

    def recognize_from_bytes(self, image_bytes: bytes) -> str:
        """对图像字节执行技术识别并返回原始文本。"""


class RawOcrToolAdapter:
    """ContextToolPort 实现：单工件、注入引擎、不导入 OCR 依赖。"""

    def __init__(
        self,
        engine: RawOcrEnginePort,
        *,
        started_at: str = _DEFAULT_STARTED_AT,
        completed_at: str = _DEFAULT_COMPLETED_AT,
    ) -> None:
        self._engine = engine
        self._started_at = started_at
        self._completed_at = completed_at

    def invoke(
        self,
        context: ToolContext,
        artifacts: Sequence[ResolvedArtifact],
    ) -> ToolResult:
        """对恰好一个已解析工件调用注入引擎，并映射为冻结 ToolResult。"""

        if len(artifacts) != 1:
            return self._failure(
                context,
                status="NON_RETRYABLE_FAILURE",
                retryable=False,
                reason_code=ERROR_RAW_OCR_INPUT_CARDINALITY_INVALID,
                error_category="VALIDATION",
                error_code=ERROR_RAW_OCR_INPUT_CARDINALITY_INVALID,
                error_message="raw OCR accepts exactly one resolved artifact",
            )

        resolved = artifacts[0]
        # 明确不把 original_filename / storage_ref 当作可打开路径
        try:
            raw_text = self._engine.recognize_from_bytes(resolved.content)
        except Exception as engine_error:
            return self._map_engine_error(context, engine_error)

        if raw_text == "":
            return self._result(
                context,
                status="NO_RESULT",
                retryable=False,
                reason_code=REASON_RAW_OCR_NO_RESULT,
                output=[],
                errors=[],
            )
        if len(raw_text) > _MAX_NAMED_VALUE_STRING_LENGTH:
            return self._failure(
                context,
                status="NON_RETRYABLE_FAILURE",
                retryable=False,
                reason_code=ERROR_RAW_OCR_OUTPUT_TOO_LONG,
                error_category="VALIDATION",
                error_code=ERROR_RAW_OCR_OUTPUT_TOO_LONG,
                error_message="raw OCR output exceeds the frozen namedValue string limit",
            )
        return self._result(
            context,
            status="SUCCEEDED",
            retryable=False,
            reason_code=REASON_RAW_OCR_OK,
            output=[{"name": "raw_text", "value": raw_text}],
            errors=[],
        )

    def _map_engine_error(self, context: ToolContext, engine_error: Exception) -> ToolResult:
        """按注入引擎暴露的稳定 error_code 映射；不得导入遗留 OCR 异常类型。"""

        error_code = getattr(engine_error, "error_code", None)
        if error_code == ERROR_RAW_OCR_ENGINE_UNAVAILABLE:
            return self._failure(
                context,
                status="RETRYABLE_FAILURE",
                retryable=True,
                reason_code=ERROR_RAW_OCR_ENGINE_UNAVAILABLE,
                error_category="DEPENDENCY",
                error_code=ERROR_RAW_OCR_ENGINE_UNAVAILABLE,
                error_message="raw OCR engine is unavailable",
            )
        if error_code == ERROR_RAW_OCR_EXECUTION_FAILED:
            return self._failure(
                context,
                status="NON_RETRYABLE_FAILURE",
                retryable=False,
                reason_code=ERROR_RAW_OCR_EXECUTION_FAILED,
                error_category="INTERNAL",
                error_code=ERROR_RAW_OCR_EXECUTION_FAILED,
                error_message="raw OCR execution failed",
            )
        return self._failure(
            context,
            status="NON_RETRYABLE_FAILURE",
            retryable=False,
            reason_code=ERROR_RAW_OCR_ADAPTER_INTERNAL,
            error_category="INTERNAL",
            error_code=ERROR_RAW_OCR_ADAPTER_INTERNAL,
            error_message="raw OCR adapter encountered an unexpected engine error",
        )

    def _failure(
        self,
        context: ToolContext,
        *,
        status: str,
        retryable: bool,
        reason_code: str,
        error_category: str,
        error_code: str,
        error_message: str,
    ) -> ToolResult:
        """构造不含原文、不含字节、不含堆栈的失败 ToolResult。"""

        return self._result(
            context,
            status=status,
            retryable=retryable,
            reason_code=reason_code,
            output=[],
            errors=[
                {
                    "code": error_code,
                    "category": error_category,
                    "message": error_message,
                    "retryable": retryable,
                }
            ],
        )

    def _result(
        self,
        context: ToolContext,
        *,
        status: str,
        retryable: bool,
        reason_code: str,
        output: list,
        errors: list,
    ) -> ToolResult:
        """用上下文身份派生冻结 ToolResult；时间戳由构造注入，避免墙钟竞态。"""

        return ToolResult(
            contract_version=context.contract_version,
            envelope=context.envelope.model_copy(
                update={
                    "contract_name": "ToolResult",
                    "message_id": f"{context.envelope.message_id}.result",
                    "created_at": self._completed_at,
                    "producer": _RESULT_PRODUCER,
                }
            ),
            tool_name="raw-ocr",
            tool_version=context.capability.capability_version,
            invocation_id=f"invoke-{context.envelope.message_id}",
            status=status,
            reason_code=reason_code,
            retryable=retryable,
            output=output,
            suggested_patches=[],
            evidence_refs=[],
            errors=errors,
            started_at=self._started_at,
            completed_at=self._completed_at,
        )
