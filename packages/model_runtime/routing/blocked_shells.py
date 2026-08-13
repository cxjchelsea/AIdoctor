"""A7-NC-P6：Observation Extraction / Question Wording DEFAULT BLOCKED shells。

仅提供结构性声明 + 复用现有 ModelRoutePolicy / OutputSchemaRegistry /
assert_route_eligible 失败关闭语义。不执行 Prompt、Provider、网络或状态写入。
"""

from __future__ import annotations

from typing import Final

from pydantic import field_validator, model_validator

from ..api.models import (
    SHARED_CONTRACT_V1_IDS,
    SHARED_CONTRACT_V1_VERSION,
    Identifier,
    StructuralModel,
    Version,
)
from ..api.types import validate_identifier, validate_semver
from ..gateway.errors import GatewayErrorCode, GatewayRuntimeError
from ..gateway.selection import assert_route_eligible
from ..schemas import OutputSchemaRegistry
from .policies import ModelRoutePolicy, RouteCategory, RouteMatch, RouteStatus

# 结构性 ModelRoutePolicy token（非 capabilities/** 绑定）
STRUCTURAL_BLOCKED_ROUTE_CAPABILITY: Final[str] = "blocked-route-shell"

# 授权合同引用（仅 ID/version；不构造临床 payload）
AUTHORIZED_INPUT_CONTRACT_ID: Final[str] = "tool-context"
AUTHORIZED_OUTPUT_CONTRACT_ID: Final[str] = "tool-result"
AUTHORIZED_CONTRACT_VERSION: Final[str] = SHARED_CONTRACT_V1_VERSION

OBSERVATION_EXTRACTION_ROUTE_ID: Final[str] = "observation-extraction"
QUESTION_WORDING_ROUTE_ID: Final[str] = "question-wording"
AUTHORIZED_SHELL_VERSION: Final[str] = "1.0.0"


class BlockedRouteShell(StructuralModel):
    """P6 additive blocked-route shell：声明结构，不激活临床路由。"""

    route_id: Identifier
    version: Version
    policy: ModelRoutePolicy
    input_contract_id: Identifier
    input_contract_version: Version
    output_contract_id: Identifier
    output_contract_version: Version

    _route_id = field_validator("route_id")(validate_identifier)
    _version = field_validator("version")(validate_semver)
    _input_contract_id = field_validator("input_contract_id")(validate_identifier)
    _input_contract_version = field_validator("input_contract_version")(validate_semver)
    _output_contract_id = field_validator("output_contract_id")(validate_identifier)
    _output_contract_version = field_validator("output_contract_version")(validate_semver)

    @model_validator(mode="after")
    def validate_blocked_shell_invariants(self) -> "BlockedRouteShell":
        """强制 EG-15/16 DEFAULT BLOCKED 结构不变量。"""

        if self.policy.route_id != self.route_id or self.policy.version != self.version:
            raise ValueError("shell identity must match embedded ModelRoutePolicy identity")
        if self.policy.category is not RouteCategory.CLINICAL:
            raise ValueError("blocked route shell category must be CLINICAL")
        if self.policy.status is not RouteStatus.BLOCKED:
            raise ValueError("blocked route shell status must be BLOCKED")
        if self.policy.eligible:
            raise ValueError("blocked route shell must remain ineligible")
        if self.policy.primary_model is not None:
            raise ValueError("blocked route shell primary_model must be None")
        if self.policy.fallback_models:
            raise ValueError("blocked route shell fallback_models must be empty")
        if self.policy.required_capabilities != (STRUCTURAL_BLOCKED_ROUTE_CAPABILITY,):
            raise ValueError(
                "blocked route shell required_capabilities must be exactly "
                f"({STRUCTURAL_BLOCKED_ROUTE_CAPABILITY!r},)"
            )
        # P6 授权合同引用固定；禁止 state-patch / patient-delivery-view 等语义漂移
        if self.input_contract_id != AUTHORIZED_INPUT_CONTRACT_ID:
            raise ValueError(
                f"blocked route shell input contract must be {AUTHORIZED_INPUT_CONTRACT_ID}"
            )
        if self.output_contract_id != AUTHORIZED_OUTPUT_CONTRACT_ID:
            raise ValueError(
                f"blocked route shell output contract must be {AUTHORIZED_OUTPUT_CONTRACT_ID}"
            )
        if self.input_contract_id not in SHARED_CONTRACT_V1_IDS:
            raise ValueError(f"unknown input contract: {self.input_contract_id}")
        if self.output_contract_id not in SHARED_CONTRACT_V1_IDS:
            raise ValueError(f"unknown output contract: {self.output_contract_id}")
        if self.input_contract_version != AUTHORIZED_CONTRACT_VERSION:
            raise ValueError("input contract version must be exact Shared Contracts v1")
        if self.output_contract_version != AUTHORIZED_CONTRACT_VERSION:
            raise ValueError("output contract version must be exact Shared Contracts v1")
        return self


