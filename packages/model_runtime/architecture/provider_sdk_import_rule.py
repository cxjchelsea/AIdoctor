"""NC-12 / EG-13：禁止 business code 直接 import provider SDK。

本模块是仓库架构守卫，不是 ProviderAdapter 运行时行为。
扫描根、禁止模块集、规则 ID 均为模块内不可变常量；
不得通过环境变量或调用方任意 allowlist 绕过。

动态 import 策略（有界）：
仅检测常量字符串形式的 importlib.import_module / from-import 别名 / __import__。
不做通用名字解析、字符串拼接或反射分析。
"""

from __future__ import annotations

import ast
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

# 永久规则标识（诊断输出稳定）
RULE_ID = "P5_PROVIDER_SDK_DIRECT_IMPORT"
PARSE_FAILURE_RULE_ID = "P5_PROVIDER_SDK_ARCHITECTURE_PARSE_FAILURE"
PARSE_FAILURE_CATEGORY = "UNPARSEABLE_BUSINESS_PYTHON"
PARSE_FAILURE_MESSAGE = "unparseable business Python file"

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


class ProviderSdkArchitectureScanError(Exception):
    """仓库架构扫描 fail-closed：不可解析的 included business Python。"""

    def __init__(
        self,
        *,
        path: str,
        line: int,
        category: str = PARSE_FAILURE_CATEGORY,
        rule_id: str = PARSE_FAILURE_RULE_ID,
        message: str = PARSE_FAILURE_MESSAGE,
    ) -> None:
        self.path = path
        self.line = line
        self.category = category
        self.rule_id = rule_id
        self.message = message
        super().__init__(message)


def _module_is_forbidden(module_name: str) -> bool:
    """判断模块名是否命中禁止 provider SDK 集合。"""
    if not module_name:
        return False
    if module_name in FORBIDDEN_PROVIDER_SDK_ROOTS:
        return True
    root = module_name.split(".")[0]
    if root in FORBIDDEN_PROVIDER_SDK_ROOTS:
        return True
    for forbidden in FORBIDDEN_PROVIDER_SDK_ROOTS:
        if module_name == forbidden or module_name.startswith(forbidden + "."):
            return True
    return False


def _canonical_forbidden_module(module_name: str) -> str:
    """返回稳定的禁止模块诊断标识。"""
    if module_name in FORBIDDEN_PROVIDER_SDK_ROOTS:
        return module_name
    matches = [
        forbidden
        for forbidden in FORBIDDEN_PROVIDER_SDK_ROOTS
        if module_name == forbidden or module_name.startswith(forbidden + ".")
    ]
    if matches:
        # 选择最长匹配，优先完整 provider 标识（如 google.genai）
        return max(matches, key=len)
    root = module_name.split(".")[0]
    if root in FORBIDDEN_PROVIDER_SDK_ROOTS:
        return root
    return module_name


def _match_forbidden_module(module_name: str) -> str | None:
    if _module_is_forbidden(module_name):
        return _canonical_forbidden_module(module_name)
    return None


def _match_absolute_import_from(module_name: str, imported_names: Iterable[str]) -> str | None:
    """绝对 ImportFrom：检查 module 与 parent+alias 组合。"""
    direct = _match_forbidden_module(module_name)
    if direct is not None:
        return direct
    for alias_name in imported_names:
        candidate = f"{module_name}.{alias_name}"
        matched = _match_forbidden_module(candidate)
        if matched is not None:
            return matched
    return None


def _langchain_from_import_forbidden(module_name: str, imported_names: Iterable[str]) -> bool:
    """仅标记 provider-bound 的 LangChain 导入，不禁止全部 langchain。"""
    if module_name in _PROVIDER_BOUND_LANGCHAIN_MODULES or any(
        module_name.startswith(prefix + ".") for prefix in _PROVIDER_BOUND_LANGCHAIN_MODULES
    ):
        return any(name in _PROVIDER_BOUND_LANGCHAIN_NAMES for name in imported_names)
    if module_name in {"langchain_community", "langchain"}:
        return any(name in _PROVIDER_BOUND_LANGCHAIN_NAMES for name in imported_names)
    return False


def _constant_str(node: ast.AST) -> str | None:
    if isinstance(node, ast.Constant) and isinstance(node.value, str):
        return node.value
    return None


