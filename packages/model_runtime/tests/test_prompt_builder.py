"""Minimal deterministic Prompt Builder tests."""

import math

import pytest
from pydantic import ValidationError

from packages.model_runtime.api import PromptSpec, PromptVariable
from packages.model_runtime.api.types import FrozenJsonObject
from packages.model_runtime.prompts import (
    PromptBuilder,
    PromptDocument,
    PromptErrorCode,
    PromptLoader,
    PromptMessage,
    PromptRegistry,
    PromptRegistryEntry,
    PromptRuntimeError,
    RenderedMessage,
    RenderedPrompt,
    compute_prompt_checksum,
)


def document():
    registry = PromptRegistry(
        [PromptRegistryEntry(prompt_id="classify-color", version="1.0.0", resource="classify-color/1.0.0.yaml")]
    )
    return PromptLoader(registry).load("classify-color", "1.0.0")


def with_valid_checksum(prompt_document: PromptDocument) -> PromptDocument:
    """为直接构造的 PromptDocument 写入与内容匹配的 checksum。"""

    digest = compute_prompt_checksum(prompt_document)
    return prompt_document.model_copy(update={"spec": prompt_document.spec.model_copy(update={"checksum": digest})})


def typed_document(value_type):
    provisional = PromptDocument(
        spec=PromptSpec(
            prompt_id="render-value",
            version="1.0.0",
            variables=(PromptVariable(name="value", value_type=value_type),),
            output_contract_id="tool-result",
            output_contract_version="1.0.0",
            checksum="a" * 64,
        ),
        messages=(PromptMessage(role="user", content="Value: ${value}"),),
    )
    return with_valid_checksum(provisional)


def test_builder_renders_synthetic_prompt_deterministically():
    builder = PromptBuilder()
    first = builder.build(document(), {"color_name": "blue"})
    second = builder.build(document(), {"color_name": "blue"})
    assert first == second
    assert first.messages[1].content == "Color: blue"
    assert first.manifest["prompt_id"] == "classify-color"
    assert not hasattr(first, "provider")
    assert not hasattr(first, "route")


def test_builder_rejects_missing_and_extra_variables():
    builder = PromptBuilder()
    with pytest.raises(PromptRuntimeError) as missing:
        builder.build(document(), {})
    assert missing.value.code is PromptErrorCode.PROMPT_REQUIRED_VARIABLE_MISSING
    with pytest.raises(PromptRuntimeError) as extra:
        builder.build(document(), {"color_name": "blue", "extra": 1})
    assert extra.value.code is PromptErrorCode.PROMPT_EXTRA_VARIABLE


def test_value_types_and_canonical_rendering():
    render = PromptBuilder._validate_and_render_value
    assert render("value", "string", "x") == "x"
    assert render("value", "integer", 2) == "2"
    assert render("value", "number", 2.5) == "2.5"
    assert render("value", "boolean", True) == "true"
    assert render("value", "object", {"b": 2, "a": 1}) == '{"a":1,"b":2}'
    assert render("value", "array", [2, {"b": 2, "a": 1}]) == '[2,{"a":1,"b":2}]'
    assert render("value", "object", {"a": 1, "b": 2}) == render("value", "object", {"b": 2, "a": 1})


@pytest.mark.parametrize(
    "value_type,value,expected",
    [
        ("string", "text", "text"),
        ("integer", 2, "2"),
        ("number", 2.5, "2.5"),
        ("boolean", False, "false"),
        ("object", {"b": 2, "a": 1}, '{"a":1,"b":2}'),
        ("array", [2, {"b": 2, "a": 1}], '[2,{"a":1,"b":2}]'),
    ],
)
def test_public_builder_supports_all_six_variable_types(value_type, value, expected):
    rendered = PromptBuilder().build(typed_document(value_type), {"value": value})
    assert rendered.messages[0].content == f"Value: {expected}"


@pytest.mark.parametrize(
    "value_type,value",
    [
        ("integer", True),
        ("number", True),
        ("boolean", 1),
        ("object", []),
        ("array", {}),
        ("number", float("nan")),
        ("number", float("inf")),
        ("number", float("-inf")),
        ("object", {"x": float("nan")}),
        ("array", [float("inf")]),
        ("object", {1: "value"}),
        ("object", {"x": {1: "value"}}),
        ("object", {"x": (1, 2)}),
        ("array", [(1, 2)]),
        ("array", [{"x": (1, 2)}]),
        ("object", {"x": object()}),
        ("object", {"x": {b"k": "v"}}),
    ],
)
def test_value_types_reject_wrong_non_finite_and_non_json_native_values(value_type, value):
    with pytest.raises(PromptRuntimeError) as error:
        PromptBuilder._validate_and_render_value("value", value_type, value)
    assert error.value.code is PromptErrorCode.PROMPT_VARIABLE_TYPE_INVALID


def test_strict_json_native_object_and_array_accept_string_keys_and_lists():
    assert PromptBuilder._validate_and_render_value("value", "object", {"1": "value"}) == '{"1":"value"}'
    assert PromptBuilder._validate_and_render_value("value", "object", {"x": {"1": "value"}}) == '{"x":{"1":"value"}}'
    assert PromptBuilder._validate_and_render_value("value", "object", {"x": [1, 2]}) == '{"x":[1,2]}'
    assert PromptBuilder._validate_and_render_value("value", "array", [1, 2]) == "[1,2]"


@pytest.mark.parametrize(
    "template",
    ["${unknown}", "${obj.attr}", "${func()}", "${a + b}", "${MALFORMED}", "${unterminated"],
)
def test_renderer_rejects_unknown_expressions_and_malformed_placeholders(template):
    with pytest.raises(PromptRuntimeError) as error:
        PromptBuilder._render_template(template, {"known": "value"})
    assert error.value.code is PromptErrorCode.PROMPT_RENDER_INVALID


