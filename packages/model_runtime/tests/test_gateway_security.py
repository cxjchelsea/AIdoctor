"""NC-08 / P3 adversarial and leakage detectors."""

from __future__ import annotations

import ast
import os
from pathlib import Path

import pytest
from pydantic import ValidationError

from packages.model_runtime.gateway import ModelGateway
from packages.model_runtime.gateway.errors import GatewayErrorCode, GatewayRuntimeError
from packages.model_runtime.routing.policies import (
    ModelReference,
    ModelRoutePolicy,
    RouteCategory,
    RouteMatch,
    RouteStatus,
)
from packages.model_runtime.tests.conftest_p3 import build_gateway, gateway_request, model_spec


P3_SOURCE_DIRS = (
    Path("packages/model_runtime/schemas"),
    Path("packages/model_runtime/gateway"),
)

FORBIDDEN_IMPORT_NAMES = {
    "socket",
    "requests",
    "httpx",
    "aiohttp",
    "openai",
    "anthropic",
    "urllib.request",
    "http.client",
}

FORBIDDEN_NAME_FRAGMENTS = (
    "ProviderAdapter",
    "FakeProvider",
    "DeterministicFakeProviderAdapter",
    "ModelInvocationResult",
    "call_provider",
    "os.getenv",
    "os.environ",
)


def _iter_p3_python_files():
    for directory in P3_SOURCE_DIRS:
        for path in sorted(directory.rglob("*.py")):
            yield path


def test_p3_source_has_no_provider_or_network_implementation():
    for path in _iter_p3_python_files():
        source = path.read_text(encoding="utf-8")
        tree = ast.parse(source, filename=str(path))
        for node in ast.walk(tree):
            if isinstance(node, ast.Import):
                for alias in node.names:
                    assert alias.name.split(".")[0] not in FORBIDDEN_IMPORT_NAMES, path
            if isinstance(node, ast.ImportFrom) and node.module:
                root = node.module.split(".")[0]
                assert root not in FORBIDDEN_IMPORT_NAMES, path
                assert node.module not in FORBIDDEN_IMPORT_NAMES, path
        for fragment in FORBIDDEN_NAME_FRAGMENTS:
            # 允许错误/文档字符串中的否定说明；禁止作为实现符号出现在赋值/类定义中
            if fragment in {"os.getenv", "os.environ"}:
                assert fragment not in source, path
            elif f"class {fragment}" in source or f"def {fragment}" in source:
                raise AssertionError(f"forbidden implementation symbol {fragment} in {path}")


def test_network_entrypoints_unused_during_prepare(monkeypatch):
    def boom(*_args, **_kwargs):
        raise AssertionError("network entrypoint must not be used by P3 Gateway")

    monkeypatch.setattr("socket.socket", boom, raising=False)
    monkeypatch.setattr("urllib.request.urlopen", boom, raising=False)
    gateway = build_gateway()
    prepared = gateway.prepare(gateway_request())
    assert prepared.request_id == "req-classify-1"


def test_env_secret_lookup_not_used(monkeypatch):
    def boom_getenv(key, default=None):
        raise AssertionError(f"os.getenv must not be used by P3 Gateway: {key}")

    class BoomEnviron(dict):
        def __getitem__(self, key):
            raise AssertionError(f"os.environ must not be used by P3 Gateway: {key}")

        def get(self, key, default=None):
            raise AssertionError(f"os.environ.get must not be used by P3 Gateway: {key}")

    monkeypatch.setattr(os, "getenv", boom_getenv)
    monkeypatch.setattr(os, "environ", BoomEnviron())
    gateway = build_gateway()
    gateway.prepare(gateway_request())


def test_gateway_immutable_and_no_invoke_api():
    gateway = build_gateway()
    with pytest.raises(TypeError):
        gateway.foo = 1  # type: ignore[attr-defined]
    for forbidden in ("invoke", "send", "chat", "generate", "complete", "call_provider"):
        assert not hasattr(gateway, forbidden)


def test_clinical_route_rejected():
    route = ModelRoutePolicy(
        route_id="classify-color",
        version="1.0.0",
        category=RouteCategory.CLINICAL,
        match=RouteMatch(task_type="classify-color", required_tags=("synthetic",)),
        required_capabilities=("structured-output",),
        primary_model=ModelReference(
            provider_id="synthetic-provider",
            model_id="color-classifier",
            model_version="1.0.0",
        ),
        status=RouteStatus.BLOCKED,
        eligible=False,
    )
    gateway = build_gateway(routes=(route,), models=(model_spec(),))
    with pytest.raises(GatewayRuntimeError) as error:
        gateway.prepare(gateway_request())
    assert error.value.code is GatewayErrorCode.ROUTE_INELIGIBLE


def test_duplicate_model_identity_rejected():
    with pytest.raises(ValueError, match="duplicate ModelSpec"):
        build_gateway(models=(model_spec(), model_spec()))


def test_no_direct_clinical_state_imports_in_p3_source():
    forbidden = ("EncounterCDP", "ClinicalObservation", "TriageAssessment", "StateCommitter")
    for path in _iter_p3_python_files():
        source = path.read_text(encoding="utf-8")
        for name in forbidden:
            assert name not in source, path


def test_prepared_invocation_model_copy_revalidates():
    gateway = build_gateway()
    prepared = gateway.prepare(gateway_request())
    with pytest.raises(ValidationError):
        prepared.model_copy(update={"output_contract_id": "missing-contract"})


def test_eg09_offline_gateway_path():
    """EG-09：完整 offline Gateway 路径，无 provider 调用。"""

    gateway = build_gateway()
    assert isinstance(gateway, ModelGateway)
    prepared = gateway.prepare(gateway_request())
    from packages.model_runtime.tests.conftest_p3 import valid_tool_result_payload

    result = gateway.validate_output(prepared, valid_tool_result_payload())
    assert result.valid is True
    assert prepared.provenance.prompt_checksum
    assert prepared.timeout_policy.timeout_ms == 1000
