"""
LLM 相关异常类（legacy compatibility）

A7-NC-P5：新增 LegacyLLMDisabledError，作为 disabled facade 的确定性错误权威。
"""


class LLMException(Exception):
    """LLM 基础异常"""

    def __init__(self, message: str, error_code: str = None):
        self.message = message
        self.error_code = error_code
        super().__init__(self.message)


class LLMTimeoutException(LLMException):
    """LLM 超时异常（兼容符号保留；legacy runtime 已禁用）"""

    def __init__(self, message: str = "LLM调用超时"):
        super().__init__(message, "LLM_TIMEOUT")


class LLMAPIException(LLMException):
    """LLM API 调用异常（兼容符号保留；legacy runtime 已禁用）"""

    def __init__(self, message: str, status_code: int = None):
        self.status_code = status_code
        super().__init__(message, "LLM_API_ERROR")


class LLMConfigException(LLMException):
    """LLM 配置异常（兼容符号保留；legacy runtime 已禁用）"""

    def __init__(self, message: str):
        super().__init__(message, "LLM_CONFIG_ERROR")


class LegacyLLMDisabledError(LLMException):
    """legacy provider runtime 已禁用（FAIL_CLOSED_NO_ENABLE_PATH）。

    固定、有界、不泄漏密钥 / Prompt / 路径 / provider 响应。
    """

    _FIXED_MESSAGE = "legacy provider runtime disabled"
    _FIXED_CODE = "LEGACY_LLM_DISABLED"

    def __init__(self, message: str | None = None):
        # 忽略调用方自定义文案，防止误带入密钥或临床内容
        super().__init__(self._FIXED_MESSAGE, self._FIXED_CODE)
