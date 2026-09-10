"""Deterministic, P2-local prompt runtime errors."""

from __future__ import annotations

from enum import Enum


class PromptErrorCode(str, Enum):
    PROMPT_NOT_REGISTERED = "PROMPT_NOT_REGISTERED"
    PROMPT_VERSION_NOT_REGISTERED = "PROMPT_VERSION_NOT_REGISTERED"
    PROMPT_RESOURCE_NOT_FOUND = "PROMPT_RESOURCE_NOT_FOUND"
    PROMPT_RESOURCE_FORBIDDEN = "PROMPT_RESOURCE_FORBIDDEN"
    PROMPT_SCHEMA_INVALID = "PROMPT_SCHEMA_INVALID"
    PROMPT_CHECKSUM_MISMATCH = "PROMPT_CHECKSUM_MISMATCH"
    PROMPT_REQUIRED_VARIABLE_MISSING = "PROMPT_REQUIRED_VARIABLE_MISSING"
    PROMPT_VARIABLE_TYPE_INVALID = "PROMPT_VARIABLE_TYPE_INVALID"
    PROMPT_EXTRA_VARIABLE = "PROMPT_EXTRA_VARIABLE"
    PROMPT_RENDER_INVALID = "PROMPT_RENDER_INVALID"


class PromptRuntimeError(ValueError):
    """One stable prompt-runtime failure with a machine-readable code."""

    def __init__(self, code: PromptErrorCode, detail: str) -> None:
        self.code = code
        self.detail = detail
        super().__init__(f"{code.value}: {detail}")
