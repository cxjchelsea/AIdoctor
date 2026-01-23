"""
归一化词表加载器
"""
import json
import csv
import logging
from pathlib import Path
from typing import Dict, List, Optional, Set
from collections import defaultdict

logger = logging.getLogger(__name__)


class VocabularyLoader:
    """归一化词表加载器"""
    
    def __init__(self, base_path: str = "data/vocabularies"):
        """
        初始化词表加载器
        
        Args:
            base_path: 词表文件基础路径
        """
        self.base_path = Path(base_path)
        self.vocabularies: Dict[str, Dict[str, Dict]] = {}
        self.indexes: Dict[str, Dict[str, Set[str]]] = {}  # 倒排索引：同义词 -> 标准术语集合
        
    def load_vocabulary(self, vocab_type: str, file_path: Optional[str] = None) -> bool:
        """
        加载归一化词表
        
        Args:
            vocab_type: 词表类型 (symptom/disease/medication/allergy/examination/indicator)
            file_path: 词表文件路径（可选，默认从base_path读取）
        
        Returns:
            是否加载成功
        """
        if file_path is None:
            file_path = self.base_path / f"{vocab_type}_normalization.csv"
        else:
            file_path = Path(file_path)
        
        if not file_path.exists():
            logger.warning(f"词表文件不存在: {file_path}")
            return False
        
        try:
            vocabulary = {}
            index = defaultdict(set)
            
            with open(file_path, 'r', encoding='utf-8') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    standard_term = row.get('standard_term', '').strip()
                    if not standard_term:
                        continue
                    
                    # 获取同义词列表
                    synonyms = []
                    for key in ['synonym', 'alias', 'common_name', 'variant']:
                        if key in row and row[key]:
                            synonyms.extend([s.strip() for s in row[key].split(',') if s.strip()])
                    
                    # 获取编码
                    cui = row.get('cui', '').strip()
                    icd = row.get('icd', '').strip()
                    snomed = row.get('snomed', '').strip()
                    loinc = row.get('loinc', '').strip()
                    atc = row.get('atc', '').strip()
                    
                    # 存储标准术语信息
                    vocabulary[standard_term] = {
                        'cui': cui,
                        'icd': icd,
                        'snomed': snomed,
                        'loinc': loinc,
                        'atc': atc,
                        'synonyms': synonyms,
                    }
                    
                    # 构建倒排索引：同义词 -> 标准术语
                    index[standard_term].add(standard_term)  # 标准术语本身
                    for synonym in synonyms:
                        index[synonym.lower()].add(standard_term)
            
            self.vocabularies[vocab_type] = vocabulary
            self.indexes[vocab_type] = {k: v for k, v in index.items()}
            
            logger.info(f"加载词表成功: {vocab_type}, 共 {len(vocabulary)} 条记录")
            return True
            
        except Exception as e:
            logger.error(f"加载词表失败: {vocab_type}, 错误: {e}", exc_info=True)
            return False
    
    def load_all_vocabularies(self) -> bool:
        """
        加载所有词表
        
        Returns:
            是否全部加载成功
        """
        vocab_types = ['symptom', 'disease', 'medication', 'allergy', 'examination', 'indicator']
        success_count = 0
        
        for vocab_type in vocab_types:
            if self.load_vocabulary(vocab_type):
                success_count += 1
        
        logger.info(f"词表加载完成: {success_count}/{len(vocab_types)}")
        return success_count > 0
    
    def find_match(self, text: str, vocab_type: str, exact_match: bool = True) -> Optional[Dict]:
        """
        查找匹配的标准术语
        
        Args:
            text: 输入文本
            vocab_type: 词表类型
            exact_match: 是否精确匹配
        
        Returns:
            匹配的标准术语信息，如果未找到返回None
        """
        if vocab_type not in self.indexes:
            return None
        
        text_lower = text.lower().strip()
        index = self.indexes[vocab_type]
        
        # 精确匹配
        if text_lower in index:
            standard_terms = index[text_lower]
            if standard_terms:
                # 返回第一个匹配的标准术语
                standard_term = list(standard_terms)[0]
                vocab = self.vocabularies.get(vocab_type, {})
                return {
                    'standard_term': standard_term,
                    **vocab.get(standard_term, {})
                }
        
        # 模糊匹配（如果精确匹配失败）
        if not exact_match:
            # 在文本中查找包含的同义词（优先匹配更长的同义词）
            matched_synonyms = []
            for synonym, standard_terms in index.items():
                if synonym in text_lower:
                    matched_synonyms.append((len(synonym), synonym, standard_terms))
            
            # 如果直接包含匹配失败，尝试部分匹配（适用于中文）
            # 例如：文本中有"疼"，词表中有"头疼"，应该能匹配到
            if not matched_synonyms:
                # 提取文本中的关键词（去除常见助词、连词等）
                import re
                # 提取2-4字的中文词汇
                words = re.findall(r'[\u4e00-\u9fa5]{2,4}', text_lower)
                for word in words:
                    # 检查词表中的同义词是否包含这个词，或者这个词是否包含同义词
                    for synonym, standard_terms in index.items():
                        if len(synonym) >= 2:  # 只匹配2字以上的同义词
                            if word in synonym or synonym in word:
                                matched_synonyms.append((len(synonym), synonym, standard_terms))
                                break  # 每个词只匹配一次
            
            # 按长度降序排序，优先匹配更长的同义词（更精确）
            if matched_synonyms:
                matched_synonyms.sort(reverse=True, key=lambda x: x[0])
                synonym_len, matched_synonym, standard_terms = matched_synonyms[0]
                standard_term = list(standard_terms)[0]
                vocab = self.vocabularies.get(vocab_type, {})
                return {
                    'standard_term': standard_term,
                    **vocab.get(standard_term, {})
                }
        
        return None
    
    def get_all_vocab_types(self) -> List[str]:
        """获取所有已加载的词表类型"""
        return list(self.vocabularies.keys())

