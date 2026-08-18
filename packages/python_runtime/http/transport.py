"""传输层身份校验与机器可读错误；不是 Shared Contracts 新语义族。"""

from __future__ import annotations

from collections.abc import Mapping
from typing import Any, Optional

from aidoctor_shared_contracts import CONTRACT_VERSION, ContractEnvelope
from pydantic import ValidationError

# POSTFREEZE-02 唯一授权的合成能力标识；其它身份一律失败关闭
AUTHORIZED_SYNTHETIC_CAPABILITY_ID = "engineering.synthetic.runtime_smoke"
HEADER_TRACE_ID = "X-Trace-Id"
HEADER_CDP_ID = "X-CDP-Id"

ERROR_CONTRACT_VERSION_MISMATCH = "CONTRACT_VERSION_MISMATCH"
ERROR_ENVELOPE_INVALID = "ENVELOPE_INVALID"
ERROR_IDENTITY_MISMATCH = "IDENTITY_MISMATCH"
ERROR_OPERATION_NOT_AUTHORIZED = "OPERATION_NOT_AUTHORIZED"


class TransportError(Exception):
    """传输局部错误；不得被解释为新的契约语义家族。"""

    def __init__(
        self,
        error_code: str,
        message: str,
        *,
        http_status: int,
        retryable: bool = False,
        correlation_id: Optional[str] = None,
        trace_id: Optional[str] = None,
        cdp_id: Optional[str] = None,
    ) -> None:
        super().__init__(message)
        self.error_code = error_code
        self.message = message
        self.http_status = http_status
        self.retryable = retryable
        self.correlation_id = correlation_id
        self.trace_id = trace_id
        self.cdp_id = cdp_id

    def to_payload(self) -> dict[str, Any]:
        """生成机器可读传输错误体；无法安全恢复的字段可省略。"""

        payload: dict[str, Any] = {
            "error_code": self.error_code,
            "retryable": self.retryable,
            "message": self.message,
        }
        if self.correlation_id is not None:
            payload["correlation_id"] = self.correlation_id
        if self.trace_id is not None:
            payload["trace_id"] = self.trace_id
        return payload


def _optional_identifier(raw_value: Any) -> Optional[str]:
    """仅在值为非空字符串时回传，避免把畸形输入写入错误信封。"""

    if isinstance(raw_value, str) and raw_value != "":
        return raw_value
    return None


def parse_contract_envelope(raw_payload: Any) -> ContractEnvelope:
    """先区分版本不匹配，再走真实 Shared Contracts 绑定校验。

    成功路径必须经过 ContractEnvelope.model_validate，禁止 model_construct。
    不得改写、升级或降级 contract_version。
    """

    if not isinstance(raw_payload, Mapping):
        raise TransportError(
            ERROR_ENVELOPE_INVALID,
            "request body must be a JSON object",
            http_status=400,
        )
    declared_version = raw_payload.get("contract_version")
    if declared_version is not None and declared_version != CONTRACT_VERSION:
        raise TransportError(
            ERROR_CONTRACT_VERSION_MISMATCH,
            (
                f"contract_version must be {CONTRACT_VERSION!r} with EXACT negotiation, "
                f"got {declared_version!r}"
            ),
            http_status=400,
            correlation_id=_optional_identifier(raw_payload.get("correlation_id")),
            trace_id=_optional_identifier(raw_payload.get("trace_id")),
        )
    try:
        return ContractEnvelope.model_validate(dict(raw_payload))
    except ValidationError as exc:
        raise TransportError(
            ERROR_ENVELOPE_INVALID,
            "request body is not a valid Shared Contracts v1 ContractEnvelope",
            http_status=400,
            correlation_id=_optional_identifier(raw_payload.get("correlation_id")),
            trace_id=_optional_identifier(raw_payload.get("trace_id")),
        ) from exc


def require_trace_identity(header_trace_id: Optional[str], envelope: ContractEnvelope) -> None:
    """X-Trace-Id 必须存在且等于信封 trace_id；调用方已提供则不得另造。"""

    if header_trace_id is None or header_trace_id == "":
        raise TransportError(
            ERROR_IDENTITY_MISMATCH,
            "X-Trace-Id is required and must equal body trace_id",
            http_status=400,
            correlation_id=envelope.correlation_id,
            trace_id=envelope.trace_id,
        )
    if header_trace_id != envelope.trace_id:
        raise TransportError(
            ERROR_IDENTITY_MISMATCH,
            "X-Trace-Id must equal ContractEnvelope.trace_id",
            http_status=400,
            correlation_id=envelope.correlation_id,
            trace_id=envelope.trace_id,
        )


def require_authorized_synthetic_operation(envelope: ContractEnvelope) -> None:
    """本批只授权合成工程 smoke 能力，其它操作失败关闭。"""

    if envelope.capability_id != AUTHORIZED_SYNTHETIC_CAPABILITY_ID:
        raise TransportError(
            ERROR_OPERATION_NOT_AUTHORIZED,
            "only engineering.synthetic.runtime_smoke is authorized in POSTFREEZE-02",
            http_status=403,
            correlation_id=envelope.correlation_id,
            trace_id=envelope.trace_id,
        )
