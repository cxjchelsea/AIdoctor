"""Adversarial Prompt Loader boundary tests."""

from pathlib import Path

import pytest
from pydantic import ValidationError

from packages.model_runtime.prompts import (
    PromptErrorCode,
    PromptLoader,
    PromptRegistryEntry,
    PromptRuntimeError,
    parse_prompt_document,
)


@pytest.mark.parametrize(
    "resource",
    [
        "../secret.yaml",
        "/absolute/secret.yaml",
        "classify-color",
        "classify-color/1.0.0.txt",
        "classify-color/subdir/1.0.0.yaml",
        "./1.0.0.yaml",
    ],
)
def test_registry_entry_rejects_forbidden_resource_identifiers(resource):
    with pytest.raises(ValidationError):
        PromptRegistryEntry(prompt_id="classify-color", version="1.0.0", resource=resource)


@pytest.mark.parametrize(
    "raw",
    [
        b"x: !!python/object/apply:os.system ['echo unsafe']\n",
        b"version: 1.0.0\nversion: 2.0.0\n",
        b"1: value\n",
        b"first: document\n---\nsecond: document\n",
        b"\xff\xfeinvalid",
        b"spec: {prompt_id: classify-color}\nmessages: []\n",
        b"spec:\n  prompt_id: classify-color\n  version: 1.0.0\n  variables: []\n  output_contract_id: unknown-contract\n  output_contract_version: 1.0.0\n  checksum: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\nmessages: [{role: user, content: x}]\n",
        b"spec:\n  prompt_id: classify-color\n  version: 1.0.0\n  variables: []\n  output_contract_id: tool-result\n  output_contract_version: 999.0.0\n  checksum: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\nmessages: [{role: user, content: x}]\n",
    ],
)
def test_strict_parser_rejects_unsafe_ambiguous_or_invalid_documents(raw):
    with pytest.raises(PromptRuntimeError) as error:
        parse_prompt_document(raw)
    assert error.value.code is PromptErrorCode.PROMPT_SCHEMA_INVALID


def test_resource_identifier_boundary_rejects_arbitrary_paths():
    for value in ("../secret.yaml", "/secret.yaml", "folder", "a/b/c.yaml", "a/b.txt"):
        with pytest.raises(PromptRuntimeError) as error:
            PromptLoader._validate_resource_identifier(value)
        assert error.value.code is PromptErrorCode.PROMPT_RESOURCE_FORBIDDEN


def test_synthetic_resource_contains_no_forbidden_clinical_identity():
    text = Path("packages/model_runtime/resources/prompts/classify-color/1.0.0.yaml").read_text(encoding="utf-8")
    lowered = text.lower()
    for forbidden in ("patient", "diagnosis", "treatment", "medical threshold", "data-ev001", "data-ev002", "data-ev003"):
        assert forbidden not in lowered
