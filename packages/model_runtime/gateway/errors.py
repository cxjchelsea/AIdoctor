"""Deterministic provider-independent Model Gateway errors."""

from __future__ import annotations

from enum import Enum


class GatewayErrorCode(str, Enum):
    REQUEST_INVALID = "REQUEST_INVALID"
    ROUTE_NOT_FOUND = "ROUTE_NOT_FOUND"
    ROUTE_BLOCKED = "ROUTE_BLOCKED"
    ROUTE_INELIGIBLE = "ROUTE_INELIGIBLE"
    MODEL_NOT_FOUND = "MODEL_NOT_FOUND"
    NO_ELIGIBLE_MODEL = "NO_ELIGIBLE_MODEL"
    CONTRACT_UNKNOWN = "CONTRACT_UNKNOWN"
    CONTRACT_VERSION_UNKNOWN = "CONTRACT_VERSION_UNKNOWN"
    OUTPUT_INVALID = "OUTPUT_INVALID"
    PROVENANCE_INVALID = "PROVENANCE_INVALID"
    SELECTION_FAILED = "SELECTION_FAILED"
    CONTRACT_MISMATCH = "CONTRACT_MISMATCH"


class GatewayRuntimeError(ValueError):
    """One stable gateway-local failure with a machine-readable code."""

    def __init__(self, code: GatewayErrorCode, detail: str) -> None:
        self.code = code
        self.detail = detail
        super().__init__(f"{code.value}: {detail}")
