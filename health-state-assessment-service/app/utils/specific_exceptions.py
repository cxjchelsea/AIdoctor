"""
健康状态判定服务特定异常
错误码范围：1000-1099
"""
from app.utils.exceptions import BusinessException


class EntryAssessmentException(BusinessException):
    """入口判定异常"""
    def __init__(self, message: str = "入口判定失败"):
        super().__init__(1001, message)


class RedFlagCheckException(BusinessException):
    """危险信号检查异常"""
    def __init__(self, message: str = "危险信号检查失败"):
        super().__init__(1002, message)


class WorkModeDeterminationException(BusinessException):
    """工作态判定异常"""
    def __init__(self, message: str = "工作态判定失败"):
        super().__init__(1003, message)


class WellnessPlanGenerationException(BusinessException):
    """健康管理计划生成异常"""
    def __init__(self, message: str = "健康管理计划生成失败"):
        super().__init__(1004, message)


class UserInputEmptyException(BusinessException):
    """用户输入为空异常"""
    def __init__(self, message: str = "用户输入为空"):
        super().__init__(1005, message)


class UserIdEmptyException(BusinessException):
    """用户ID为空异常"""
    def __init__(self, message: str = "用户ID不能为空"):
        super().__init__(1006, message)


class ParameterValidationException(BusinessException):
    """参数验证异常"""
    def __init__(self, message: str = "参数验证失败"):
        super().__init__(1007, message)


class WellnessScreeningA1Exception(BusinessException):
    """A1需求分类异常"""
    def __init__(self, message: str = "A1需求分类失败"):
        super().__init__(1008, message)


class WellnessScreeningA2Exception(BusinessException):
    """A2收集健康画像异常"""
    def __init__(self, message: str = "A2收集健康画像失败"):
        super().__init__(1009, message)


class WellnessScreeningA3Exception(BusinessException):
    """A3执行分支异常"""
    def __init__(self, message: str = "A3执行分支失败"):
        super().__init__(1010, message)


class WellnessScreeningA4Exception(BusinessException):
    """A4生成统一结果异常"""
    def __init__(self, message: str = "A4生成统一结果失败"):
        super().__init__(1011, message)


class WellnessScreeningA5Exception(BusinessException):
    """A5设置随访异常"""
    def __init__(self, message: str = "A5设置随访失败"):
        super().__init__(1012, message)