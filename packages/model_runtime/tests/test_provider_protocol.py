"""AC-P4：ProviderAdapter Protocol 与 Fake 符合性。"""

from __future__ import annotations

import inspect

from packages.model_runtime.providers import (
    DeterministicFakeProviderAdapter,
    ProviderAdapter,
)
from packages.model_runtime.providers.fake import load_builtin_synthetic_fixture_catalog
from packages.model_runtime.tests.conftest_p3 import build_gateway, gateway_request


def test_fake_is_runtime_checkable_provider_adapter() -> None:
    catalog = load_builtin_synthetic_fixture_catalog()
    fake = DeterministicFakeProviderAdapter(
        fixture_catalog=catalog,
        fixture_id="classify-color-success",
    )
    assert isinstance(fake, ProviderAdapter)
    assert hasattr(fake, "provider_id")
    assert callable(fake.invoke)
    signature = inspect.signature(DeterministicFakeProviderAdapter.invoke)
    assert list(signature.parameters) == ["self", "prepared"]


def test_protocol_has_no_fake_specific_members() -> None:
    annotations = getattr(ProviderAdapter, "__annotations__", {})
    for forbidden_name in ("fixture_id", "scenario", "fixture_catalog"):
        assert forbidden_name not in annotations
    source = inspect.getsource(ProviderAdapter)
    assert "SIMULATED_" not in source


def test_invoke_accepts_prepared_invocation_only() -> None:
    catalog = load_builtin_synthetic_fixture_catalog()
    fake = DeterministicFakeProviderAdapter(
        fixture_catalog=catalog,
        fixture_id="classify-color-success",
    )
    prepared = build_gateway().prepare(gateway_request())
    result = fake.invoke(prepared)
    assert result.provider_id == fake.provider_id
    assert result.request_id == prepared.request_id
