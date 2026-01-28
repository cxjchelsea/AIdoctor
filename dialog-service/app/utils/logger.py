"""
日志配置工具
支持控制台和文件输出，自动轮转
"""
import logging
import sys
from pathlib import Path
from logging.handlers import RotatingFileHandler
from typing import Optional


def setup_logger(
    name: str = "dialog-service",
    level: str = "INFO",
    log_file: Optional[str] = None,
    format_string: Optional[str] = None,
    max_bytes: int = 10 * 1024 * 1024,  # 10MB
    backup_count: int = 5
) -> logging.Logger:
    """
    设置日志配置（支持控制台和文件输出）
    
    Args:
        name: 日志名称
        level: 日志级别（DEBUG/INFO/WARNING/ERROR）
        log_file: 日志文件路径（可选，如果为None则不输出到文件）
        format_string: 日志格式字符串（可选）
        max_bytes: 日志文件最大大小（字节），默认10MB
        backup_count: 保留的备份文件数量，默认5个
        
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
    console_formatter = logging.Formatter(format_string, datefmt='%Y-%m-%d %H:%M:%S')
    console_handler.setFormatter(console_formatter)
    logger.addHandler(console_handler)
    
    # 文件输出（如果指定了日志文件）
    if log_file:
        log_path = Path(log_file)
        # 创建日志目录
        log_path.parent.mkdir(parents=True, exist_ok=True)
        
        # 文件处理器（轮转，避免日志文件过大）
        file_handler = RotatingFileHandler(
            log_file,
            maxBytes=max_bytes,
            backupCount=backup_count,
            encoding='utf-8'
        )
        file_handler.setLevel(getattr(logging, level.upper()))
        # 文件日志格式包含更多信息
        file_format_string = (
            "[%(asctime)s] [%(levelname)s] [%(name)s] [%(filename)s:%(lineno)d] "
            "[%(funcName)s] - %(message)s"
        )
        file_formatter = logging.Formatter(file_format_string, datefmt='%Y-%m-%d %H:%M:%S')
        file_handler.setFormatter(file_formatter)
        logger.addHandler(file_handler)
    
    return logger

