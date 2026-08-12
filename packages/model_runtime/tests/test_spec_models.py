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


@pytest.mark.parametrize(
    "version",
    ["0.0.0", "1.0.0", "10.20.30", "1.0.0-alpha", "1.0.0-alpha.1", "1.0.0+build.1", "1.0.0-alpha+build"],
)
def test_semver_accepts_supported_forms(version):
    data = model_data()
    data["version"] = version
    assert ModelSpec.model_validate(data).version == version


@pytest.mark.parametrize(
    "version",
    ["01.0.0", "1.00.0", "1.0.00", "1.0.0-01", "latest", "v1.0.0", "1", "1.0", "", " 1.0.0"],
)
def test_semver_rejects_invalid_and_implicit_versions(version):
    data = model_data()
    data["version"] = version
    with pytest.raises(ValidationError):
        ModelSpec.model_validate(data)


def test_shared_contract_reference_requires_existing_id_and_version_pair():
    assert PromptSpec.model_validate(prompt_data()).output_contract_version == "1.0.0"
    data = prompt_data()
    data["output_contract_version"] = "999.0.0"
    with pytest.raises(ValidationError):
        PromptSpec.model_validate(data)
    data = prompt_data()
    data["output_contract_id"] = "unknown-contract"
    with pytest.raises(ValidationError):
        PromptSpec.model_validate(data)


def test_metadata_is_recursively_immutable():
    data = prompt_data()
    data["metadata"] = {"nested": {"x": 1}, "items": [1, {"y": 2}]}
    spec = PromptSpec.model_validate(data)
    with pytest.raises(TypeError):
        spec.metadata["x"] = "changed"
    with pytest.raises(TypeError):
        spec.metadata["nested"]["x"] = "changed"
    with pytest.raises(AttributeError):
        spec.metadata["items"].append(3)


@pytest.mark.parametrize(
    "value",
    [object(), lambda: None, {1, 2}, b"bytes", bytearray(b"bytes"), complex(1, 2), float("nan"), float("inf"), float("-inf")],
)
def test_metadata_rejects_non_json_and_non_finite_values(value):
    data = prompt_data()
    data["metadata"] = {"value": value}
    with pytest.raises(ValidationError):
        PromptSpec.model_validate(data)


def test_metadata_key_order_is_canonical_and_round_trip_is_stable():
    left = prompt_data()
    left["metadata"] = {"b": 2, "a": {"d": 4, "c": [3, 2, 1]}}
    right = prompt_data()
    right["metadata"] = {"a": {"c": [3, 2, 1], "d": 4}, "b": 2}
    left_json = PromptSpec.model_validate(left).model_dump_json()
    right_json = PromptSpec.model_validate(right).model_dump_json()
    assert left_json == right_json
    assert PromptSpec.model_validate_json(left_json).model_dump_json() == left_json


def test_checksum_error_describes_both_accepted_formats():
    data = prompt_data()
    data["checksum"] = "invalid"
    with pytest.raises(ValidationError, match=r"64-character.*or sha256:<digest>"):
        PromptSpec.model_validate(data)
    data = prompt_data()
    data["variables"] *= 2
    with pytest.raises(ValidationError):
        PromptSpec.model_validate(data)
