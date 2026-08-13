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


class ProviderAdapterError(ValueError):
    """One stable provider-local failure with a machine-readable code."""

    def __init__(self, code: ProviderAdapterErrorCode, detail: str) -> None:
        self.code = code
        # 错误详情必须有界且确定性，禁止泄漏完整 prompt/payload/路径/密钥
        self.detail = detail
        super().__init__(f"{code.value}: {detail}")
