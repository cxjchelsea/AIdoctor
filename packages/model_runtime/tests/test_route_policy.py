"""Fail-closed structural tests for ModelRoutePolicy."""

import pytest
from pydantic import ValidationError

from packages.model_runtime.routing import ModelRoutePolicy, RouteCategory, RouteStatus


def route_data():
    return {
        "route_id": "classify-color",
        "version": "1.0.0",
        "category": "NON_CLINICAL",
        "match": {"task_type": "classify-color", "required_tags": ["synthetic"]},
        "required_capabilities": ["echo-structured-input"],
        "primary_model": {
            "provider_id": "demo-provider",
            "model_id": "demo-model-v1",
            "model_version": "1.0.0",
        },
        "fallback_models": [
            {
                "provider_id": "demo-provider",
                "model_id": "demo-model-v2",
                "model_version": "2.0.0",
            }
        ],
    }


def test_valid_policy_is_declarative_and_ineligible():
    policy = ModelRoutePolicy.model_validate(route_data())
    assert policy.status is RouteStatus.BLOCKED
    assert policy.eligible is False
    assert not hasattr(policy, "execute")
    assert not hasattr(policy, "invoke")


def test_unknown_route_defaults_fail_closed():
    data = route_data()
    data.pop("category")
    policy = ModelRoutePolicy.model_validate(data)
    assert policy.category is RouteCategory.UNKNOWN
    assert policy.status is RouteStatus.BLOCKED
    assert policy.eligible is False


def test_clinical_route_is_blocked_and_ineligible():
    data = route_data()
    data["category"] = "CLINICAL"
    policy = ModelRoutePolicy.model_validate(data)
    assert policy.status is RouteStatus.BLOCKED
    assert policy.eligible is False


@pytest.mark.parametrize("category", ["UNKNOWN", "CLINICAL"])
def test_unknown_and_clinical_routes_cannot_be_disabled_instead_of_blocked(category):
    data = route_data()
    data.update(category=category, status="DISABLED")
    with pytest.raises(ValidationError):
        ModelRoutePolicy.model_validate(data)


def test_eligibility_and_status_fail_closed():
    data = route_data()
    data["category"] = "CLINICAL"
    data["eligible"] = True
    with pytest.raises(ValidationError):
        ModelRoutePolicy.model_validate(data)
    data = route_data()
    data["status"] = "ACTIVE"
    with pytest.raises(ValidationError):
        ModelRoutePolicy.model_validate(data)


def test_non_clinical_policy_can_declare_structural_eligibility_without_execution():
    data = route_data()
    data["status"] = "ELIGIBLE"
    data["eligible"] = True
    policy = ModelRoutePolicy.model_validate(data)
    assert policy.category is RouteCategory.NON_CLINICAL
    assert policy.eligible is True
    assert not hasattr(policy, "execute")


def test_non_clinical_eligibility_fields_must_agree():
    data = route_data()
    data["eligible"] = True
    with pytest.raises(ValidationError):
        ModelRoutePolicy.model_validate(data)
    data = route_data()
    data["status"] = "ELIGIBLE"
    with pytest.raises(ValidationError):
        ModelRoutePolicy.model_validate(data)


def test_fallback_topology_is_declarative_and_unique():
    policy = ModelRoutePolicy.model_validate(route_data())
    assert policy.fallback_models[0].model_id == "demo-model-v2"
    data = route_data()
    data["fallback_models"] = [data["primary_model"]]
    with pytest.raises(ValidationError):
        ModelRoutePolicy.model_validate(data)


def test_provider_model_reference_requires_explicit_version():
    data = route_data()
    data["primary_model"]["model_version"] = "latest"
    with pytest.raises(ValidationError):
        ModelRoutePolicy.model_validate(data)
