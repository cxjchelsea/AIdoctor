"""
引擎基类
定义所有诊断引擎的通用接口
"""

from abc import ABC, abstractmethod
from typing import Dict, Any, List


class BaseEngine(ABC):
    """诊断引擎基类"""
    
    @abstractmethod
    def diagnose(
        self,
        symptoms: List[str],
        signs: Dict[str, Any],
        context: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        执行诊断
        
        Args:
            symptoms: 症状列表
            signs: 体征信息
            context: 上下文信息
            
        Returns:
            诊断结果，包含候选疾病列表和置信度
        """
        pass
    
    @abstractmethod
    def get_engine_name(self) -> str:
        """
        获取引擎名称
        
        Returns:
            引擎名称
        """
        pass
    
    def validate_input(self, symptoms: List[str]) -> bool:
        """
        验证输入
        
        Args:
            symptoms: 症状列表
            
        Returns:
            是否有效
        """
        return len(symptoms) > 0

