"""NC-12 / EG-13：禁止 business code 直接 import provider SDK。

本模块是仓库架构守卫，不是 ProviderAdapter 运行时行为。
扫描根、禁止模块集、规则 ID 均为模块内不可变常量；
不得通过环境变量或调用方任意 allowlist 绕过。
"""

from __future__ import annotations

import ast
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

# 永久规则标识（诊断输出稳定）
RULE_ID = "P5_PROVIDER_SDK_DIRECT_IMPORT"

# 禁止的 provider SDK / provider-bound 包根（不可变权威）
FORBIDDEN_PROVIDER_SDK_ROOTS: frozenset[str] = frozenset(
    {
        "openai",
        "anthropic",
        "langchain_openai",
        # 常见未来 provider SDK 标识（仅作禁止 import 名，不实现适配器）
        "google.generativeai",
        "google.genai",
        "vertexai",
        "azure.ai.openai",
        "azure.ai.inference",
        "ollama",  # 官方 ollama Python 包直连
    }
)

# LangChain 中明确绑定具体 provider 的模块前缀
_PROVIDER_BOUND_LANGCHAIN_MODULES: frozenset[str] = frozenset(
    {
        "langchain_community.llms",
        "langchain_community.chat_models",
        "langchain.chat_models",
        "langchain.llms",
    }
)

_PROVIDER_BOUND_LANGCHAIN_NAMES: frozenset[str] = frozenset(
    {
        "ChatOpenAI",
        "AzureChatOpenAI",
        "Ollama",
        "ChatOllama",
        "ChatAnthropic",
        "ChatGoogleGenerativeAI",
        "AzureOpenAI",
        "OpenAI",
    }
)

# 固定 business-code 根（相对仓库根）；另含顶层 *-service 目录
_FIXED_BUSINESS_ROOTS: tuple[str, ...] = (
    "common",
    "packages",
    "capabilities",
)

# 明确排除的目录名（不得用宽泛 common/** 豁免）
_EXCLUDED_DIR_NAMES: frozenset[str] = frozenset(
    {
        ".git",
        "__pycache__",
        ".venv",
        "venv",
        "node_modules",
        "tests",
        "test",
        "docs",
        "DRKnows-main",
        "frontend",
        "frontend-admin",
        "science",
        "scripts",
        "monitoring",
        ".github",
    }
)


@dataclass(frozen=True, order=True)
class ForbiddenProviderSdkImport:
    """确定性违规诊断。"""

    path: str
    line: int
    module: str
    rule_id: str = RULE_ID


def _module_is_forbidden(module_name: str) -> bool:
    """判断模块名是否命中禁止 provider SDK 集合。"""
    if module_name in FORBIDDEN_PROVIDER_SDK_ROOTS:
        return True
    root = module_name.split(".")[0]
    if root in FORBIDDEN_PROVIDER_SDK_ROOTS:
        return True
    for forbidden in FORBIDDEN_PROVIDER_SDK_ROOTS:
        if module_name == forbidden or module_name.startswith(forbidden + "."):
            return True
    return False


def _langchain_from_import_forbidden(module_name: str, imported_names: Iterable[str]) -> bool:
    """仅标记 provider-bound 的 LangChain 导入，不禁止全部 langchain。"""
    if module_name in _PROVIDER_BOUND_LANGCHAIN_MODULES or any(
        module_name.startswith(prefix + ".") for prefix in _PROVIDER_BOUND_LANGCHAIN_MODULES
    ):
        return any(name in _PROVIDER_BOUND_LANGCHAIN_NAMES for name in imported_names)
    # from langchain_community import ChatOpenAI 这类
    if module_name in {"langchain_community", "langchain"}:
        return any(name in _PROVIDER_BOUND_LANGCHAIN_NAMES for name in imported_names)
    return False


def _constant_str(node: ast.AST) -> str | None:
    if isinstance(node, ast.Constant) and isinstance(node.value, str):
        return node.value
    return None


