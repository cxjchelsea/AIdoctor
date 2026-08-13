"""EG-13 / NC-12：business code 不得直接 import provider SDK。"""

from __future__ import annotations

import textwrap
from pathlib import Path

import pytest

from packages.model_runtime.architecture.provider_sdk_import_rule import (
    RULE_ID,
    ForbiddenProviderSdkImport,
    scan_provider_sdk_imports,
    scan_repository_business_code,
)


REPO_ROOT = Path(__file__).resolve().parents[3]


def test_detects_import_openai(tmp_path: Path) -> None:
    sample = tmp_path / "svc.py"
    sample.write_text("import openai\n", encoding="utf-8")
    violations = scan_provider_sdk_imports(sample)
    assert len(violations) == 1
    assert violations[0].module == "openai"
    assert violations[0].rule_id == RULE_ID
    assert violations[0].line == 1


def test_detects_import_openai_as_alias(tmp_path: Path) -> None:
    sample = tmp_path / "svc.py"
    sample.write_text("import openai as sdk\n", encoding="utf-8")
    violations = scan_provider_sdk_imports(sample)
    assert violations[0].module == "openai"


def test_detects_from_openai_import(tmp_path: Path) -> None:
    sample = tmp_path / "svc.py"
    sample.write_text("from openai import OpenAI\n", encoding="utf-8")
    violations = scan_provider_sdk_imports(sample)
    assert violations[0].module == "openai"


def test_detects_langchain_openai(tmp_path: Path) -> None:
    sample = tmp_path / "svc.py"
    sample.write_text("from langchain_openai import ChatOpenAI\n", encoding="utf-8")
    violations = scan_provider_sdk_imports(sample)
    assert violations[0].module == "langchain_openai"


def test_detects_constant_dynamic_import(tmp_path: Path) -> None:
    sample = tmp_path / "svc.py"
    sample.write_text('import importlib\nimportlib.import_module("openai")\n', encoding="utf-8")
    violations = scan_provider_sdk_imports(sample)
    assert any(item.module == "openai" for item in violations)


def test_detects_dunder_import_constant(tmp_path: Path) -> None:
    sample = tmp_path / "svc.py"
    sample.write_text('__import__("anthropic")\n', encoding="utf-8")
    violations = scan_provider_sdk_imports(sample)
    assert any(item.module == "anthropic" for item in violations)


def test_comments_and_docstrings_are_not_violations(tmp_path: Path) -> None:
    sample = tmp_path / "svc.py"
    sample.write_text(
        textwrap.dedent(
            '''
            """This docstring mentions import openai for documentation only."""
            # import openai
            from packages.model_runtime import ModelGateway  # noqa: F401
            '''
        ).strip()
        + "\n",
        encoding="utf-8",
    )
    assert scan_provider_sdk_imports(sample) == []


def test_httpx_alone_is_not_provider_sdk_violation(tmp_path: Path) -> None:
    sample = tmp_path / "svc.py"
    sample.write_text("import httpx\n", encoding="utf-8")
    assert scan_provider_sdk_imports(sample) == []


def test_generic_langchain_without_provider_binding_is_not_flagged(tmp_path: Path) -> None:
    sample = tmp_path / "svc.py"
    sample.write_text("import langchain\nfrom langchain.schema import Document\n", encoding="utf-8")
    assert scan_provider_sdk_imports(sample) == []


def test_diagnostics_are_deterministically_sorted(tmp_path: Path) -> None:
    sample = tmp_path / "svc.py"
    sample.write_text(
        "import anthropic\nimport openai\nfrom langchain_openai import ChatOpenAI\n",
        encoding="utf-8",
    )
    violations = scan_provider_sdk_imports(sample)
    triples = [(item.path, item.line, item.module) for item in violations]
    assert triples == sorted(triples)


def test_repository_business_code_has_zero_provider_sdk_imports() -> None:
    """EG-13：business-code provider-SDK direct imports = 0。"""
    violations = scan_repository_business_code(REPO_ROOT)
    assert violations == [], [
        f"{item.path}:{item.line}:{item.module}" for item in violations
    ]


def test_violation_type_is_stable() -> None:
    assert RULE_ID == "P5_PROVIDER_SDK_DIRECT_IMPORT"
    sample = ForbiddenProviderSdkImport(path="x.py", line=1, module="openai")
    assert sample.rule_id == RULE_ID
    assert sample.module == "openai"
    assert "module" in ForbiddenProviderSdkImport.__dataclass_fields__
