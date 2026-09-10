"""EG-14 / AC-P5：legacy aidoctor_llm 必须 fail-closed，且无 enable path。"""

from __future__ import annotations

import importlib
import os
import socket
import sys
import urllib.request
from pathlib import Path

import pytest

# 保证可以从 common/ 解析 aidoctor_llm 包
_COMMON_ROOT = Path(__file__).resolve().parents[2]
if str(_COMMON_ROOT) not in sys.path:
    sys.path.insert(0, str(_COMMON_ROOT))


DISABLED_DETAIL_TOKEN = "legacy provider runtime disabled"
PROVIDER_SECRET_KEYS = (
    "OPENAI_API_KEY",
    "CHATGLM_API_KEY",
    "CUSTOM_LLM_API_KEY",
    "OPENAI_BASE_URL",
    "CHATGLM_API_URL",
    "CUSTOM_LLM_API_URL",
    "OLLAMA_BASE_URL",
    "LLM_BACKEND",
)


def _install_network_bombs(monkeypatch) -> None:
    """任一网络入口被触碰即失败。"""

    def boom_network(*_args, **_kwargs):
        raise AssertionError("legacy disabled path must not touch network")

    monkeypatch.setattr(socket, "socket", boom_network)
    monkeypatch.setattr(socket, "create_connection", boom_network)
    monkeypatch.setattr(urllib.request, "urlopen", boom_network)
    for module_name in ("httpx", "requests"):
        try:
            module = importlib.import_module(module_name)
        except ImportError:
            continue
        for attr_name in ("request", "get", "post", "AsyncClient", "Client"):
            if hasattr(module, attr_name):
                monkeypatch.setattr(module, attr_name, boom_network, raising=False)


def _install_env_bombs(monkeypatch) -> list[str]:
    """记录并禁止凭证相关环境读取。"""

    accessed_keys: list[str] = []

    real_getenv = os.getenv

    def tracking_getenv(key, default=None):
        accessed_keys.append(str(key))
        if str(key) in PROVIDER_SECRET_KEYS:
            raise AssertionError(f"disabled path must not read secret env: {key}")
        return real_getenv(key, default)

    class BoomEnviron(dict):
        def __getitem__(self, key):
            accessed_keys.append(str(key))
            if str(key) in PROVIDER_SECRET_KEYS:
                raise AssertionError(f"disabled path must not read secret environ: {key}")
            return dict.__getitem__(self, key)

        def get(self, key, default=None):
            accessed_keys.append(str(key))
            if str(key) in PROVIDER_SECRET_KEYS:
                raise AssertionError(f"disabled path must not read secret environ.get: {key}")
            return default

    monkeypatch.setattr(os, "getenv", tracking_getenv)
    monkeypatch.setattr(os, "environ", BoomEnviron(os.environ.copy()))
    return accessed_keys


def test_package_exports_resolve_without_provider_sdk(monkeypatch) -> None:
    """import 兼容面必须解析，且不得拉起 provider SDK / httpx。"""

    _install_network_bombs(monkeypatch)

    # 清理可能缓存的包后重新导入
    for loaded in list(sys.modules):
        if loaded == "aidoctor_llm" or loaded.startswith("aidoctor_llm."):
            del sys.modules[loaded]

    import aidoctor_llm

    assert aidoctor_llm.LangChainLLMClient is not None
    assert aidoctor_llm.PromptTemplateManager is not None
    assert aidoctor_llm.LLMConfig is not None
    assert aidoctor_llm.LLMBackend is not None
    assert aidoctor_llm.LegacyLLMDisabledError is not None


def test_import_does_not_load_openai_or_langchain_openai() -> None:
    for loaded in list(sys.modules):
        if loaded == "aidoctor_llm" or loaded.startswith("aidoctor_llm."):
            del sys.modules[loaded]

    # 预先放入哨兵，真实 import 会覆盖；若从未 import，哨兵仍在
    sentinel_openai = object()
    sentinel_lc = object()
    sys.modules.setdefault("openai", sentinel_openai)  # type: ignore[assignment]
    sys.modules.setdefault("langchain_openai", sentinel_lc)  # type: ignore[assignment]
    before_openai = sys.modules.get("openai")
    before_lc = sys.modules.get("langchain_openai")

    import aidoctor_llm  # noqa: F401

    assert sys.modules.get("openai") is before_openai
    assert sys.modules.get("langchain_openai") is before_lc
    # 哨兵未被真实 SDK 模块替换
    assert sys.modules.get("openai") is sentinel_openai
    assert sys.modules.get("langchain_openai") is sentinel_lc


