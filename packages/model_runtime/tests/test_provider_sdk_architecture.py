"""EG-13 / NC-12：business code 不得直接 import provider SDK。"""

from __future__ import annotations

import textwrap
from pathlib import Path

import pytest

from packages.model_runtime.architecture.provider_sdk_import_rule import (
    PARSE_FAILURE_RULE_ID,
    RULE_ID,
    ForbiddenProviderSdkImport,
    ProviderSdkArchitectureScanError,
    scan_provider_sdk_imports,
    scan_repository_business_code,
)


REPO_ROOT = Path(__file__).resolve().parents[3]


def _write(sample: Path, source: str) -> Path:
    sample.write_text(source, encoding="utf-8")
    return sample


def test_detects_import_openai(tmp_path: Path) -> None:
    sample = _write(tmp_path / "svc.py", "import openai\n")
    violations = scan_provider_sdk_imports(sample)
    assert len(violations) == 1
    assert violations[0].module == "openai"
    assert violations[0].rule_id == RULE_ID
    assert violations[0].line == 1


def test_detects_import_openai_as_alias(tmp_path: Path) -> None:
    sample = _write(tmp_path / "svc.py", "import openai as sdk\n")
    assert scan_provider_sdk_imports(sample)[0].module == "openai"


def test_detects_from_openai_import(tmp_path: Path) -> None:
    sample = _write(tmp_path / "svc.py", "from openai import OpenAI\n")
    assert scan_provider_sdk_imports(sample)[0].module == "openai"


def test_detects_langchain_openai(tmp_path: Path) -> None:
    sample = _write(tmp_path / "svc.py", "from langchain_openai import ChatOpenAI\n")
    assert scan_provider_sdk_imports(sample)[0].module == "langchain_openai"


def test_detects_provider_bound_langchain_variants(tmp_path: Path) -> None:
    cases = [
        "from langchain_community.llms import Ollama\n",
        "from langchain_community.chat_models import ChatOpenAI\n",
        "from langchain.llms import OpenAI\n",
        "from langchain.chat_models import ChatOpenAI\n",
    ]
    for source in cases:
        assert scan_provider_sdk_imports(_write(tmp_path / "svc.py", source))


def test_detects_constant_dynamic_import(tmp_path: Path) -> None:
    sample = _write(
        tmp_path / "svc.py",
        'import importlib\nimportlib.import_module("openai")\n',
    )
    assert any(item.module == "openai" for item in scan_provider_sdk_imports(sample))


def test_detects_importlib_module_alias(tmp_path: Path) -> None:
    sample = _write(
        tmp_path / "svc.py",
        'import importlib as il\nil.import_module("openai")\n',
    )
    assert any(item.module == "openai" for item in scan_provider_sdk_imports(sample))


def test_detects_from_importlib_import_module(tmp_path: Path) -> None:
    sample = _write(
        tmp_path / "svc.py",
        'from importlib import import_module\nimport_module("openai")\n',
    )
    assert any(item.module == "openai" for item in scan_provider_sdk_imports(sample))


def test_detects_from_importlib_import_module_as_load(tmp_path: Path) -> None:
    sample = _write(
        tmp_path / "svc.py",
        'from importlib import import_module as load\nload("anthropic")\n',
    )
    assert any(item.module == "anthropic" for item in scan_provider_sdk_imports(sample))


def test_detects_dunder_import_constant(tmp_path: Path) -> None:
    sample = _write(tmp_path / "svc.py", '__import__("anthropic")\n')
    assert any(item.module == "anthropic" for item in scan_provider_sdk_imports(sample))


def test_non_constant_dynamic_import_not_detected(tmp_path: Path) -> None:
    sample = _write(
        tmp_path / "svc.py",
        'from importlib import import_module\nname = "open" + "ai"\nimport_module(name)\n',
    )
    assert scan_provider_sdk_imports(sample) == []


def test_detects_google_and_azure_parent_package_forms(tmp_path: Path) -> None:
    cases = [
        ("from google import genai\n", "google.genai"),
        ("from google import generativeai\n", "google.generativeai"),
        ("from google.genai import Client\n", "google.genai"),
        ("import google.genai\n", "google.genai"),
        ("import google.generativeai\n", "google.generativeai"),
        ("from azure.ai import openai\n", "azure.ai.openai"),
        ("from azure.ai import inference\n", "azure.ai.inference"),
        ("import azure.ai.openai\n", "azure.ai.openai"),
        ("import azure.ai.inference\n", "azure.ai.inference"),
    ]
    for source, expected_module in cases:
        violations = scan_provider_sdk_imports(_write(tmp_path / "svc.py", source))
        assert any(item.module == expected_module for item in violations), (source, violations)


