"""
诊断引擎服务具体异常类
按照《AI医生系统-错误处理规范.md》定义
tool_3：鉴别诊断引擎错误码（3000-3999）
"""
from app.utils.exceptions import BusinessException


class DiagnosisEngineUnavailableException(BusinessException):
    """诊断引擎服务不可用异常"""
    def __init__(self, service_name: str):
        super().__init__(3001, f"诊断引擎服务不可用: {service_name}")


class DiagnosisEngineTimeoutException(BusinessException):
    """诊断引擎调用超时异常"""
    def __init__(self, service_name: str, timeout: int):
        super().__init__(3002, f"诊断引擎调用超时: {service_name}, 超时时间={timeout}ms")


class RuleEngineFailedException(BusinessException):
    """规则引擎执行失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(3003, f"规则引擎执行失败: {reason}")


class KnowledgeGraphFailedException(BusinessException):
    """知识图谱查询失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(3004, f"知识图谱查询失败: {reason}")


class KgPathRetrievalFailedException(BusinessException):
    """知识图谱路径检索失败异常（DR.KNOWS核心）"""
    def __init__(self, reason: str = ""):
        super().__init__(3005, f"知识图谱路径检索失败: {reason}")


class PathScoringSortingFailedException(BusinessException):
    """路径评分排序失败异常（DR.KNOWS核心）"""
    def __init__(self, reason: str = ""):
        super().__init__(3006, f"路径评分排序失败: {reason}")


class PathInjectionLlmFailedException(BusinessException):
    """路径注入LLM失败异常（DR.KNOWS核心）"""
    def __init__(self, reason: str = ""):
        super().__init__(3007, f"路径注入LLM失败: {reason}")


class StatisticalModelFailedException(BusinessException):
    """统计模型推理失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(3008, f"统计模型推理失败: {reason}")


class LlmCallFailedException(BusinessException):
    """大模型调用失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(3009, f"大模型调用失败: {reason}")


class DifferentialEngineFailedException(BusinessException):
    """鉴别诊断引擎失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(3010, f"鉴别诊断引擎失败: {reason}")


class MultiEngineFusionFailedException(BusinessException):
    """多引擎融合失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(3011, f"多引擎融合失败: {reason}")


class ReasoningGroupOrganizationFailedException(BusinessException):
    """推理子组组织失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(3012, f"推理子组组织失败: {reason}")


class DiversionPathDesignFailedException(BusinessException):
    """分流路径设计失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(3013, f"分流路径设计失败: {reason}")


class EvidenceAnalysisFailedException(BusinessException):
    """证据分析失败异常"""
    def __init__(self, reason: str = ""):
        super().__init__(3014, f"证据分析失败: {reason}")