def _build_blocked_policy(*, route_id: str, version: str) -> ModelRoutePolicy:
    """构造 CLINICAL / BLOCKED / eligible=false / 无模型 的 ModelRoutePolicy。"""

    return ModelRoutePolicy(
        route_id=route_id,
        version=version,
        category=RouteCategory.CLINICAL,
        match=RouteMatch(task_type=route_id, required_tags=()),
        required_capabilities=(STRUCTURAL_BLOCKED_ROUTE_CAPABILITY,),
        primary_model=None,
        fallback_models=(),
        status=RouteStatus.BLOCKED,
        eligible=False,
    )


def _build_official_shell(*, route_id: str) -> BlockedRouteShell:
    """仓库权威官方 shell（固定合同引用）。"""

    return BlockedRouteShell(
        route_id=route_id,
        version=AUTHORIZED_SHELL_VERSION,
        policy=_build_blocked_policy(route_id=route_id, version=AUTHORIZED_SHELL_VERSION),
        input_contract_id=AUTHORIZED_INPUT_CONTRACT_ID,
        input_contract_version=AUTHORIZED_CONTRACT_VERSION,
        output_contract_id=AUTHORIZED_OUTPUT_CONTRACT_ID,
        output_contract_version=AUTHORIZED_CONTRACT_VERSION,
    )


# 模块加载期物化官方 identity（不可变 StructuralModel）
_OBSERVATION_EXTRACTION_SHELL: Final[BlockedRouteShell] = _build_official_shell(
    route_id=OBSERVATION_EXTRACTION_ROUTE_ID
)
_QUESTION_WORDING_SHELL: Final[BlockedRouteShell] = _build_official_shell(
    route_id=QUESTION_WORDING_ROUTE_ID
)
_OFFICIAL_SHELLS: Final[tuple[BlockedRouteShell, BlockedRouteShell]] = (
    _OBSERVATION_EXTRACTION_SHELL,
    _QUESTION_WORDING_SHELL,
)


def get_observation_extraction_shell() -> BlockedRouteShell:
    """返回 NC-13 / EG-15 官方 Observation Extraction BLOCKED shell。"""

    return _OBSERVATION_EXTRACTION_SHELL


def get_question_wording_shell() -> BlockedRouteShell:
    """返回 NC-14 / EG-16 官方 Question Wording BLOCKED shell。"""

    return _QUESTION_WORDING_SHELL


def list_official_blocked_route_shells() -> tuple[BlockedRouteShell, ...]:
    """返回全部官方 P6 blocked shells（恰好两个）。"""

    return _OFFICIAL_SHELLS


def is_official_blocked_route_shell(shell: BlockedRouteShell) -> bool:
    """仅官方工厂返回的对象构成 P6 authorized shell identity（对象身份）。"""

    if type(shell) is not BlockedRouteShell:
        return False
    return any(shell is official for official in _OFFICIAL_SHELLS)


def resolve_shell_contract_refs(
    shell: BlockedRouteShell,
    registry: OutputSchemaRegistry | None = None,
) -> tuple[object, object]:
    """通过现有 OutputSchemaRegistry 精确解析合同引用（不构造业务 payload）。"""

    schema_registry = registry if registry is not None else OutputSchemaRegistry()
    if type(schema_registry) is not OutputSchemaRegistry:
        raise TypeError("registry must be exact OutputSchemaRegistry")
    input_entry = schema_registry.get(shell.input_contract_id, shell.input_contract_version)
    output_entry = schema_registry.get(shell.output_contract_id, shell.output_contract_version)
    return input_entry, output_entry


def assert_blocked_shell_fail_closed(shell: BlockedRouteShell) -> GatewayRuntimeError:
    """集成确定性校验：合同可解析后，在 Prompt/模型/Provider 之前 fail-closed。

    期望现有 precedence：CLINICAL → GatewayErrorCode.ROUTE_INELIGIBLE。
    """

    resolve_shell_contract_refs(shell)
    try:
        assert_route_eligible(shell.policy)
    except GatewayRuntimeError as error:
        if error.code is not GatewayErrorCode.ROUTE_INELIGIBLE:
            raise GatewayRuntimeError(
                error.code,
                f"unexpected fail-closed code for blocked shell {shell.route_id}@{shell.version}",
            ) from error
        return error
    raise AssertionError(
        f"blocked shell must fail closed before provider: {shell.route_id}@{shell.version}"
    )
