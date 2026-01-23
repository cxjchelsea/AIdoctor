"""
OCR服务具体异常类
按照《AI医生系统-错误处理规范.md》定义
OCR服务错误码（4000-4999）
"""
from app.utils.exceptions import BusinessException


class OcrRecognitionException(BusinessException):
    """OCR识别失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(4001, f"OCR识别失败: {reason}")


class OcrFileParseException(BusinessException):
    """文件解析失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(4002, f"文件解析失败: {reason}")


class UnsupportedImageFormatException(BusinessException):
    """图片格式不支持异常"""
    def __init__(self, format: str):
        super().__init__(4003, f"图片格式不支持: {format}")


class LowImageQualityException(BusinessException):
    """图片质量过低异常"""
    def __init__(self, reason: str = ""):
        super().__init__(4004, f"图片质量过低: {reason}")


class LowOcrConfidenceException(BusinessException):
    """文字识别置信度过低异常"""
    def __init__(self, confidence: float, threshold: float):
        super().__init__(4005, f"文字识别置信度过低: 当前={confidence:.2f}, 阈值={threshold:.2f}")

