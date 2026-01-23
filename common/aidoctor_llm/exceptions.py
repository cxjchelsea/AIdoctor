"""
LLM相关异常类
"""


class LLMException(Exception):
    """LLM基础异常"""
    def __init__(self, message: str, error_code: str = None):
        self.message = message
        self.error_code = error_code
        super().__init__(self.message)


class LLMTimeoutException(LLMException):
    """LLM超时异常"""
    def __init__(self, message: str = "LLM调用超时"):
        super().__init__(message, "LLM_TIMEOUT")


class LLMAPIException(LLMException):
    """LLM API调用异常"""
    def __init__(self, message: str, status_code: int = None):
        self.status_code = status_code
        super().__init__(message, "LLM_API_ERROR")


class LLMConfigException(LLMException):
    """LLM配置异常"""
    def __init__(self, message: str):
        super().__init__(message, "LLM_CONFIG_ERROR")

