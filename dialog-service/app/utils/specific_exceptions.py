"""
主动问诊服务具体异常类
按照《AI医生系统-错误处理规范.md》定义
tool_2：主动问诊服务错误码（1200-1299）
"""
from app.utils.exceptions import BusinessException


class InformationGapIdentificationFailedException(BusinessException):
    """信息缺口识别失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(1201, f"信息缺口识别失败: {reason}")


class IntelligentQuestioningGenerationFailedException(BusinessException):
    """智能追问生成失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(1202, f"智能追问生成失败: {reason}")


class NluFailedException(BusinessException):
    """自然语言理解失败异常（NLU）"""
    def __init__(self, reason: str = ""):
        super().__init__(1203, f"自然语言理解失败: {reason}")


class NlgFailedException(BusinessException):
    """自然语言生成失败异常（NLG）"""
    def __init__(self, reason: str = ""):
        super().__init__(1204, f"自然语言生成失败: {reason}")


class DialogContextManagementFailedException(BusinessException):
    """对话上下文管理失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(1205, f"对话上下文管理失败: {reason}")


class QuestioningCountExceededException(BusinessException):
    """追问次数超限异常"""
    def __init__(self, current_count: int, max_count: int):
        super().__init__(1206, f"追问次数超限: 当前={current_count}, 最大={max_count}")

