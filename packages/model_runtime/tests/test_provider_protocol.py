"""AC-P4：ProviderAdapter Protocol 与 Fake 符合性。"""

from __future__ import annotations

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