def test_jinja_like_text_is_literal_and_never_executed():
    text = PromptBuilder._render_template("{{ jinja }} and $PATH", {})
    assert text == "{{ jinja }} and $PATH"


def test_builder_rejects_direct_document_with_wrong_checksum():
    """A7-NC-P2-REV-F001：公开 Builder 不得接受 checksum 不匹配的 PromptDocument。"""

    bad = PromptDocument(
        spec=PromptSpec(
            prompt_id="classify-color",
            version="1.0.0",
            variables=(PromptVariable(name="color_name", value_type="string", required=True),),
            output_contract_id="tool-result",
            output_contract_version="1.0.0",
            checksum="a" * 64,
            metadata=FrozenJsonObject({"purpose": "synthetic-color-classification"}),
        ),
        messages=(
            PromptMessage(role="system", content="Classify the supplied synthetic color name into basic color categories."),
            PromptMessage(role="user", content="Color: ${color_name}"),
        ),
    )
    assert bad.spec.checksum.removeprefix("sha256:") != compute_prompt_checksum(bad)
    with pytest.raises(PromptRuntimeError) as error:
        PromptBuilder().build(bad, {"color_name": "red"})
    assert error.value.code is PromptErrorCode.PROMPT_CHECKSUM_MISMATCH


def test_builder_accepts_direct_document_with_correct_checksum_and_loader_path():
    """F001：正确 checksum 的直接构造与 Loader 路径均可 build。"""

    loaded = document()
    rendered_loader = PromptBuilder().build(loaded, {"color_name": "blue"})
    assert rendered_loader.resource_checksum == compute_prompt_checksum(loaded)

    direct = with_valid_checksum(
        PromptDocument(
            spec=PromptSpec(
                prompt_id="classify-color",
                version="1.0.0",
                variables=(PromptVariable(name="color_name", value_type="string", required=True),),
                output_contract_id="tool-result",
                output_contract_version="1.0.0",
                checksum="a" * 64,
                metadata=FrozenJsonObject({"purpose": "synthetic-color-classification"}),
            ),
            messages=(
                PromptMessage(
                    role="system",
                    content="Classify the supplied synthetic color name into basic color categories.",
                ),
                PromptMessage(role="user", content="Color: ${color_name}"),
            ),
        )
    )
    rendered_direct = PromptBuilder().build(direct, {"color_name": "blue"})
    assert rendered_direct.resource_checksum == compute_prompt_checksum(direct)
    assert rendered_direct.messages[1].content == "Color: blue"


def test_builder_rejects_semantic_mutation_without_checksum_update():
    """语义变更但未更新 checksum 时，Builder 必须拒绝。"""

    original = document()
    mutated = original.model_copy(
        update={"messages": (PromptMessage(role="user", content="mutated ${color_name}"),)}
    )
    assert mutated.spec.checksum == original.spec.checksum
    assert compute_prompt_checksum(mutated) != original.spec.checksum.removeprefix("sha256:")
    with pytest.raises(PromptRuntimeError) as error:
        PromptBuilder().build(mutated, {"color_name": "red"})
    assert error.value.code is PromptErrorCode.PROMPT_CHECKSUM_MISMATCH


def test_rendered_message_rejects_invalid_role():
    """A7-NC-P2-REV-F003。"""

    RenderedMessage(role="user", content="x")
    with pytest.raises(ValidationError):
        RenderedMessage(role="anything", content="x")


def test_rendered_prompt_rejects_invalid_direct_construction():
    """A7-NC-P2-REV-F004。"""

    valid = PromptBuilder().build(document(), {"color_name": "blue"})
    assert valid.messages

    with pytest.raises(ValidationError):
        RenderedPrompt(
            prompt_id="!!!",
            version="1.0.0",
            messages=(RenderedMessage(role="user", content="x"),),
            resource_checksum="a" * 64,
            manifest=FrozenJsonObject(
                {"prompt_id": "!!!", "prompt_version": "1.0.0", "resource_checksum": "a" * 64}
            ),
        )
    with pytest.raises(ValidationError):
        RenderedPrompt(
            prompt_id="classify-color",
            version="latest",
            messages=(RenderedMessage(role="user", content="x"),),
            resource_checksum="a" * 64,
            manifest=FrozenJsonObject(
                {
                    "prompt_id": "classify-color",
                    "prompt_version": "latest",
                    "resource_checksum": "a" * 64,
                }
            ),
        )
    with pytest.raises(ValidationError):
        RenderedPrompt(
            prompt_id="classify-color",
            version="1.0.0",
            messages=(),
            resource_checksum="a" * 64,
            manifest=FrozenJsonObject(
                {
                    "prompt_id": "classify-color",
                    "prompt_version": "1.0.0",
                    "resource_checksum": "a" * 64,
                }
            ),
        )
    with pytest.raises(ValidationError):
        RenderedPrompt(
            prompt_id="classify-color",
            version="1.0.0",
            messages=(RenderedMessage(role="user", content="x"),),
            resource_checksum="not-a-hash",
            manifest=FrozenJsonObject(
                {
                    "prompt_id": "classify-color",
                    "prompt_version": "1.0.0",
                    "resource_checksum": "not-a-hash",
                }
            ),
        )
    with pytest.raises(ValidationError):
        RenderedPrompt(
            prompt_id="classify-color",
            version="1.0.0",
            messages=(RenderedMessage(role="user", content="x"),),
            resource_checksum="a" * 64,
            manifest=FrozenJsonObject(
                {
                    "prompt_id": "other-id",
                    "prompt_version": "1.0.0",
                    "resource_checksum": "a" * 64,
                }
            ),
        )
