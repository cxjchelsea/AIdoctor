"""AC-P4：FakeProviderFixtureCatalog 不可变 / 重复 / 未知。"""

from __future__ import annotations

import pytest

from packages.model_runtime.providers.errors import ProviderAdapterError, ProviderAdapterErrorCode
from packages.model_runtime.providers.fake import (
    DeterministicFakeProviderAdapter,
    FakeProviderFixtureCatalog,
    load_builtin_synthetic_fixture_catalog,
)
from packages.model_runtime.providers.models import (
    FakeProviderFixture,
    ProviderInvocationOutcome,
    SIMULATED_FAILURE_CODE,
)


def test_builtin_catalog_is_exact_and_immutable() -> None:
    catalog = load_builtin_synthetic_fixture_catalog()
    assert len(catalog) == 4
    assert catalog.list_fixture_ids() == (
        "classify-color-failure",
        "classify-color-invalid-output",
        "classify-color-success",
        "classify-color-timeout",
    )
    with pytest.raises(TypeError):
        catalog._fixtures = {}  # type: ignore[misc]


def test_unknown_fixture_fail_closed_at_construction() -> None:
    catalog = load_builtin_synthetic_fixture_catalog()
    with pytest.raises(ProviderAdapterError) as captured:
        DeterministicFakeProviderAdapter(
            fixture_catalog=catalog,
            fixture_id="does-not-exist",
        )
    assert captured.value.code == ProviderAdapterErrorCode.FIXTURE_NOT_FOUND


def test_duplicate_fixture_id_rejected() -> None:
    catalog = load_builtin_synthetic_fixture_catalog()
    fixture = catalog.get("classify-color-failure")
    with pytest.raises(ProviderAdapterError) as captured:
        FakeProviderFixtureCatalog([fixture, fixture])
    assert captured.value.code == ProviderAdapterErrorCode.INVALID_FIXTURE


def test_caller_collection_mutation_does_not_affect_catalog() -> None:
    failure = FakeProviderFixture(
        fixture_id="classify-color-failure",
        provider_id="synthetic-provider",
        outcome=ProviderInvocationOutcome.FAILURE,
        output_contract_id="tool-result",
        output_contract_version="1.0.0",
        error_code=SIMULATED_FAILURE_CODE,
        error_detail="simulated provider failure",
    )
    mutable_rows = [failure]
    catalog = FakeProviderFixtureCatalog(mutable_rows)
    mutable_rows.clear()
    assert catalog.contains("classify-color-failure")
    assert len(catalog) == 1


def test_fixture_model_copy_revalidates() -> None:
    catalog = load_builtin_synthetic_fixture_catalog()
    fixture = catalog.get("classify-color-success")
    with pytest.raises(Exception):
        fixture.model_copy(update={"outcome": ProviderInvocationOutcome.FAILURE})