def test_langchain_client_construct_fail_closed_before_env_and_network(monkeypatch) -> None:
    from aidoctor_llm import LangChainLLMClient, LegacyLLMDisabledError

    accessed = _install_env_bombs(monkeypatch)
    _install_network_bombs(monkeypatch)

    with pytest.raises(LegacyLLMDisabledError) as raised:
        LangChainLLMClient()

    message = str(raised.value)
    assert DISABLED_DETAIL_TOKEN in message.lower() or DISABLED_DETAIL_TOKEN in getattr(
        raised.value, "message", ""
    ).lower()
    assert not any(key in PROVIDER_SECRET_KEYS for key in accessed)
    for secret_fragment in ("sk-", "api_key", "OPENAI_API_KEY"):
        assert secret_fragment not in message


def test_langchain_client_construct_with_config_still_disabled(monkeypatch) -> None:
    from aidoctor_llm import LLMBackend, LLMConfig, LangChainLLMClient, LegacyLLMDisabledError

    _install_env_bombs(monkeypatch)
    _install_network_bombs(monkeypatch)

    config = LLMConfig(backend=LLMBackend.OPENAI, model="gpt-4")
    with pytest.raises(LegacyLLMDisabledError):
        LangChainLLMClient(config=config)


def test_generate_paths_fail_closed_without_network(monkeypatch) -> None:
    from aidoctor_llm import LangChainLLMClient, LegacyLLMDisabledError

    _install_network_bombs(monkeypatch)
    _install_env_bombs(monkeypatch)

    client = object.__new__(LangChainLLMClient)
    # 使用 coroutine.send 驱动 async 体，避免 asyncio 事件循环自身的 socketpair
    coroutine = client.generate("hello")
    with pytest.raises(LegacyLLMDisabledError):
        try:
            coroutine.send(None)
        finally:
            coroutine.close()
    with pytest.raises(LegacyLLMDisabledError):
        client.generate_sync("hello")


def test_prompt_manager_does_not_load_clinical_templates_on_init(monkeypatch) -> None:
    from aidoctor_llm import PromptTemplateManager

    # 禁止 jinja / yaml / 模板目录副作用在 init 路径被依赖为“可用”
    def boom_open(*_args, **_kwargs):
        raise AssertionError("prompt manager init must not open template files")

    monkeypatch.setattr("builtins.open", boom_open)
    manager = PromptTemplateManager()
    assert getattr(manager, "_builtin_templates", {}) in ({}, None) or not manager._builtin_templates


def test_prompt_manager_methods_fail_closed(monkeypatch) -> None:
    from aidoctor_llm import LegacyLLMDisabledError, PromptTemplateManager

    _install_network_bombs(monkeypatch)
    manager = PromptTemplateManager()

    with pytest.raises(LegacyLLMDisabledError):
        manager.get_template("diagnosis_reasoning")
    with pytest.raises(LegacyLLMDisabledError):
        manager.format_diagnosis_reasoning(symptoms=["x"], signs={}, context={})
    with pytest.raises(LegacyLLMDisabledError):
        manager.format_question_generation(missing_info="x", context={})
    with pytest.raises(LegacyLLMDisabledError):
        manager.format_explanation_generation(diagnosis={}, evidence=[], reasoning_path="")
    with pytest.raises(LegacyLLMDisabledError):
        manager.format("diagnosis_reasoning")


def test_no_enable_path_env_switch(monkeypatch) -> None:
    """FAIL_CLOSED_NO_ENABLE_PATH：任何 enable 环境变量不得重新打开 runtime。"""
    from aidoctor_llm import LangChainLLMClient, LegacyLLMDisabledError

    for key, value in (
        ("AIDOCTOR_LEGACY_LLM_ENABLED", "1"),
        ("ENABLE_LEGACY_PROVIDER", "true"),
        ("LLM_BACKEND", "openai"),
        ("OPENAI_API_KEY", "should-not-be-read"),
    ):
        monkeypatch.setenv(key, value)

    _install_network_bombs(monkeypatch)
    with pytest.raises(LegacyLLMDisabledError):
        LangChainLLMClient()


def test_submodule_imports_are_also_disabled(monkeypatch) -> None:
    from aidoctor_llm.llm_client import LangChainLLMClient
    from aidoctor_llm.prompt_manager import PromptTemplateManager
    from aidoctor_llm.exceptions import LegacyLLMDisabledError

    _install_network_bombs(monkeypatch)
    with pytest.raises(LegacyLLMDisabledError):
        LangChainLLMClient()
    manager = PromptTemplateManager()
    with pytest.raises(LegacyLLMDisabledError):
        manager.get_template("diagnosis_reasoning")


def test_requirements_and_setup_have_no_provider_sdk_deps() -> None:
    package_root = Path(__file__).resolve().parents[1]
    requirements_text = (package_root / "requirements.txt").read_text(encoding="utf-8").lower()
    setup_text = (package_root / "setup.py").read_text(encoding="utf-8").lower()
    forbidden = ("openai", "langchain", "httpx", "anthropic")
    for token in forbidden:
        assert token not in requirements_text, token
        assert token not in setup_text, token
