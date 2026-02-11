"""
可解释性服务具体异常类
按照《AI医生系统-错误处理规范.md》定义
tool_7：可解释性服务错误码（1600-1699）
"""
from app.utils.exceptions import BusinessException


class EvidenceChainConstructionFailedException(BusinessException):
    """证据链构建失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(1601, f"证据链构建失败: {reason}")


class ReasoningPathVisualizationFailedException(BusinessException):
    """推理路径可视化失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(1602, f"推理路径可视化失败: {reason}")


class NaturalLanguageExplanationFailedException(BusinessException):
    """自然语言解释生成失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(1604, f"自然语言解释生成失败: {reason}")


class ConclusionPackageGenerationFailedException(BusinessException):
    """终点结论包生成失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(1603, f"终点结论包生成失败: {reason}")


class CDPNotFoundException(BusinessException):
    """CDP不存在异常"""
    def __init__(self, cdp_id: str = ""):
        super().__init__(1605, f"CDP不存在: {cdp_id}")


class DiagnosisDataIncompleteException(BusinessException):
    """诊断结果数据不完整异常"""
    def __init__(self, reason: str = ""):
        super().__init__(1606, f"诊断结果数据不完整: {reason}")

