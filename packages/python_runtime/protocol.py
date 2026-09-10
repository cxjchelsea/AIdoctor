"""Shared Contracts v1 薄消费边界：只校验 EXACT 1.0.0，不复制 schema。"""

from __future__ import annotations

from aidoctor_shared_contracts import (
    CONTRACT_VERSION,
    VERSION_NEGOTIATION,
    ContractEnvelope,
)


class ProtocolValidationError(ValueError):
    """契约版本或协商策略不满足 EXACT 1.0.0 时失败关闭。"""


def require_exact_contract_binding() -> None:
    """绑定包自身必须仍是冻结的 EXACT / 1.0.0；漂移则失败关闭。"""

    if CONTRACT_VERSION != "1.0.0":
        raise ProtocolValidationError(
            f"aidoctor_shared_contracts.CONTRACT_VERSION must be 1.0.0, got {CONTRACT_VERSION!r}"
        )
    if VERSION_NEGOTIATION != "EXACT":
        raise ProtocolValidationError(
            "aidoctor_shared_contracts.VERSION_NEGOTIATION must be EXACT, "
            f"got {VERSION_NEGOTIATION!r}"
        )


def require_exact_contract_version(contract_version: str) -> str:
    """要求调用方声明的 contract_version 精确等于冻结 v1。"""

    require_exact_contract_binding()
    if contract_version != CONTRACT_VERSION:
        raise ProtocolValidationError(
            f"contract_version must be {CONTRACT_VERSION!r} with EXACT negotiation, "
            f"got {contract_version!r}"
        )
    return contract_version


def validate_envelope(envelope: ContractEnvelope) -> ContractEnvelope:
    """校验信封版本；不发明跨语言字段，不改契约语义。"""

    if not isinstance(envelope, ContractEnvelope):
        raise ProtocolValidationError("envelope must be aidoctor_shared_contracts.ContractEnvelope")
    require_exact_contract_version(envelope.contract_version)
    return envelope
