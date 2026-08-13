"""
Legacy LangChain LLM 客户端兼容 facade（A7-NC-P5 / NC-10）

机械兼容：符号可 import。
执行语义：FAIL_CLOSED_NO_ENABLE_PATH。
禁止：provider SDK、网络、密钥读取、clinical Prompt 执行、静默重定向。
"""

from __future__ import annotations

from typing import Any, Dict, Optional

from .config import LLMConfig
from .exceptions import LegacyLLMDisabledError


def _raise_disabled() -> None:
    """在任何 SDK / env / network / Prompt 路径之前 fail-closed。"""
    raise LegacyLLMDisabledError()


class LangChainLLMClient:
    """Legacy LLM 客户端兼容符号：构造与调用一律禁用。"""

    def __init__(self, config: Optional[LLMConfig] = None) -> None:
        # config 参数仅保留签名兼容；不得读取 env / 创建 SDK / 建立 HTTP
        _ = config
        _raise_disabled()

    async def generate(
        self,
        prompt: str,
        **kwargs: Any,
    ) -> str:
        _ = prompt, kwargs
        _raise_disabled()
        raise AssertionError("unreachable")  # pragma: no cover

    def generate_sync(
        self,
        prompt: str,
        **kwargs: Any,
    ) -> str:
        _ = prompt, kwargs
        _raise_disabled()
        raise AssertionError("unreachable")  # pragma: no cover

    async def _call_chatglm_api(self, *args: Any, **kwargs: Any) -> str:
        _ = args, kwargs
        _raise_disabled()
        raise AssertionError("unreachable")  # pragma: no cover

    async def _call_custom_api(self, *args: Any, **kwargs: Any) -> str:
        _ = args, kwargs
        _raise_disabled()
        raise AssertionError("unreachable")  # pragma: no cover

    def _create_llm(self, *args: Any, **kwargs: Any) -> Any:
        _ = args, kwargs
        _raise_disabled()
        raise AssertionError("unreachable")  # pragma: no cover

    def _load_config_from_env(self) -> LLMConfig:
        _raise_disabled()
        raise AssertionError("unreachable")  # pragma: no cover
