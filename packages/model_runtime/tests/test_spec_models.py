"""Structural validation tests for ModelSpec and PromptSpec."""

import pytest
from pydantic import ValidationError

from packages.model_runtime.api import (
    ContextLimits,
    CostMetadata,
    ModelSpec,
    PromptSpec,
    PromptVariable,
    RetryClass,
    TimeoutPolicy,
)


def model_data():
    return {
        "provider_id": "demo-provider",
        "model_id": "demo-model-v1",
        "version": "1.0.0",
        "capabilities": ["echo-structured-input"],
        "context_limits": {"max_input_tokens": 4096, "max_output_tokens": 512},
        "supports_structured_output": True,
        "timeout_policy": {"timeout_ms": 1000},
        "retry_class": "TRANSIENT",
        "cost_metadata": {
            "currency": "USD",
            "input_microunits_per_million_tokens": 0,
            "output_microunits_per_million_tokens": 0,
        },
    }


def prompt_data():
    return {
        "prompt_id": "classify-color",
        "version": "1.0.0",
        "variables": [{"name": "color", "value_type": "string", "required": True}],
        "output_contract_id": "tool-result",
        "output_contract_version": "1.0.0",
        "checksum": "sha256:" + "a" * 64,
        "metadata": {"purpose": "synthetic-non-clinical"},
    }


def test_valid_model_spec_construction():
    spec = ModelSpec.model_validate(model_data())
    assert isinstance(spec.context_limits, ContextLimits)
    assert isinstance(spec.timeout_policy, TimeoutPolicy)
    assert isinstance(spec.cost_metadata, CostMetadata)
    assert spec.retry_class is RetryClass.TRANSIENT
    assert spec.version == "1.0.0"


@pytest.mark.parametrize("field,value", [("provider_id", ""), ("model_id", "Latest Model"), ("version", "latest")])
def test_model_identity_and_version_fail_closed(field, value):
    data = model_data()
    data[field] = value
    with pytest.raises(ValidationError):
        ModelSpec.model_validate(data)


@pytest.mark.parametrize("status", ["APPROVED", "CLINICALLY_APPROVED", "CLINICALLY_VALIDATED", "PRODUCTION_CLINICAL"])
def test_model_clinical_approval_statuses_are_rejected(status):
    data = model_data()
    data["status"] = status
    with pytest.raises(ValidationError):
        ModelSpec.model_validate(data)


def test_model_constraints_and_capabilities_are_strict():
    data = model_data()
    data["context_limits"]["max_input_tokens"] = 0
    with pytest.raises(ValidationError):
        ModelSpec.model_validate(data)
    data = model_data()
    data["retry_class"] = "ALWAYS"
    with pytest.raises(ValidationError):
        ModelSpec.model_validate(data)
    data = model_data()
    data["capabilities"] = []
    with pytest.raises(ValidationError):
        ModelSpec.model_validate(data)
    data = model_data()
    data["timeout_policy"]["timeout_ms"] = "1000"
    with pytest.raises(ValidationError):
        ModelSpec.model_validate(data)


def test_valid_prompt_spec_has_metadata_not_body():
    spec = PromptSpec.model_validate(prompt_data())
    assert spec.version == "1.0.0"
    assert spec.output_contract_id == "tool-result"
    assert set(PromptSpec.model_fields).isdisjoint({"body", "template", "instructions"})


@pytest.mark.parametrize("field,value", [("version", "latest"), ("output_contract_version", "v1"), ("checksum", "not-a-hash")])
def test_prompt_version_and_checksum_fail_closed(field, value):
    data = prompt_data()
    data[field] = value
    with pytest.raises(ValidationError):
        PromptSpec.model_validate(data)


def test_prompt_invalid_status_and_duplicate_variables_are_rejected():
    data = prompt_data()
    data["status"] = "CLINICALLY_APPROVED"
    with pytest.raises(ValidationError):
        PromptSpec.model_validate(data)


def test_prompt_rejects_unknown_output_contract_reference():
    data = prompt_data()
    data["output_contract_id"] = "invented-clinical-output"
    with pytest.raises(ValidationError):
        PromptSpec.model_validate(data)
    data = prompt_data()
    data["variables"] *= 2
    with pytest.raises(ValidationError):
        PromptSpec.model_validate(data)
