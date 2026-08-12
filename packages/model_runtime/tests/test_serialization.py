"""Deterministic offline serialization tests for P1 structures."""

from packages.model_runtime.api import ModelSpec, PromptSpec
from packages.model_runtime.routing import ModelRoutePolicy

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
