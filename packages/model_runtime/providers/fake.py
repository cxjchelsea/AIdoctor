"""SUP-01 DeterministicFakeProviderAdapter and SUP-02 P4 fixture catalog."""

from __future__ import annotations

import json
from collections.abc import Iterable
from importlib import resources
from types import MappingProxyType

from ..api.types import validate_identifier
from ..gateway.models import PreparedInvocation
from .errors import ProviderAdapterError, ProviderAdapterErrorCode
from .models import (
    FakeProviderFixture,
    ModelInvocationResult,
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


class FakeProviderFixtureCatalog:
    """Immutable exact fixture catalog owned by the Fake Provider support layer."""

    __slots__ = ("_fixtures", "_fixture_ids")

    def __init__(self, fixtures: Iterable[FakeProviderFixture]) -> None:
        indexed: dict[str, FakeProviderFixture] = {}
        # 立即物化为元组，调用方后续变异集合不影响目录
        materialised = tuple(fixtures)
        for fixture in materialised:
            if not isinstance(fixture, FakeProviderFixture):
                raise ProviderAdapterError(
                    ProviderAdapterErrorCode.INVALID_FIXTURE,
                    "catalog entries must be FakeProviderFixture instances",
                )
            if fixture.fixture_id in indexed:
                raise ProviderAdapterError(
                    ProviderAdapterErrorCode.INVALID_FIXTURE,
                    f"duplicate fixture_id rejected: {fixture.fixture_id}",
                )
            indexed[fixture.fixture_id] = fixture
        object.__setattr__(self, "_fixtures", MappingProxyType(indexed))
        object.__setattr__(self, "_fixture_ids", tuple(sorted(indexed)))

    def __setattr__(self, _name: str, _value: object) -> None:
        raise TypeError("FakeProviderFixtureCatalog is immutable")

    def __len__(self) -> int:
        return len(self._fixtures)

    def contains(self, fixture_id: str) -> bool:
        return fixture_id in self._fixtures

    def get(self, fixture_id: str) -> FakeProviderFixture:
        try:
            return self._fixtures[fixture_id]
        except KeyError as exc:
            raise ProviderAdapterError(
                ProviderAdapterErrorCode.FIXTURE_NOT_FOUND,
                f"fixture is not registered: {fixture_id}",
            ) from exc

    def list_fixture_ids(self) -> tuple[str, ...]:
        """Deterministic enumeration of registered fixture identifiers."""

        return self._fixture_ids


def load_builtin_synthetic_fixture_catalog() -> FakeProviderFixtureCatalog:
    """Load the sealed SUP-02 P4 synthetic fixture subset from package resources."""

    try:
        resource_root = resources.files(SYNTHETIC_PROVIDER_RESOURCE_PACKAGE)
    except (ImportError, ModuleNotFoundError, AttributeError) as exc:
        raise ProviderAdapterError(
            ProviderAdapterErrorCode.INVALID_FIXTURE,
            "synthetic provider resource package is missing",
        ) from exc

    loaded: list[FakeProviderFixture] = []
    for file_name in _BUILTIN_SYNTHETIC_FIXTURE_FILES:
        target = resource_root.joinpath(file_name)
        try:
            if not target.is_file():
                raise ProviderAdapterError(
                    ProviderAdapterErrorCode.FIXTURE_NOT_FOUND,
                    f"builtin fixture resource missing: {file_name}",
                )
            raw_text = target.read_text(encoding="utf-8")
        except ProviderAdapterError:
            raise
        except OSError as exc:
            raise ProviderAdapterError(
                ProviderAdapterErrorCode.INVALID_FIXTURE,
                f"builtin fixture resource unreadable: {file_name}",
            ) from exc
        try:
            payload = json.loads(raw_text)
            loaded.append(FakeProviderFixture.model_validate(payload))
        except (json.JSONDecodeError, ValueError, TypeError) as exc:
            raise ProviderAdapterError(
                ProviderAdapterErrorCode.INVALID_FIXTURE,
                f"builtin fixture resource invalid: {file_name}",
            ) from exc
    return FakeProviderFixtureCatalog(loaded)


class DeterministicFakeProviderAdapter:
    """Offline deterministic Fake Provider bound to exactly one fixture_id.

    Fixture selection is configuration-time only — invoke(prepared) never accepts
    fixture controls, never infers from prompt text, and never waits on clocks.
    """

    __slots__ = ("_provider_id", "_fixture", "_fixture_catalog")

    def __init__(
        self,
        *,
        provider_id: str = DEFAULT_SYNTHETIC_PROVIDER_ID,
        fixture_catalog: FakeProviderFixtureCatalog,
        fixture_id: str,
    ) -> None:
        try:
            validated_provider_id = validate_identifier(provider_id)
        except ValueError as exc:
            raise ProviderAdapterError(
                ProviderAdapterErrorCode.INVALID_FIXTURE,
                "adapter provider_id is not a valid Identifier",
            ) from exc
        # 未知 fixture：构造期 fail-closed，禁止静默默认
        fixture = fixture_catalog.get(fixture_id)
        if fixture.provider_id != validated_provider_id:
            raise ProviderAdapterError(
                ProviderAdapterErrorCode.PROVIDER_MISMATCH,
                "fixture provider_id does not match adapter provider_id",
            )
        object.__setattr__(self, "_provider_id", validated_provider_id)
        object.__setattr__(self, "_fixture", fixture)
        object.__setattr__(self, "_fixture_catalog", fixture_catalog)

    @property
    def provider_id(self) -> str:
        return self._provider_id

    @property
    def fixture_id(self) -> str:
        return self._fixture.fixture_id

    def invoke(self, prepared: PreparedInvocation) -> ModelInvocationResult:
        """Return the exact configured fixture outcome as a candidate-only result."""

        if not isinstance(prepared, PreparedInvocation):
            raise ProviderAdapterError(
                ProviderAdapterErrorCode.INVOCATION_RESULT_INVALID,
                "invoke requires PreparedInvocation",
            )

        # 禁止重选模型 / 重渲染 / 改写 prepared
        if prepared.selected_model.provider_id != self._provider_id:
            raise ProviderAdapterError(
                ProviderAdapterErrorCode.PROVIDER_MISMATCH,
                "prepared selected provider does not match adapter provider_id",
            )

        fixture = self._fixture
        if (
            fixture.output_contract_id != prepared.output_contract_id
            or fixture.output_contract_version != prepared.output_contract_version
        ):
            raise ProviderAdapterError(
                ProviderAdapterErrorCode.CONTRACT_MISMATCH,
                "fixture output contract does not match prepared contract",
            )

        # TIMEOUT 仅模拟结果：可读 timeout_policy，但绝不 sleep/等待/改写
        _ = prepared.timeout_policy

        result = ModelInvocationResult(
            request_id=prepared.request_id,
            provider_id=self._provider_id,
            selected_model=prepared.selected_model,
            outcome=fixture.outcome,
            candidate_payload=fixture.candidate_payload,
            output_contract_id=prepared.output_contract_id,
            output_contract_version=prepared.output_contract_version,
            rendered_prompt_digest=prepared.provenance.rendered_prompt_digest,
            fixture_id=fixture.fixture_id,
            error_code=fixture.error_code,
            error_detail=fixture.error_detail,
        )
        assert_result_compatible(prepared, result)
        return result
