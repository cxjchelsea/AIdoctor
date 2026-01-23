"""
文本处理工具
"""
import re
import logging
from typing import List, Tuple

logger = logging.getLogger(__name__)


class TextProcessor:
    """文本处理器"""
    
    @staticmethod
    def clean_text(text: str) -> str:
        """
        文本清洗
        去除标点、特殊字符，统一编码
        """
        if not text:
            return ""
        
        # 去除多余空白字符
        text = re.sub(r'\s+', ' ', text)
        
        # 去除首尾空白
        text = text.strip()
        
        return text
    
    @staticmethod
    def extract_time_expressions(text: str) -> List[Tuple[str, str]]:
        """
        提取时间表达式
        返回: [(时间表达式, 标准化时间), ...]
        """
        time_patterns = [
            (r'最近(\d+)天', 'recent'),
            (r'(\d+)天前', 'past'),
            (r'(\d+)周前', 'past'),
            (r'(\d+)个月前', 'past'),
            (r'(\d+)年前', 'past'),
            (r'最近', 'recent'),
            (r'刚才', 'just_now'),
            (r'今天', 'today'),
            (r'昨天', 'yesterday'),
            (r'前天', 'day_before_yesterday'),
        ]
        
        results = []
        for pattern, normalized in time_patterns:
            matches = re.finditer(pattern, text)
            for match in matches:
                results.append((match.group(0), normalized))
        
        return results
    
    @staticmethod
    def extract_severity_expressions(text: str) -> List[Tuple[str, str]]:
        """
        提取严重度表达式
        返回: [(严重度表达式, 标准化严重度), ...]
        """
        severity_mapping = {
            '轻微': 'mild',
            '轻度': 'mild',
            '有点': 'mild',
            '稍微': 'mild',
            '中等': 'moderate',
            '中度': 'moderate',
            '比较': 'moderate',
            '严重': 'severe',
            '重度': 'severe',
            '很严重': 'severe',
            '非常严重': 'severe',
        }
        
        results = []
        for expr, normalized in severity_mapping.items():
            if expr in text:
                results.append((expr, normalized))
        
        return results
    
    @staticmethod
    def extract_trigger_expressions(text: str) -> List[str]:
        """
        提取诱因表达式
        返回: [诱因, ...]
        """
        trigger_keywords = [
            '活动后', '运动后', '劳累后', '情绪激动后',
            '进食后', '饭后', '空腹时',
            '夜间', '晚上', '白天',
            '受凉后', '感冒后',
        ]
        
        triggers = []
        for keyword in trigger_keywords:
            if keyword in text:
                triggers.append(keyword)
        
        return triggers
    
    @staticmethod
    def split_sentences(text: str) -> List[str]:
        """
        分句
        使用标点符号分割句子
        """
        # 中文句号、问号、感叹号
        sentences = re.split(r'[。！？；\n]', text)
        # 过滤空句子
        sentences = [s.strip() for s in sentences if s.strip()]
        return sentences
    
    @staticmethod
    def normalize_text(text: str) -> str:
        """
        文本标准化
        统一编码、大小写等
        """
        # 统一全角半角
        text = text.replace('，', ',').replace('。', '.').replace('！', '!')
        text = text.replace('？', '?').replace('：', ':').replace('；', ';')
        
        return text

