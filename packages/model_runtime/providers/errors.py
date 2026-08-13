"""Deterministic ProviderAdapter boundary errors."""

from __future__ import annotations

from enum import Enum


class ProviderAdapterErrorCode(str, Enum):
    """Machine-readable provider-boundary failure codes (not real-provider taxonomy)."""

    PROVIDER_MISMATCH = "PROVIDER_MISMATCH"
    FIXTURE_NOT_FOUND = "FIXTURE_NOT_FOUND"
    INVALID_FIXTURE = "INVALID_FIXTURE"
    INVOCATION_RESULT_INVALID = "INVOCATION_RESULT_INVALID"
    CONTRACT_MISMATCH = "CONTRACT_MISMATCH"


# 固定安全详情：禁止调用方注入 prompt/payload/secret/路径
_SAFE_ERROR_DETAILS: dict[ProviderAdapterErrorCode, str] = {
    ProviderAdapterErrorCode.PROVIDER_MISMATCH: "provider identity mismatch",
    ProviderAdapterErrorCode.FIXTURE_NOT_FOUND: "fixture is not registered",
    ProviderAdapterErrorCode.INVALID_FIXTURE: "fixture catalog or fixture row is invalid",
    ProviderAdapterErrorCode.INVOCATION_RESULT_INVALID: "invocation result is incompatible",
    ProviderAdapterErrorCode.CONTRACT_MISMATCH: "output contract mismatch",
}


class ProviderAdapterError(ValueError):
    """One stable provider-local failure with a fixed safe detail string."""

    def __init__(self, code: ProviderAdapterErrorCode) -> None:
        if not isinstance(code, ProviderAdapterErrorCode):
            raise TypeError("code must be ProviderAdapterErrorCode")
        self.code = code
        # 公共构造仅接受 code；详情由内部映射决定，有界且确定性
        self.detail = _SAFE_ERROR_DETAILS[code]
        super().__init__(f"{code.value}: {self.detail}")
