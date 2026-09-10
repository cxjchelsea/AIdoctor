"""SUP-01 DeterministicFakeProviderAdapter and SUP-02 P4 fixture catalog."""

from __future__ import annotations

import json
from collections.abc import Iterable
from enum import Enum
from importlib import resources
from types import MappingProxyType

from pydantic import field_validator, model_validator

from ..api.models import Identifier, StructuralModel, Version
from ..api.types import FrozenJsonObject, validate_identifier, validate_semver
from ..gateway.models import PreparedInvocation
from .errors import ProviderAdapterError, ProviderAdapterErrorCode
from .models import (
    BoundedErrorDetail,
    ModelInvocationResult,
    ProviderErrorCode,
    ProviderInvocationOutcome,
    assert_result_compatible,
)

# 包内固定资源根：禁止向公共调用方暴露 filesystem_path / fixture_root
SYNTHETIC_PROVIDER_RESOURCE_PACKAGE = "packages.model_runtime.resources.providers.synthetic"

# 显式枚举：无发现、无 latest、无任意调用方路径
_BUILTIN_SYNTHETIC_FIXTURE_FILES: tuple[str, ...] = (
    "classify-color-success.json",
    "classify-color-failure.json",
    "classify-color-timeout.json",
    "classify-color-invalid-output.json",
)

DEFAULT_SYNTHETIC_PROVIDER_ID = "synthetic-provider"
SIMULATED_FAILURE_CODE = "SIMULATED_FAILURE"
SIMULATED_TIMEOUT_CODE = "SIMULATED_TIMEOUT"


class FakeProviderScenario(str, Enum):
    """SUP-01/SUP-02 Fake-only deterministic scenarios (not NC-09 Protocol)."""

    SUCCESS = "SUCCESS"
    FAILURE = "FAILURE"
    TIMEOUT = "TIMEOUT"
    INVALID_OUTPUT = "INVALID_OUTPUT"


class FakeProviderFixture(StructuralModel):
    """Immutable exact fixture row for DeterministicFakeProviderAdapter."""

    fixture_id: Identifier
    provider_id: Identifier
    scenario: FakeProviderScenario
    output_contract_id: Identifier
    output_contract_version: Version
    candidate_payload: FrozenJsonObject | None = None
    error_code: ProviderErrorCode | None = None
    error_detail: BoundedErrorDetail | None = None

    _fixture_id = field_validator("fixture_id")(validate_identifier)
    _provider_id = field_validator("provider_id")(validate_identifier)
    _output_contract_id = field_validator("output_contract_id")(validate_identifier)
    _output_contract_version = field_validator("output_contract_version")(validate_semver)

    @model_validator(mode="after")
    def validate_fixture_coherence(self) -> "FakeProviderFixture":
        """Enforce Fake-scenario invariants (SIMULATED_* stays SUP-01-local)."""

        if self.scenario in {FakeProviderScenario.SUCCESS, FakeProviderScenario.INVALID_OUTPUT}:
            if self.candidate_payload is None:
                raise ValueError(f"{self.scenario.value} fixture requires candidate_payload")
            if self.error_code is not None or self.error_detail is not None:
                raise ValueError(f"{self.scenario.value} fixture forbids error fields")
            return self

        if self.scenario == FakeProviderScenario.FAILURE:
            if self.candidate_payload is not None:
                raise ValueError("FAILURE fixture forbids candidate_payload")
            if self.error_code != SIMULATED_FAILURE_CODE or self.error_detail is None:
                raise ValueError("FAILURE fixture requires SIMULATED_FAILURE and detail")
            return self

        if self.scenario == FakeProviderScenario.TIMEOUT:
            if self.candidate_payload is not None:
                raise ValueError("TIMEOUT fixture forbids candidate_payload")
            if self.error_code != SIMULATED_TIMEOUT_CODE or self.error_detail is None:
                raise ValueError("TIMEOUT fixture requires SIMULATED_TIMEOUT and detail")
            return self

        raise ValueError("unsupported fake provider scenario")


