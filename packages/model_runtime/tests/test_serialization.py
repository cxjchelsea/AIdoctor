"""Deterministic offline serialization and validated-copy tests."""

import pytest
from pydantic import ValidationError

from packages.model_runtime.api import ContextLimits, CostMetadata, ModelSpec, PromptSpec, PromptVariable, TimeoutPolicy
from packages.model_runtime.routing import ModelReference, ModelRoutePolicy, RouteMatch, RouteStatus

from .test_route_policy import route_data
from .test_spec_models import model_data, prompt_data


def assert_stable_round_trip(model_type, data):
    first = model_type.model_validate(data).model_dump_json()
    second = model_type.model_validate_json(first).model_dump_json()
    assert first == second


def test_model_spec_round_trip_is_stable():
    assert_stable_round_trip(ModelSpec, model_data())


def test_prompt_spec_round_trip_is_stable():
    assert_stable_round_trip(PromptSpec, prompt_data())


def test_route_policy_round_trip_is_stable():
    assert_stable_round_trip(ModelRoutePolicy, route_data())


def test_public_imports_have_no_runtime_side_effect_dependencies():
    assert ModelSpec.__module__ == "packages.model_runtime.api.models"
    assert PromptSpec.__module__ == "packages.model_runtime.api.models"
    assert ModelRoutePolicy.__module__ == "packages.model_runtime.routing.policies"


def test_empty_and_deep_copies_preserve_semantics_and_concrete_types():
    for model_type, data in (
        (ModelSpec, model_data()),
        (PromptSpec, prompt_data()),
        (ModelRoutePolicy, route_data()),
    ):
        original = model_type.model_validate(data)
        for copied in (original.model_copy(), original.model_copy(update={}), original.model_copy(deep=True)):
            assert type(copied) is model_type
            assert copied == original
            assert copied.model_dump_json() == original.model_dump_json()


def test_valid_copies_are_revalidated_and_keep_original_isolated():
    model = ModelSpec.model_validate(model_data())
    assert model.model_copy(update={"version": "2.0.0"}).version == "2.0.0"
    assert model.version == "1.0.0"

    prompt = PromptSpec.model_validate(prompt_data())
    metadata = {"b": 2, "a": {"x": [1, 2]}}
    copied_prompt = prompt.model_copy(update={"checksum": "b" * 64, "metadata": metadata})
    assert copied_prompt.checksum == "b" * 64
    assert type(copied_prompt.metadata).__name__ == "FrozenJsonObject"
    metadata["a"]["x"].append(3)
    assert copied_prompt.model_dump_json() == PromptSpec.model_validate_json(copied_prompt.model_dump_json()).model_dump_json()
    assert prompt.checksum == "sha256:" + "a" * 64

    policy = ModelRoutePolicy.model_validate(route_data())
    copied_policy = policy.model_copy(update={"status": "ELIGIBLE", "eligible": True})
    assert copied_policy.status is RouteStatus.ELIGIBLE
    assert copied_policy.eligible is True


@pytest.mark.parametrize(
    "update",
    [
        {"version": "latest"},
        {"capabilities": []},
        {"context_limits": {"max_input_tokens": 0, "max_output_tokens": 10}},
        {"unknown_field": "x"},
    ],
)
def test_model_spec_copy_rejects_invalid_updates(update):
    with pytest.raises(ValidationError):
        ModelSpec.model_validate(model_data()).model_copy(update=update)


@pytest.mark.parametrize(
    "update",
    [
        {"metadata": {"opaque": object()}},
        {"metadata": {"value": float("nan")}},
        {"output_contract_id": "unknown-contract"},
        {"output_contract_version": "999.0.0"},
        {"checksum": "bad"},
        {"version": "latest"},
        {"not_a_field": 1},
    ],
)
def test_prompt_spec_copy_rejects_invalid_updates(update):
    with pytest.raises(ValidationError):
        PromptSpec.model_validate(prompt_data()).model_copy(update=update)


@pytest.mark.parametrize(
    "update",
    [
        {"status": "ELIGIBLE", "eligible": True, "primary_model": None},
        {"category": "CLINICAL", "status": "ELIGIBLE", "eligible": True},
        {
            "primary_model": None,
            "fallback_models": [{"provider_id": "demo-provider", "model_id": "demo-model-v2", "model_version": "2.0.0"}],
        },
        {"version": "latest"},
        {
            "primary_model": {"provider_id": "demo-provider", "model_id": "demo-model-v1", "model_version": "latest"}
        },
        {"not_a_field": 1},
    ],
)
def test_route_policy_copy_rejects_invalid_updates(update):
    with pytest.raises(ValidationError):
        ModelRoutePolicy.model_validate(route_data()).model_copy(update=update)


def test_nested_structural_models_use_the_same_validated_copy_boundary():
    models_and_updates = (
        (ContextLimits(max_input_tokens=10, max_output_tokens=5), {"max_input_tokens": 0}),
        (TimeoutPolicy(timeout_ms=10), {"timeout_ms": 0}),
        (
            CostMetadata(
                currency="USD",
                input_microunits_per_million_tokens=0,
                output_microunits_per_million_tokens=0,
            ),
            {"currency": "usd"},
        ),
        (PromptVariable(name="color", value_type="string"), {"required": "true"}),
        (RouteMatch(task_type="classify-color"), {"task_type": "Bad"}),
        (
            ModelReference(provider_id="demo-provider", model_id="demo-model-v1", model_version="1.0.0"),
            {"model_version": "latest"},
        ),
    )
    for model, update in models_and_updates:
        with pytest.raises(ValidationError):
            model.model_copy(update=update)


def test_nested_structural_models_allow_valid_updates():
    limits = ContextLimits(max_input_tokens=10, max_output_tokens=5)
    assert limits.model_copy(update={"max_input_tokens": 20}).max_input_tokens == 20
    reference = ModelReference(provider_id="demo-provider", model_id="demo-model-v1", model_version="1.0.0")
    assert reference.model_copy(update={"model_version": "2.0.0"}).model_version == "2.0.0"
