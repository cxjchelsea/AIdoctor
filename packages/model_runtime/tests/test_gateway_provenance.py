"""NC-08 deterministic provenance tests."""

from __future__ import annotations

import pytest
from pydantic import ValidationError

from packages.model_runtime.gateway.models import GatewayProvenance
from packages.model_runtime.routing.policies import ModelReference
from packages.model_runtime.tests.conftest_p3 import build_gateway, gateway_request, model_spec, eligible_route


def test_same_inputs_produce_equal_provenance():
    gateway = build_gateway()
    first = gateway.prepare(gateway_request()).provenance
    second = gateway.prepare(gateway_request()).provenance
    assert first == second


def test_provenance_changes_with_selection_and_prompt_inputs():
    gateway = build_gateway()
    base = gateway.prepare(gateway_request()).provenance

    # 不同 request_id
    other_request = gateway.prepare(gateway_request(request_id="req-classify-2")).provenance
    assert other_request.request_id != base.request_id

    # fallback 位置变化
    primary = ModelReference(
        provider_id="synthetic-provider",
        model_id="bad-model",
        model_version="1.0.0",
    )
    fallback = ModelReference(
        provider_id="synthetic-provider",
        model_id="color-classifier",
        model_version="1.0.0",
    )
    route = eligible_route(primary=primary, fallbacks=(fallback,))
    gateway_fallback = build_gateway(
        routes=(route,),
        models=(
            model_spec(model_id="bad-model", supports_structured_output=False),
            model_spec(model_id="color-classifier"),
        ),
    )
    fallback_prov = gateway_fallback.prepare(gateway_request()).provenance
    assert fallback_prov.fallback_used is True
    assert fallback_prov.selection_index == 1
    assert fallback_prov != base


def test_provenance_model_copy_rejects_invalid_updates():
    gateway = build_gateway()
    provenance = gateway.prepare(gateway_request()).provenance
    assert isinstance(provenance, GatewayProvenance)
    with pytest.raises(ValidationError):
        provenance.model_copy(update={"prompt_checksum": "not-a-digest"})
    with pytest.raises(ValidationError):
        provenance.model_copy(update={"fallback_used": True})
    with pytest.raises(ValidationError):
        provenance.model_copy(update={"output_contract_id": "missing"})
