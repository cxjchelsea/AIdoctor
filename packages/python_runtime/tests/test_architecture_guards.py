"""POSTFREEZE-01 架构守卫：pytest + 标准库 AST，不依赖本机绝对路径。"""

from __future__ import annotations

import ast
from pathlib import Path

# 相对仓库根的业务扫描范围
_REPO_ROOT = Path(__file__).resolve().parents[3]
_PYTHON_RUNTIME = _REPO_ROOT / "packages" / "python_runtime"
_MODEL_RUNTIME = _REPO_ROOT / "packages" / "model_runtime"
_CAPABILITIES = _REPO_ROOT / "capabilities"

_EXCLUDED_DIR_NAMES = frozenset({"__pycache__", ".venv", "venv", "tests", "test", ".git"})

_FORBIDDEN_PROVIDER_ROOTS = frozenset(
    {
        "openai",
        "anthropic",
        "langchain_openai",
        "google.generativeai",
        "google.genai",
        "vertexai",
        "ollama",
        "langgraph",
    }
)

_ALLOWED_MODEL_RUNTIME_PREFIXES = (
    "packages.model_runtime",
    "packages.model_runtime.gateway",
    "packages.model_runtime.gateway.models",
    "packages.model_runtime.gateway.errors",
)


def _iter_production_python(root: Path):
    """遍历生产 Python 文件；排除测试目录。"""

    if not root.is_dir():
        return
    for path in root.rglob("*.py"):
        relative_parts = path.relative_to(root).parts
        if any(part in _EXCLUDED_DIR_NAMES for part in relative_parts):
            continue
        yield path


def _module_names(node: ast.AST):
    """收集 import / from-import 模块名。"""

    names = []
    if isinstance(node, ast.Import):
        names.extend(alias.name for alias in node.names)
    elif isinstance(node, ast.ImportFrom) and node.module:
        names.append(node.module)
    return names


def _imported_modules(path: Path):
    """解析单个文件的导入模块名。"""

    tree = ast.parse(path.read_text(encoding="utf-8"), filename=path.name)
    modules = []
    for node in ast.walk(tree):
        modules.extend(_module_names(node))
    return modules


def _repo_relative(path: Path) -> str:
    """返回仓库相对 POSIX 路径，避免盘符耦合。"""

    return path.relative_to(_REPO_ROOT).as_posix()


def test_model_runtime_production_does_not_import_python_runtime():
    """packages/model_runtime 生产代码不得反向依赖 python_runtime。"""

    violations = []
    for path in _iter_production_python(_MODEL_RUNTIME):
        for module_name in _imported_modules(path):
            if module_name == "packages.python_runtime" or module_name.startswith(
                "packages.python_runtime."
            ):
                violations.append(_repo_relative(path))
    assert violations == []


def test_python_runtime_does_not_import_provider_sdk_or_langgraph():
    """python_runtime 不得导入 provider SDK 或 langgraph。"""

    violations = []
    for path in _iter_production_python(_PYTHON_RUNTIME):
        for module_name in _imported_modules(path):
            for forbidden in _FORBIDDEN_PROVIDER_ROOTS:
                if module_name == forbidden or module_name.startswith(f"{forbidden}."):
                    violations.append(f"{_repo_relative(path)}:{module_name}")
    assert violations == []


def test_python_runtime_does_not_import_capabilities():
    """python_runtime 不得导入 capabilities 内部。"""

    violations = []
    for path in _iter_production_python(_PYTHON_RUNTIME):
        for module_name in _imported_modules(path):
            if module_name == "capabilities" or module_name.startswith("capabilities."):
                violations.append(_repo_relative(path))
    assert violations == []


def test_capabilities_production_does_not_import_python_runtime():
    """capabilities 生产代码不得导入 python_runtime 内部。"""

    violations = []
    for path in _iter_production_python(_CAPABILITIES):
        for module_name in _imported_modules(path):
            if module_name == "packages.python_runtime" or module_name.startswith(
                "packages.python_runtime."
            ):
                violations.append(_repo_relative(path))
    assert violations == []


def test_python_runtime_has_no_copied_contract_schemas():
    """不得在 Runtime 包内复制 contracts/v1 schema 文件。"""

    copied = [
        _repo_relative(path)
        for path in _PYTHON_RUNTIME.rglob("*.schema.json")
        if "tests" not in path.relative_to(_PYTHON_RUNTIME).parts
    ]
    assert copied == []


def test_model_runtime_consumption_uses_public_modules_only():
    """生产代码只允许经已审查的 model_runtime 公开模块消费。"""

    violations = []
    for path in _iter_production_python(_PYTHON_RUNTIME):
        for module_name in _imported_modules(path):
            if not module_name.startswith("packages.model_runtime"):
                continue
            allowed = any(
                module_name == prefix or module_name.startswith(f"{prefix}.")
                for prefix in _ALLOWED_MODEL_RUNTIME_PREFIXES
            )
            # 顶层 packages.model_runtime 及其公开 gateway 子模块允许
            if module_name == "packages.model_runtime":
                allowed = True
            if module_name.startswith("packages.model_runtime.gateway"):
                allowed = True
            if module_name.startswith("packages.model_runtime.resources"):
                allowed = False
            if module_name.startswith("packages.model_runtime.architecture"):
                allowed = False
            if module_name.startswith("packages.model_runtime.tests"):
                allowed = False
            if not allowed:
                violations.append(f"{_repo_relative(path)}:{module_name}")
    assert violations == []


def test_no_state_committer_implementation():
    """本批不得出现 State Committer 实现或导入。"""

    violations = []
    for path in _iter_production_python(_PYTHON_RUNTIME):
        text = path.read_text(encoding="utf-8")
        if "state_committer" in path.name.lower():
            violations.append(_repo_relative(path))
        for module_name in _imported_modules(path):
            if "state_committer" in module_name:
                violations.append(_repo_relative(path))
        if "class StateCommitter" in text:
            violations.append(_repo_relative(path))
    assert violations == []


def test_no_legacy_service_imports_python_runtime():
    """十个遗留 FastAPI 服务不得导入 Runtime 内部。"""

    violations = []
    for service_dir in sorted(_REPO_ROOT.glob("*-service")):
        for path in _iter_production_python(service_dir):
            for module_name in _imported_modules(path):
                if module_name == "packages.python_runtime" or module_name.startswith(
                    "packages.python_runtime."
                ):
                    violations.append(_repo_relative(path))
    assert violations == []


def test_python_runtime_contains_no_java_sources():
    """Runtime 包内不得混入 Java 生产源。"""

    java_files = [_repo_relative(path) for path in _PYTHON_RUNTIME.rglob("*.java")]
    assert java_files == []
