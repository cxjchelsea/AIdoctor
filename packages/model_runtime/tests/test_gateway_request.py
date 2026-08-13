"""NC-08 GatewayRequest structural tests."""

from __future__ import annotations

import pytest
from pydantic import ValidationError

from packages.model_runtime.gateway.models import GatewayRequest
from packages.model_runtime.tests.conftest_p3 import gateway_request


def test_valid_gateway_request():
    request = gateway_request()
    assert request.prompt_id == "classify-color"
    assert request.output_contract_id == "tool-result"


@pytest.mark.parametrize(
    "update",
    [
        {"route_id": "BadId"},
        {"route_version": "latest"},
        {"prompt_id": "!!!"},
        {"prompt_version": "1"},
        {"output_contract_id": "not-a-contract"},
        {"output_contract_version": "2.0.0"},
        {"tags": ("synthetic", "synthetic")},
    ],
)
def test_invalid_gateway_request_rejected(update):
    payload = gateway_request().model_dump()
    payload.update(update)
    with pytest.raises(ValidationError):
        GatewayRequest(**payload)


def test_extra_fields_rejected():
    payload = gateway_request().model_dump()
    payload["patient_id"] = "patient-1"
    with pytest.raises(ValidationError):
        GatewayRequest(**payload)
    payload = gateway_request().model_dump()
    payload["api_key"] = "secret"
    with pytest.raises(ValidationError):
        GatewayRequest(**payload)


def test_model_copy_revalidates():
    request = gateway_request()
    with pytest.raises(ValidationError):
        request.model_copy(update={"output_contract_id": "unknown-contract"})
    with pytest.raises(ValidationError):
        request.model_copy(update={"route_version": "latest"})
