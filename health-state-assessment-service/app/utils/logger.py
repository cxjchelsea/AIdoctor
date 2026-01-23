"""
日志配置工具
"""
import logging
import sys
from typing import Optional


def setup_logger(
    name: str = "health-state-assessment-service",
    level: str = "INFO",
    format_string: Optional[str] = None
) -> logging.Logger:
    """
    设置日志配置
    
    Args:
        name: 日志名称
        level: 日志级别（DEBUG/INFO/WARNING/ERROR）
        format_string: 日志格式字符串（可选）
    
    Returns:
        配置好的Logger实例
    """
    if format_string is None:
        format_string = (
            "[%(asctime)s] [%(levelname)s] [%(name)s] [%(filename)s:%(lineno)d] "
            "- %(message)s"
        )
    
    logger = logging.getLogger(name)
    logger.setLevel(getattr(logging, level.upper()))
    
    # 避免重复添加handler
    if logger.handlers:
        return logger
    
    # 控制台输出
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setLevel(getattr(logging, level.upper()))
    console_formatter = logging.Formatter(format_string)
    console_handler.setFormatter(console_formatter)
    logger.addHandler(console_handler)
    
    return logger


# 创建默认logger
logger = setup_logger()

