"""Packaged Prompt Loader and checksum tests."""

from pathlib import Path

import pytest

from packages.model_runtime.api import PromptSpec
from packages.model_runtime.prompts import (
    PromptErrorCode,
    PromptLoader,
    PromptRegistry,
    PromptRegistryEntry,
    PromptRuntimeError,
    compute_prompt_checksum,
    parse_prompt_document,
)


RESOURCE = Path("packages/model_runtime/resources/prompts/classify-color/1.0.0.yaml")


def registry(resource="classify-color/1.0.0.yaml"):
    return PromptRegistry(
        [PromptRegistryEntry(prompt_id="classify-color", version="1.0.0", resource=resource)]
    )


def test_loader_reads_exact_registered_packaged_resource():
    document = PromptLoader(registry()).load("classify-color", "1.0.0")
    assert isinstance(document.spec, PromptSpec)
    assert document.spec.prompt_id == "classify-color"
    assert document.spec.version == "1.0.0"
    assert compute_prompt_checksum(document) == document.spec.checksum


@pytest.mark.parametrize(
    "raw_transform",
    [
        lambda raw: raw,
        lambda raw: raw.replace(b"\n", b"\r\n"),
        lambda raw: b"\xef\xbb\xbf" + raw,
        lambda raw: raw.rstrip(b"\n"),
        lambda raw: raw + b"\n# insignificant comment\n",
    ],
)
def test_checksum_is_stable_across_text_encodings(raw_transform):
    raw = RESOURCE.read_bytes()
    expected = compute_prompt_checksum(parse_prompt_document(raw))
    assert compute_prompt_checksum(parse_prompt_document(raw_transform(raw))) == expected


def test_checksum_is_stable_across_yaml_key_order_and_whitespace():
    raw = RESOURCE.read_text(encoding="utf-8")
    reordered = """messages:
  - content: Classify the supplied synthetic color name into basic color categories.
    role: system
  - content: 'Color: ${color_name}'
    role: user
spec:
  metadata: {purpose: synthetic-color-classification}
  status: DRAFT
  checksum: 759d7994cfddc74fc281f800206fd24ed0313b5d6162651a0b7cc5ef79541b73
  output_contract_version: 1.0.0
  output_contract_id: tool-result
  variables:
    - {required: true, value_type: string, name: color_name}
  version: 1.0.0
  prompt_id: classify-color
"""
    assert compute_prompt_checksum(parse_prompt_document(raw.encode())) == compute_prompt_checksum(
        parse_prompt_document(reordered.encode())
    )


def test_semantic_mutations_change_checksum():
    raw = RESOURCE.read_text(encoding="utf-8")
    original = compute_prompt_checksum(parse_prompt_document(raw.encode()))
    changed_template = raw.replace("Color: ${color_name}", "Synthetic color: ${color_name}")
    assert compute_prompt_checksum(parse_prompt_document(changed_template.encode())) != original
    changed_order = raw.replace(
        "  - role: system\n    content: Classify the supplied synthetic color name into basic color categories.\n  - role: user\n    content: \"Color: ${color_name}\"",
        "  - role: user\n    content: \"Color: ${color_name}\"\n  - role: system\n    content: Classify the supplied synthetic color name into basic color categories.",
    )
    assert compute_prompt_checksum(parse_prompt_document(changed_order.encode())) != original


def test_loader_rejects_checksum_mismatch():
    document = parse_prompt_document(RESOURCE.read_bytes())
    bad = document.model_copy(update={"spec": document.spec.model_copy(update={"checksum": "b" * 64})})
    assert compute_prompt_checksum(bad) != bad.spec.checksum


def test_loader_stops_on_checksum_mismatch(monkeypatch, tmp_path):
    package_root = tmp_path / "prompts"
    resource_dir = package_root / "classify-color"
    resource_dir.mkdir(parents=True)
    bad = RESOURCE.read_text(encoding="utf-8").replace(
        "759d7994cfddc74fc281f800206fd24ed0313b5d6162651a0b7cc5ef79541b73",
        "b" * 64,
    )
    (resource_dir / "1.0.0.yaml").write_text(bad, encoding="utf-8")
    monkeypatch.setattr("packages.model_runtime.prompts.loader.resources.files", lambda _package: package_root)
    with pytest.raises(PromptRuntimeError) as error:
        PromptLoader(registry()).load("classify-color", "1.0.0")
    assert error.value.code is PromptErrorCode.PROMPT_CHECKSUM_MISMATCH


def test_loader_has_no_implicit_version_or_discovery_api():
    loader = PromptLoader(registry())
    assert not hasattr(loader, "load_latest")
    assert not hasattr(loader, "discover")
    with pytest.raises(TypeError):
        loader.load("classify-color")


def test_loader_missing_resource_is_deterministic():
    missing_registry = PromptRegistry(
        [PromptRegistryEntry(prompt_id="classify-color", version="9.0.0", resource="classify-color/9.0.0.yaml")]
    )
    loader = PromptLoader(missing_registry)
    with pytest.raises(PromptRuntimeError) as error:
        loader.load("classify-color", "9.0.0")
    assert error.value.code is PromptErrorCode.PROMPT_RESOURCE_NOT_FOUND
