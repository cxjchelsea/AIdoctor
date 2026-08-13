"""Minimal deterministic Prompt Builder tests."""

import math

import pytest

from packages.model_runtime.api import PromptSpec, PromptVariable
from packages.model_runtime.prompts import (
    PromptBuilder,
    PromptDocument,
    PromptErrorCode,
    PromptLoader,
    PromptMessage,
    PromptRegistry,
    PromptRegistryEntry,
    PromptRuntimeError,
)


def document():
    registry = PromptRegistry(
        [PromptRegistryEntry(prompt_id="classify-color", version="1.0.0", resource="classify-color/1.0.0.yaml")]
    )
    return PromptLoader(registry).load("classify-color", "1.0.0")


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


def typed_document(value_type):
    spec = PromptSpec(
        prompt_id="render-value",
        version="1.0.0",
        variables=(PromptVariable(name="value", value_type=value_type),),
        output_contract_id="tool-result",
        output_contract_version="1.0.0",
        checksum="a" * 64,
    )
    return PromptDocument(spec=spec, messages=(PromptMessage(role="user", content="Value: ${value}"),))


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
    ],
)
def test_value_types_reject_wrong_and_non_finite_values(value_type, value):
    with pytest.raises(PromptRuntimeError) as error:
        PromptBuilder._validate_and_render_value("value", value_type, value)
    assert error.value.code is PromptErrorCode.PROMPT_VARIABLE_TYPE_INVALID


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