def _collect_importlib_aliases(tree: ast.AST) -> tuple[set[str], set[str]]:
    """收集 importlib 模块别名与 import_module 直接别名（有界，非完整名字解析）。"""
    importlib_module_aliases: set[str] = set()
    import_module_aliases: set[str] = set()
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            for alias in node.names:
                if alias.name == "importlib":
                    importlib_module_aliases.add(alias.asname or "importlib")
        elif (
            isinstance(node, ast.ImportFrom)
            and node.level == 0
            and node.module == "importlib"
        ):
            for alias in node.names:
                if alias.name == "import_module":
                    import_module_aliases.add(alias.asname or "import_module")
    return importlib_module_aliases, import_module_aliases


def scan_provider_sdk_imports(path: Path) -> list[ForbiddenProviderSdkImport]:
    """对单个 Python 文件做 AST 扫描，返回确定性排序的违规列表。"""
    source = path.read_text(encoding="utf-8")
    tree = ast.parse(source, filename=str(path))
    found: list[ForbiddenProviderSdkImport] = []
    display_path = path.as_posix()
    importlib_module_aliases, import_module_aliases = _collect_importlib_aliases(tree)

    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            for alias in node.names:
                matched = _match_forbidden_module(alias.name)
                if matched is not None:
                    found.append(
                        ForbiddenProviderSdkImport(
                            path=display_path,
                            line=node.lineno,
                            module=matched,
                        )
                    )
        elif isinstance(node, ast.ImportFrom) and node.module:
            # 相对导入指向本地包解析，不按 top-level provider SDK 判定
            if node.level and node.level > 0:
                continue
            imported_names = [alias.name for alias in node.names]
            matched = _match_absolute_import_from(node.module, imported_names)
            if matched is None and _langchain_from_import_forbidden(
                node.module, imported_names
            ):
                # provider-bound LangChain：诊断使用模块根/路径
                if node.module.startswith("langchain_openai"):
                    matched = "langchain_openai"
                elif node.module.startswith("langchain_community"):
                    matched = "langchain_community"
                else:
                    matched = "langchain"
            if matched is not None:
                found.append(
                    ForbiddenProviderSdkImport(
                        path=display_path,
                        line=node.lineno,
                        module=matched,
                    )
                )
        elif isinstance(node, ast.Call):
            func = node.func
            constant = _constant_str(node.args[0]) if node.args else None
            if constant is None or not _module_is_forbidden(constant):
                continue
            module_label = _canonical_forbidden_module(constant)

            # __import__("openai")
            if isinstance(func, ast.Name) and func.id == "__import__":
                found.append(
                    ForbiddenProviderSdkImport(
                        path=display_path,
                        line=node.lineno,
                        module=module_label,
                    )
                )
                continue

            # from importlib import import_module / as load
            if isinstance(func, ast.Name) and func.id in import_module_aliases:
                found.append(
                    ForbiddenProviderSdkImport(
                        path=display_path,
                        line=node.lineno,
                        module=module_label,
                    )
                )
                continue

            # importlib.import_module / il.import_module
            if (
                isinstance(func, ast.Attribute)
                and func.attr == "import_module"
                and isinstance(func.value, ast.Name)
                and func.value.id in importlib_module_aliases
            ):
                found.append(
                    ForbiddenProviderSdkImport(
                        path=display_path,
                        line=node.lineno,
                        module=module_label,
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
            if path.name.startswith("test_") or path.name.endswith("_test.py"):
                continue
            files.append(path)
    return files


def scan_repository_business_code(repo_root: Path) -> list[ForbiddenProviderSdkImport]:
    """扫描仓库 business code；豁免列表为空。

    included 文件若 SyntaxError：raise ProviderSdkArchitectureScanError（fail-closed）。
    """
    violations: list[ForbiddenProviderSdkImport] = []
    for path in iter_business_python_files(repo_root):
        relative = path.relative_to(repo_root).as_posix()
        try:
            file_violations = scan_provider_sdk_imports(path)
        except SyntaxError as exc:
            raise ProviderSdkArchitectureScanError(
                path=relative,
                line=int(exc.lineno or 0),
                category=PARSE_FAILURE_CATEGORY,
                rule_id=PARSE_FAILURE_RULE_ID,
                message=PARSE_FAILURE_MESSAGE,
            ) from None
        for item in file_violations:
            violations.append(
                ForbiddenProviderSdkImport(
                    path=relative,
                    line=item.line,
                    module=item.module,
                    rule_id=item.rule_id,
                )
            )
    return sorted(violations)