def test_non_provider_google_azure_parent_imports_allowed(tmp_path: Path) -> None:
    cases = [
        "from google import protobuf\n",
        "from azure import identity\n",
    ]
    for source in cases:
        assert scan_provider_sdk_imports(_write(tmp_path / "svc.py", source)) == []


def test_arbitrary_import_module_method_is_not_violation(tmp_path: Path) -> None:
    sample = _write(
        tmp_path / "svc.py",
        textwrap.dedent(
            """
            class Loader:
                def import_module(self, value):
                    return value
            loader = Loader()
            loader.import_module("openai")
            """
        ).strip()
        + "\n",
    )
    assert scan_provider_sdk_imports(sample) == []


def test_relative_openai_imports_are_not_violations(tmp_path: Path) -> None:
    assert scan_provider_sdk_imports(_write(tmp_path / "svc.py", "from .openai import LocalThing\n")) == []
    assert scan_provider_sdk_imports(_write(tmp_path / "svc.py", "from ..openai import OtherThing\n")) == []
    assert scan_provider_sdk_imports(_write(tmp_path / "svc.py", "from .google import genai\n")) == []


def test_comments_and_docstrings_are_not_violations(tmp_path: Path) -> None:
    sample = _write(
        tmp_path / "svc.py",
        textwrap.dedent(
            '''
            """This docstring mentions import openai for documentation only."""
            # import openai
            from packages.model_runtime import ModelGateway  # noqa: F401
            '''
        ).strip()
        + "\n",
    )
    assert scan_provider_sdk_imports(sample) == []


def test_httpx_alone_is_not_provider_sdk_violation(tmp_path: Path) -> None:
    assert scan_provider_sdk_imports(_write(tmp_path / "svc.py", "import httpx\n")) == []


def test_generic_langchain_without_provider_binding_is_not_flagged(tmp_path: Path) -> None:
    sample = _write(
        tmp_path / "svc.py",
        "import langchain\nfrom langchain.schema import Document\n",
    )
    assert scan_provider_sdk_imports(sample) == []


def test_diagnostics_are_deterministically_sorted(tmp_path: Path) -> None:
    sample = _write(
        tmp_path / "svc.py",
        "import anthropic\nimport openai\nfrom langchain_openai import ChatOpenAI\n",
    )
    violations = scan_provider_sdk_imports(sample)
    triples = [(item.path, item.line, item.module) for item in violations]
    assert triples == sorted(triples)


def test_unparseable_business_file_fail_closed(tmp_path: Path) -> None:
    """F001：included business 文件 SyntaxError 不得静默返回空违规。"""
    packages = tmp_path / "packages"
    packages.mkdir()
    bad = packages / "bad.py"
    bad.write_text("import openai\nthis is not valid python !!!\n", encoding="utf-8")

    with pytest.raises(ProviderSdkArchitectureScanError) as raised:
        scan_repository_business_code(tmp_path)

    error = raised.value
    assert error.path == "packages/bad.py"
    assert error.rule_id == PARSE_FAILURE_RULE_ID
    assert error.category == "UNPARSEABLE_BUSINESS_PYTHON"
    assert error.message == "unparseable business Python file"
    assert "import openai" not in error.message
    assert "D:" not in error.message and "C:" not in error.message


def test_parse_failure_diagnostics_are_deterministic(tmp_path: Path) -> None:
    packages = tmp_path / "packages"
    packages.mkdir()
    (packages / "broken.py").write_text("def (\n", encoding="utf-8")
    errors = []
    for _ in range(5):
        with pytest.raises(ProviderSdkArchitectureScanError) as raised:
            scan_repository_business_code(tmp_path)
        errors.append(
            (
                type(raised.value).__name__,
                raised.value.path,
                raised.value.line,
                raised.value.category,
                raised.value.rule_id,
                raised.value.message,
            )
        )
    assert all(item == errors[0] for item in errors)


def test_syntax_error_under_excluded_tests_does_not_fail_scan(tmp_path: Path) -> None:
    packages = tmp_path / "packages"
    tests = packages / "tests"
    tests.mkdir(parents=True)
    (tests / "broken.py").write_text("import openai\n@@@\n", encoding="utf-8")
    (packages / "ok.py").write_text("import httpx\n", encoding="utf-8")
    assert scan_repository_business_code(tmp_path) == []


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
