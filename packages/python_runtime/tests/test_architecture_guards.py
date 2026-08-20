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

# Runtime 默认进程必须保持 OCR / 图像栈隔离
_FORBIDDEN_OCR_RUNTIME_ROOTS = frozenset(
    {
        "cv2",
        "numpy",
        "PIL",
        "Pillow",
        "pytesseract",
        "ocr_service",
        "app.services.raw_ocr",
        "app.services.ocr_service",
    }
)

# 精确已审查模块集合；共享前缀不得自动放行私有子模块（F002）
# packages.model_runtime.gateway.models 为已接受的 F001 债务
_APPROVED_MODEL_RUNTIME_MODULES = frozenset(
    {
        "packages.model_runtime",
        "packages.model_runtime.gateway",
        "packages.model_runtime.gateway.models",
    }
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


def is_approved_model_runtime_import(module_name: str) -> bool:
    """仅允许精确已审查模块，不按 packages.model_runtime.* 前缀自动放行。"""

    return module_name in _APPROVED_MODEL_RUNTIME_MODULES


def test_model_runtime_consumption_uses_exact_approved_modules_only():
    """生产代码只允许精确已审查的 model_runtime 模块；前缀不得自动批准。"""

    violations = []
    for path in _iter_production_python(_PYTHON_RUNTIME):
        for module_name in _imported_modules(path):
            if not module_name.startswith("packages.model_runtime"):
                continue
            if not is_approved_model_runtime_import(module_name):
                violations.append(f"{_repo_relative(path)}:{module_name}")
    assert violations == []


def test_model_runtime_allowlist_rejects_hypothetical_private_gateway_module():
    """假设的 gateway 私有模块不得仅因共享前缀而通过。"""

    assert is_approved_model_runtime_import("packages.model_runtime")
    assert is_approved_model_runtime_import("packages.model_runtime.gateway.models")
    assert not is_approved_model_runtime_import(
        "packages.model_runtime.gateway.some_private_module"
    )
    assert not is_approved_model_runtime_import("packages.model_runtime.resources.catalog")


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


def test_python_runtime_does_not_import_ocr_image_stack_or_legacy_ocr_service():
    """规范 Runtime 不得导入 ocr-service、OpenCV、NumPy、Pillow 或 pytesseract。"""

    violations = []
    for path in _iter_production_python(_PYTHON_RUNTIME):
        for module_name in _imported_modules(path):
            for forbidden in _FORBIDDEN_OCR_RUNTIME_ROOTS:
                if module_name == forbidden or module_name.startswith(f"{forbidden}."):
                    violations.append(f"{_repo_relative(path)}:{module_name}")
    assert violations == []


# POSTFREEZE-03B-F：唯一允许出现 engineering.ocr.raw 字面量的工程组合模块。
_ENGINEERING_RAW_OCR_COMPOSITION_MODULE = (
    Path("packages") / "python_runtime" / "http" / "engineering_raw_ocr.py"
)


def test_python_runtime_does_not_register_raw_ocr_capability():
    """
    默认 Runtime 生产树不得泄漏 engineering.ocr.raw。

    唯一窄例外：显式非默认工程组合模块
    packages/python_runtime/http/engineering_raw_ocr.py
    """

    allowed_relative = _ENGINEERING_RAW_OCR_COMPOSITION_MODULE.as_posix()
    violations = []
    for path in _iter_production_python(_PYTHON_RUNTIME):
        relative = _repo_relative(path)
        if relative == allowed_relative:
            continue
        text = path.read_text(encoding="utf-8")
        if "engineering.ocr.raw" in text:
            violations.append(relative)
    assert violations == []

    allowed_path = _REPO_ROOT / _ENGINEERING_RAW_OCR_COMPOSITION_MODULE
    assert allowed_path.is_file()
    assert "engineering.ocr.raw" in allowed_path.read_text(encoding="utf-8")


def test_raw_ocr_adapter_remains_injection_only_and_ocr_library_free():
    """新增适配器必须保持注入式、无 OCR 库、无遗留服务导入、无生产能力标识。"""

    adapter_path = _PYTHON_RUNTIME / "raw_ocr_adapter.py"
    assert adapter_path.is_file()
    adapter_text = adapter_path.read_text(encoding="utf-8")
    assert "engineering.ocr.raw" not in adapter_text
    imported_modules = _imported_modules(adapter_path)
    for forbidden in _FORBIDDEN_OCR_RUNTIME_ROOTS:
        for module_name in imported_modules:
            assert module_name != forbidden
            assert not module_name.startswith(f"{forbidden}.")
    for module_name in imported_modules:
        assert not module_name.endswith("_service")
        assert "ocr-service" not in module_name
        assert module_name != "app" and not module_name.startswith("app.")
