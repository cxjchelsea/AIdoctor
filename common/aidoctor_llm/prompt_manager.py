"""
Legacy Prompt 模板管理器兼容 facade（A7-NC-P5 / NC-10）

机械兼容：符号可 import / 可构造。
执行语义：一切模板加载与渲染 fail-closed。
禁止：迁移临床 Prompt、执行诊断/问诊/解释模板、静默重定向到 Prompt Registry。
"""

from __future__ import annotations

from typing import Any, Dict, Optional

from .exceptions import LegacyLLMDisabledError


def _raise_disabled() -> None:
    raise LegacyLLMDisabledError()


class PromptTemplateManager:
    """Legacy PromptTemplateManager 兼容符号：不加载、不渲染临床模板。"""

    def __init__(self, templates_dir: Optional[str] = None) -> None:
        # 接受参数以保持构造签名兼容，但不创建目录、不读文件、不加载内置临床模板
        _ = templates_dir
        self.templates_dir = None
        self.env = None
        self.templates_config: Dict[str, Any] = {}
        self._builtin_templates: Dict[str, str] = {}

    def get_template(self, template_name: str) -> Any:
        _ = template_name
        _raise_disabled()
        raise AssertionError("unreachable")  # pragma: no cover

    def format(self, template_name: str, **kwargs: Any) -> str:
        _ = template_name, kwargs
        _raise_disabled()
        raise AssertionError("unreachable")  # pragma: no cover

    def format_diagnosis_reasoning(self, *args: Any, **kwargs: Any) -> str:
        _ = args, kwargs
        _raise_disabled()
        raise AssertionError("unreachable")  # pragma: no cover

    def format_question_generation(self, *args: Any, **kwargs: Any) -> str:
        _ = args, kwargs
        _raise_disabled()
        raise AssertionError("unreachable")  # pragma: no cover

    def format_explanation_generation(self, *args: Any, **kwargs: Any) -> str:
        _ = args, kwargs
        _raise_disabled()
        raise AssertionError("unreachable")  # pragma: no cover

    def _load_templates_config(self) -> Dict[str, Any]:
        _raise_disabled()
        raise AssertionError("unreachable")  # pragma: no cover

    def _get_builtin_templates(self) -> Dict[str, str]:
        _raise_disabled()
        raise AssertionError("unreachable")  # pragma: no cover