class FakeProviderFixtureCatalog:
    """Immutable exact fixture catalog owned by the Fake Provider support layer."""

    __slots__ = ("_fixtures", "_fixture_ids")

    def __init_subclass__(cls, **kwargs: object) -> None:
        raise TypeError("FakeProviderFixtureCatalog cannot be subclassed")

    def __init__(self, fixtures: Iterable[FakeProviderFixture]) -> None:
        indexed: dict[str, FakeProviderFixture] = {}
        # 立即物化为元组，调用方后续变异集合不影响目录
        materialised = tuple(fixtures)
        for fixture in materialised:
            # 精确类型：禁止 FakeProviderFixture 子类覆写语义
            if type(fixture) is not FakeProviderFixture:
                raise ProviderAdapterError(ProviderAdapterErrorCode.INVALID_FIXTURE)
            if fixture.fixture_id in indexed:
                raise ProviderAdapterError(ProviderAdapterErrorCode.INVALID_FIXTURE)
            indexed[fixture.fixture_id] = fixture
        object.__setattr__(self, "_fixtures", MappingProxyType(indexed))
        object.__setattr__(self, "_fixture_ids", tuple(sorted(indexed)))

    def __setattr__(self, _name: str, _value: object) -> None:
        raise TypeError("FakeProviderFixtureCatalog is immutable")

    def __delattr__(self, _name: str) -> None:
        raise TypeError("FakeProviderFixtureCatalog is immutable")

    def __len__(self) -> int:
        return len(self._fixtures)

    def contains(self, fixture_id: str) -> bool:
        return fixture_id in self._fixtures

    def get(self, fixture_id: str) -> FakeProviderFixture:
        try:
            return self._fixtures[fixture_id]
        except KeyError as exc:
            raise ProviderAdapterError(ProviderAdapterErrorCode.FIXTURE_NOT_FOUND) from exc

    def list_fixture_ids(self) -> tuple[str, ...]:
        """Deterministic enumeration of registered fixture identifiers."""

        return self._fixture_ids


def load_builtin_synthetic_fixture_catalog() -> FakeProviderFixtureCatalog:
    """Load the sealed SUP-02 P4 synthetic fixture subset from package resources."""

    try:
        resource_root = resources.files(SYNTHETIC_PROVIDER_RESOURCE_PACKAGE)
    except (ImportError, ModuleNotFoundError, AttributeError) as exc:
        raise ProviderAdapterError(ProviderAdapterErrorCode.INVALID_FIXTURE) from exc

    loaded: list[FakeProviderFixture] = []
    for file_name in _BUILTIN_SYNTHETIC_FIXTURE_FILES:
        target = resource_root.joinpath(file_name)
        try:
            if not target.is_file():
                raise ProviderAdapterError(ProviderAdapterErrorCode.FIXTURE_NOT_FOUND)
            raw_text = target.read_text(encoding="utf-8")
        except ProviderAdapterError:
            raise
        except OSError as exc:
            raise ProviderAdapterError(ProviderAdapterErrorCode.INVALID_FIXTURE) from exc
        try:
            payload = json.loads(raw_text)
            loaded.append(FakeProviderFixture.model_validate(payload))
        except (json.JSONDecodeError, ValueError, TypeError) as exc:
            raise ProviderAdapterError(ProviderAdapterErrorCode.INVALID_FIXTURE) from exc
    return FakeProviderFixtureCatalog(loaded)


