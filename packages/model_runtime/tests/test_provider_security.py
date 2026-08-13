"""AC-P4 / EG-10：网络 / SDK / 密钥 / sleep / 泄漏 / 真实 Provider 静态审计。"""

from __future__ import annotations

import ast
import os
import time
from pathlib import Path

import pytest

from packages.model_runtime.providers.errors import ProviderAdapterError, ProviderAdapterErrorCode
from packages.model_runtime.providers.fake import (
    DeterministicFakeProviderAdapter,
    load_builtin_synthetic_fixture_catalog,
)
from packages.model_runtime.providers.models import ProviderInvocationOutcome
from packages.model_runtime.tests.conftest_p3 import build_gateway, gateway_request


P4_SOURCE_DIRS = (
    Path("packages/model_runtime/providers"),
)

FORBIDDEN_IMPORT_ROOTS = {
    "socket",
    "requests",
    "httpx",
    "aiohttp",
    "openai",
    "anthropic",
    "google",
    "urllib",
    "http",
}

FORBIDDEN_CLASS_NAMES = {
    "OpenAIProviderAdapter",
    "AnthropicProviderAdapter",
    "AzureProviderAdapter",
    "GeminiProviderAdapter",
    "HTTPProviderAdapter",
    "RESTProviderAdapter",
    "GenericNetworkProviderAdapter",
    "LegacyAdapter",
}

CLINICAL_LOGIC_TOKENS = (
    "EncounterCDP",
    "ClinicalObservation",
    "TriageAssessment",
    "StateCommitter",
)

SECRET_MARKER = "synthetic-secret-value-12345"


def _iter_p4_python_files():
    for directory in P4_SOURCE_DIRS:
        for path in sorted(directory.rglob("*.py")):
            yield path


def test_static_audit_no_real_provider_sdk_or_network() -> None:
    for path in _iter_p4_python_files():
        source = path.read_text(encoding="utf-8")
        tree = ast.parse(source, filename=str(path))
        for node in ast.walk(tree):
            if isinstance(node, ast.Import):
                for alias in node.names:
                    root = alias.name.split(".")[0]
                    assert root not in FORBIDDEN_IMPORT_ROOTS, path
            if isinstance(node, ast.ImportFrom) and node.module:
                root = node.module.split(".")[0]
                assert root not in FORBIDDEN_IMPORT_ROOTS, path
            if isinstance(node, ast.ClassDef):
                assert node.name not in FORBIDDEN_CLASS_NAMES, path
        for token in CLINICAL_LOGIC_TOKENS:
            assert token not in source, path


def test_fixture_resources_are_non_clinical_json_only() -> None:
    root = Path("packages/model_runtime/resources/providers/synthetic")
    banned = ("patient", "diagnosis", "clinical", "medical", "phi", "DATA-EV")
    for path in sorted(root.glob("*.json")):
        text = path.read_text(encoding="utf-8").lower()
        for token in banned:
            assert token not in text, path


def test_network_and_secret_detectors_during_fake_path(monkeypatch) -> None:
    def boom_network(*_args, **_kwargs):
        raise AssertionError("network entrypoint must not be used by P4 Fake Provider")

    def boom_getenv(key, default=None):
        raise AssertionError(f"os.getenv must not be used by P4 Fake Provider: {key}")

    class BoomEnviron(dict):
        def __getitem__(self, key):
            raise AssertionError(f"os.environ must not be read by P4 Fake Provider: {key}")

        def get(self, key, default=None):
            raise AssertionError(f"os.environ.get must not be used by P4 Fake Provider: {key}")

    monkeypatch.setattr("socket.socket", boom_network, raising=False)
    monkeypatch.setattr("socket.create_connection", boom_network, raising=False)
    monkeypatch.setattr("urllib.request.urlopen", boom_network, raising=False)
    for module_name in ("requests", "httpx"):
        try:
            module = __import__(module_name)
        except ImportError:
            continue
        if hasattr(module, "request"):
            monkeypatch.setattr(f"{module_name}.request", boom_network, raising=False)
        if hasattr(module, "get"):
            monkeypatch.setattr(f"{module_name}.get", boom_network, raising=False)

    monkeypatch.setattr(os, "getenv", boom_getenv)
    monkeypatch.setattr(os, "environ", BoomEnviron())

    catalog = load_builtin_synthetic_fixture_catalog()
    fake = DeterministicFakeProviderAdapter(
        fixture_catalog=catalog,
        fixture_id="classify-color-success",
    )
    gateway = build_gateway()
    prepared = gateway.prepare(gateway_request())
    result = fake.invoke(prepared)
    assert result.outcome == ProviderInvocationOutcome.SUCCESS
    validation = gateway.validate_output(prepared, result.candidate_payload.to_json_value())
    assert validation.valid is True


def test_timeout_does_not_sleep_or_wait(monkeypatch) -> None:
    def boom_sleep(*_args, **_kwargs):
        raise AssertionError("time.sleep must not be used by TIMEOUT fixture")

    monkeypatch.setattr(time, "sleep", boom_sleep)
    fake = DeterministicFakeProviderAdapter(
        fixture_catalog=load_builtin_synthetic_fixture_catalog(),
        fixture_id="classify-color-timeout",
    )
    prepared = build_gateway().prepare(gateway_request())
    result = fake.invoke(prepared)
    assert result.outcome == ProviderInvocationOutcome.TIMEOUT


def test_error_detail_does_not_leak_secret_marker_or_prompt() -> None:
    fake = DeterministicFakeProviderAdapter(
        fixture_catalog=load_builtin_synthetic_fixture_catalog(),
        fixture_id="classify-color-failure",
    )
    prepared = build_gateway().prepare(gateway_request())
    result = fake.invoke(prepared)
    assert SECRET_MARKER not in (result.error_detail or "")
    assert "color_name" not in (result.error_detail or "")
    for message in prepared.rendered_prompt.messages:
        assert message.content not in (result.error_detail or "")

    error = ProviderAdapterError(
        code=ProviderAdapterErrorCode.PROVIDER_MISMATCH,
        detail="prepared selected provider does not match adapter provider_id",
    )
    assert SECRET_MARKER not in str(error)
    assert SECRET_MARKER not in error.detail
