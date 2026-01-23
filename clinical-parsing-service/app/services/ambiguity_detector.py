"""
歧义表达判定器
"""
import json
import logging
from pathlib import Path
from typing import List, Dict, Optional

logger = logging.getLogger(__name__)


class AmbiguityDetector:
    """歧义表达判定器"""
    
    def __init__(self, rules_path: Optional[str] = None):
        """
        初始化歧义判定器
        
        Args:
            rules_path: 歧义规则库文件路径
        """
        self.rules_path = rules_path or "data/vocabularies/ambiguity_rules.json"
        self.rules: List[Dict] = []
        self._load_rules()
    
    def _load_rules(self):
        """加载歧义规则"""
        rules_file = Path(self.rules_path)
        if not rules_file.exists():
            logger.warning(f"歧义规则文件不存在: {rules_file}")
            # 使用默认规则
            self.rules = self._get_default_rules()
            return
        
        try:
            with open(rules_file, 'r', encoding='utf-8') as f:
                self.rules = json.load(f)
            logger.info(f"加载歧义规则成功: {len(self.rules)} 条")
        except Exception as e:
            logger.error(f"加载歧义规则失败: {e}", exc_info=True)
            self.rules = self._get_default_rules()
    
    def _get_default_rules(self) -> List[Dict]:
        """获取默认歧义规则"""
        return [
            {
                'text': '胸痛',
                'type': 'ambiguous',
                'suggested_questions': [
                    '请描述一下疼痛的具体性质？是压迫感还是刺痛？',
                    '疼痛持续多长时间？',
                    '疼痛在什么情况下出现？'
                ]
            },
            {
                'text': '头痛',
                'type': 'ambiguous',
                'suggested_questions': [
                    '请描述一下头痛的具体性质？是胀痛还是刺痛？',
                    '头痛持续多长时间？',
                    '头痛在什么情况下出现？'
                ]
            },
            {
                'text': '腹痛',
                'type': 'ambiguous',
                'suggested_questions': [
                    '请描述一下腹痛的具体位置？',
                    '腹痛持续多长时间？',
                    '腹痛在什么情况下出现？'
                ]
            },
        ]
    
    def detect(self, text: str, concepts: List[Dict]) -> List[Dict]:
        """
        检测歧义表达
        
        Args:
            text: 原始文本
            concepts: 识别到的概念列表
        
        Returns:
            歧义表达列表
        """
        ambiguous_expressions = []
        
        # 检查文本中是否包含歧义表达
        for rule in self.rules:
            ambiguous_text = rule.get('text', '')
            if ambiguous_text in text:
                ambiguous_expressions.append({
                    'text': ambiguous_text,
                    'type': rule.get('type', 'ambiguous'),
                    'suggested_questions': rule.get('suggested_questions', [])
                })
        
        return ambiguous_expressions