class DeterministicFakeProviderAdapter:
    """Offline deterministic Fake Provider bound to exactly one fixture.

    Fixture selection is configuration-time only. After construction the adapter
    authority is sealed; Protocol itself remains unsealed for future providers.
    """

    __slots__ = ("_provider_id", "_fixture")

    def __init_subclass__(cls, **kwargs: object) -> None:
        raise TypeError("DeterministicFakeProviderAdapter cannot be subclassed")

    def __init__(
        self,
        *,
        provider_id: str = DEFAULT_SYNTHETIC_PROVIDER_ID,
        fixture_catalog: FakeProviderFixtureCatalog,
        fixture_id: str,
    ) -> None:
        # 精确 catalog 类型：禁止 duck-typed / 子类动态权威
        if type(fixture_catalog) is not FakeProviderFixtureCatalog:
            raise ProviderAdapterError(ProviderAdapterErrorCode.INVALID_FIXTURE)
        try:
            validated_provider_id = validate_identifier(provider_id)
        except ValueError as exc:
            raise ProviderAdapterError(ProviderAdapterErrorCode.INVALID_FIXTURE) from exc
        # 未知 fixture：构造期 fail-closed，禁止静默默认
        fixture = fixture_catalog.get(fixture_id)
        if fixture.provider_id != validated_provider_id:
            raise ProviderAdapterError(ProviderAdapterErrorCode.PROVIDER_MISMATCH)
        object.__setattr__(self, "_provider_id", validated_provider_id)
        object.__setattr__(self, "_fixture", fixture)

    def __setattr__(self, _name: str, _value: object) -> None:
        raise TypeError("DeterministicFakeProviderAdapter is immutable")

    def __delattr__(self, _name: str) -> None:
        raise TypeError("DeterministicFakeProviderAdapter is immutable")

    @property
    def provider_id(self) -> str:
        return self._provider_id

    @property
    def fixture_id(self) -> str:
        return self._fixture.fixture_id

    @property
    def scenario(self) -> FakeProviderScenario:
        return self._fixture.scenario

    def _build_expected_result(self, prepared: PreparedInvocation) -> ModelInvocationResult:
        """唯一 fixture→generic result 构造路径（invoke 与 Fake 绑定共用）。"""

        if not isinstance(prepared, PreparedInvocation):
            raise ProviderAdapterError(ProviderAdapterErrorCode.INVOCATION_RESULT_INVALID)

        if prepared.selected_model.provider_id != self._provider_id:
            raise ProviderAdapterError(ProviderAdapterErrorCode.PROVIDER_MISMATCH)

        fixture = self._fixture
        if (
            fixture.output_contract_id != prepared.output_contract_id
            or fixture.output_contract_version != prepared.output_contract_version
        ):
            raise ProviderAdapterError(ProviderAdapterErrorCode.CONTRACT_MISMATCH)

        # TIMEOUT 仅模拟结果：可读 timeout_policy，但绝不 sleep/等待/改写
        _ = prepared.timeout_policy

        # INVALID_OUTPUT：Fake 场景注入故意无效候选，泛型结果仍为 SUCCESS+payload
        if fixture.scenario in {FakeProviderScenario.SUCCESS, FakeProviderScenario.INVALID_OUTPUT}:
            outcome = ProviderInvocationOutcome.SUCCESS
            error_code = None
            error_detail = None
            candidate_payload = fixture.candidate_payload
        elif fixture.scenario == FakeProviderScenario.FAILURE:
            outcome = ProviderInvocationOutcome.FAILURE
            error_code = fixture.error_code
            error_detail = fixture.error_detail
            candidate_payload = None
        elif fixture.scenario == FakeProviderScenario.TIMEOUT:
            outcome = ProviderInvocationOutcome.TIMEOUT
            error_code = fixture.error_code
            error_detail = fixture.error_detail
            candidate_payload = None
        else:
            raise ProviderAdapterError(ProviderAdapterErrorCode.INVALID_FIXTURE)

        return ModelInvocationResult(
            request_id=prepared.request_id,
            provider_id=self._provider_id,
            selected_model=prepared.selected_model,
            outcome=outcome,
            candidate_payload=candidate_payload,
            output_contract_id=prepared.output_contract_id,
            output_contract_version=prepared.output_contract_version,
            rendered_prompt_digest=prepared.provenance.rendered_prompt_digest,
            error_code=error_code,
            error_detail=error_detail,
        )

    def invoke(self, prepared: PreparedInvocation) -> ModelInvocationResult:
        """Return the exact configured fixture outcome as a candidate-only result."""

        result = self._build_expected_result(prepared)
        self.assert_result_compatible(prepared, result)
        return result

    def assert_result_compatible(
        self,
        prepared: PreparedInvocation,
        result: ModelInvocationResult,
    ) -> None:
        """Fake-specific exact fixture→result authority.

        Runs generic Prepared binding, then requires structural equality with the
        deterministic expected result rebuilt from the sealed fixture.
        """

        assert_result_compatible(prepared, result)
        expected = self._build_expected_result(prepared)
        if result != expected:
            raise ProviderAdapterError(ProviderAdapterErrorCode.INVOCATION_RESULT_INVALID)
