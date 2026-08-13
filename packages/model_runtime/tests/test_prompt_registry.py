"""Exact-version immutable Prompt Registry tests."""

import pytest

from packages.model_runtime.prompts import (
    PromptErrorCode,
    PromptRegistry,
    PromptRegistryEntry,
    PromptRuntimeError,
)


def entry(prompt_id="classify-color", version="1.0.0"):
    return PromptRegistryEntry(
        prompt_id=prompt_id,
        version=version,
        resource=f"{prompt_id}/{version}.yaml",
    )


def test_registry_defaults_empty_without_discovery_or_side_effects():
    registry = PromptRegistry()
    assert len(registry) == 0
    assert registry.list_versions("classify-color") == ()
    assert not hasattr(registry, "load_latest")
    assert not hasattr(registry, "register")
    assert not hasattr(registry, "discover")


def test_registry_exact_lookup_and_deterministic_versions():
    registry = PromptRegistry([entry(version="2.0.0"), entry(version="1.0.0")])
    assert registry.contains("classify-color", "1.0.0")
    assert registry.get("classify-color", "1.0.0") == entry()
    assert registry.list_versions("classify-color") == ("1.0.0", "2.0.0")


def test_registry_unknown_id_and_version_have_distinct_stable_errors():
    registry = PromptRegistry([entry()])
    with pytest.raises(PromptRuntimeError) as missing_prompt:
        registry.get("echo-color", "1.0.0")
    assert missing_prompt.value.code is PromptErrorCode.PROMPT_NOT_REGISTERED
    with pytest.raises(PromptRuntimeError) as missing_version:
        registry.get("classify-color", "9.0.0")
    assert missing_version.value.code is PromptErrorCode.PROMPT_VERSION_NOT_REGISTERED


def test_registry_rejects_duplicates_and_is_immutable():
    with pytest.raises(ValueError, match="duplicate prompt registry entry"):
        PromptRegistry([entry(), entry()])
    registry = PromptRegistry([entry()])
    with pytest.raises(TypeError, match="immutable"):
        registry.other = "value"


def test_registry_entry_resource_must_match_exact_identity():
    with pytest.raises(ValueError, match="exactly match"):
        PromptRegistryEntry(prompt_id="classify-color", version="1.0.0", resource="other/1.0.0.yaml")


@pytest.mark.parametrize("prompt_id", ["classify-color", "classify_color", "demo.classify-color"])
def test_registry_resource_supports_full_p1_identifier_grammar(prompt_id):
    assert entry(prompt_id=prompt_id).resource == f"{prompt_id}/1.0.0.yaml"
