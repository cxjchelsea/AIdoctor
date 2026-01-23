"""
病例理解服务具体异常类
按照《AI医生系统-错误处理规范.md》定义
脑区A：病例理解服务错误码（1100-1199）
"""
from app.utils.exceptions import BusinessException


class MedicalConceptRecognitionFailedException(BusinessException):
    """医学概念识别失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(1101, f"医学概念识别失败: {reason}")


class ConceptNormalizationFailedException(BusinessException):
    """概念归一化失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(1102, f"概念归一化失败: {reason}")


class MultimodalUnderstandingFailedException(BusinessException):
    """多模态理解失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(1103, f"多模态理解失败: {reason}")


class StructuredExtractionFailedException(BusinessException):
    """结构化提取失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(1104, f"结构化提取失败: {reason}")


class AmbiguityDeterminationFailedException(BusinessException):
    """歧义表达判定失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(1105, f"歧义表达判定失败: {reason}")


class OcrRecognitionFailedBrainAException(BusinessException):
    """OCR识别失败异常（脑区A调用）"""
    def __init__(self, reason: str = ""):
        super().__init__(1106, f"OCR识别失败（脑区A调用）: {reason}")