def scan_provider_sdk_imports(path: Path) -> list[ForbiddenProviderSdkImport]:
    """对单个 Python 文件做 AST 扫描，返回确定性排序的违规列表。"""
    source = path.read_text(encoding="utf-8")
    tree = ast.parse(source, filename=str(path))
    found: list[ForbiddenProviderSdkImport] = []
    display_path = path.as_posix()

    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            for alias in node.names:
                if _module_is_forbidden(alias.name):
                    found.append(
                        ForbiddenProviderSdkImport(
                            path=display_path,
                            line=node.lineno,
                            module=alias.name.split(".")[0]
                            if alias.name.split(".")[0] in FORBIDDEN_PROVIDER_SDK_ROOTS
                            else alias.name,
                        )
                    )
        elif isinstance(node, ast.ImportFrom) and node.module:
            imported_names = [alias.name for alias in node.names]
            if _module_is_forbidden(node.module) or _langchain_from_import_forbidden(
                node.module, imported_names
            ):
                root = node.module.split(".")[0]
                module_label = (
                    node.module
                    if node.module in FORBIDDEN_PROVIDER_SDK_ROOTS
                    or any(
                        node.module == item or node.module.startswith(item + ".")
                        for item in FORBIDDEN_PROVIDER_SDK_ROOTS
                    )
                    else root
                )
                # 对 langchain_openai 等保持完整根名
                if root == "langchain_openai":
                    module_label = "langchain_openai"
                elif root in FORBIDDEN_PROVIDER_SDK_ROOTS:
                    module_label = root
                found.append(
                    ForbiddenProviderSdkImport(
                        path=display_path,
                        line=node.lineno,
                        module=module_label,
                    )
                )
        elif isinstance(node, ast.Call):
            func = node.func
            # importlib.import_module("openai")
            if isinstance(func, ast.Attribute) and func.attr == "import_module":
                if node.args:
                    constant = _constant_str(node.args[0])
                    if constant and _module_is_forbidden(constant):
                        found.append(
                            ForbiddenProviderSdkImport(
                                path=display_path,
                                line=node.lineno,
                                module=constant.split(".")[0],
                            )
                        )
            # __import__("openai")
            if isinstance(func, ast.Name) and func.id == "__import__":
                if node.args:
                    constant = _constant_str(node.args[0])
                    if constant and _module_is_forbidden(constant):
                        found.append(
                            ForbiddenProviderSdkImport(
                                path=display_path,
                                line=node.lineno,
                                module=constant.split(".")[0],
                            )
                        )

    return sorted(found)


def iter_business_python_files(repo_root: Path) -> list[Path]:
    """枚举固定 business-code 根下的生产 Python 文件。"""
    roots: list[Path] = []
    for name in _FIXED_BUSINESS_ROOTS:
        candidate = repo_root / name
        if candidate.is_dir():
            roots.append(candidate)
    for child in sorted(repo_root.iterdir()):
        if child.is_dir() and child.name.endswith("-service"):
            roots.append(child)

    files: list[Path] = []
    for root in roots:
        for path in sorted(root.rglob("*.py")):
            if any(part in _EXCLUDED_DIR_NAMES for part in path.parts):
                continue
            # 测试文件名排除（目录已排除 tests/；再排除散落 test_*.py）
            if path.name.startswith("test_") or path.name.endswith("_test.py"):
                continue
            files.append(path)
    return files


def scan_repository_business_code(repo_root: Path) -> list[ForbiddenProviderSdkImport]:
    """扫描仓库 business code；豁免列表为空（legacy 已中和后）。"""
    violations: list[ForbiddenProviderSdkImport] = []
    for path in iter_business_python_files(repo_root):
        try:
            file_violations = scan_provider_sdk_imports(path)
        except SyntaxError:
            # 语法错误文件不静默放行：记为扫描失败应由测试暴露；此处跳过不可解析文件
            continue
        # 路径改为相对仓库根，保证跨机器诊断稳定
        for item in file_violations:
            relative = path.relative_to(repo_root).as_posix()
            violations.append(
                ForbiddenProviderSdkImport(
                    path=relative,
                    line=item.line,
                    module=item.module,
                    rule_id=item.rule_id,
                )
            )
    return sorted(violations)
