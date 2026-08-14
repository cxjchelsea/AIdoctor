# AI医生系统 LLM 公共库（LEGACY / DISABLED）

## 状态

**LEGACY · DISABLED · NO PROVIDER TRAFFIC · DO NOT USE FOR NEW CODE**

本包在 A7-NC-P5（NC-10）下仅为机械 import 兼容 facade：

- `FAIL_CLOSED_NO_ENABLE_PATH`：不存在受支持的 runtime enable 开关
- legacy provider traffic = 0
- 不得读取 provider API key / 建立网络 / 调用 provider SDK
- 不得执行或迁移历史临床 Prompt
- 新业务必须走 ModelGateway → ProviderAdapter（当前仅 Fake 路径已授权）

## 兼容面

以下符号仍可 import：

- `LangChainLLMClient`
- `PromptTemplateManager`
- `LLMConfig` / `LLMBackend`
- `LLMException` / `LLMTimeoutException` / `LLMAPIException` / `LLMConfigException`
- `LegacyLLMDisabledError`

构造 `LangChainLLMClient(...)` 或调用 `generate` / 模板 `format_*` 将确定性抛出
`LegacyLLMDisabledError`（detail：`legacy provider runtime disabled`）。

## 禁止

- 设置 `OPENAI_API_KEY` 等期望恢复旧 provider 行为
- `AIDOCTOR_LEGACY_LLM_ENABLED` / `ENABLE_LEGACY_PROVIDER` 等 enable 开关（不受支持）
- 将本包接入真实 OpenAI / Ollama / HTTP provider
- 将历史临床模板迁入 Prompt Registry / Capability / SUP-02

## 安装

若服务仍通过 editable 引用本包，仅会获得 disabled facade，不会安装 provider SDK：

```txt
-e ../common/aidoctor_llm
```
