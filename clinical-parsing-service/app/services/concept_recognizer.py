"""
医学概念识别器
"""
import logging
from typing import List, Dict, Optional
from app.utils.vocabulary_loader import VocabularyLoader
from app.utils.text_processor import TextProcessor

logger = logging.getLogger(__name__)


class ConceptRecognizer:
    """医学概念识别器"""
    
    def __init__(self, vocabulary_loader: VocabularyLoader):
        """
        初始化概念识别器
        
        Args:
            vocabulary_loader: 词表加载器
        """
        self.vocabulary_loader = vocabulary_loader
        self.text_processor = TextProcessor()
    
    def recognize(self, text: str) -> List[Dict]:
        """
        识别医学概念
        
        Args:
            text: 输入文本
        
        Returns:
            识别到的概念列表
        """
        if not text:
            return []
        
        # Do not log raw clinical text. Even DEBUG logs may be enabled in a
        # non-production environment and must not become an accidental PHI sink.
        logger.debug("开始识别医学概念: text_length=%s", len(text))
        concepts = []
        
        # 文本预处理
        cleaned_text = self.text_processor.clean_text(text)
        sentences = self.text_processor.split_sentences(cleaned_text)
        logger.debug("分句完成: sentence_count=%s", len(sentences))
        
        # 对每个句子进行概念识别
        for sentence in sentences:
            sentence_concepts = self._recognize_in_sentence(sentence)
            logger.debug("单句概念识别完成: concept_count=%s", len(sentence_concepts))
            concepts.extend(sentence_concepts)
        
        # 去重
        concepts = self._deduplicate_concepts(concepts)
        logger.debug("去重后概念数: concept_count=%s", len(concepts))
        
        return concepts
    
    def _recognize_in_sentence(self, sentence: str) -> List[Dict]:
        """在单个句子中识别概念"""
        concepts = []
        
        # 尝试识别各类概念
        vocab_types = ['symptom', 'disease', 'medication', 'allergy', 'examination', 'indicator']
        
        for vocab_type in vocab_types:
            # 在句子中查找匹配的概念（使用模糊匹配，因为句子中可能包含多个词）
            match = self.vocabulary_loader.find_match(sentence, vocab_type, exact_match=False)
            if match:
                concepts.append({
                    'original_text': sentence,
                    'concept_type': vocab_type,
                    'standard_term': match['standard_term'],
                    'cui': match.get('cui', ''),
                    'icd': match.get('icd', ''),
                    'snomed': match.get('snomed', ''),
                    'loinc': match.get('loinc', ''),
                    'atc': match.get('atc', ''),
                    'confidence': 0.85,  # 句子中包含匹配的置信度
                })
        
        return concepts
    
    def _deduplicate_concepts(self, concepts: List[Dict]) -> List[Dict]:
        """去重概念列表"""
        seen = set()
        unique_concepts = []
        
        for concept in concepts:
            # 使用标准术语和概念类型作为唯一标识
            key = (concept.get('standard_term', ''), concept.get('concept_type', ''))
            if key not in seen:
                seen.add(key)
                unique_concepts.append(concept)
        
        return unique_concepts

