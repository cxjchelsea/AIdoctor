"""
实体归一化模块：整合LLM提取的实体和临床解析服务的结果
"""
from typing import List, Dict, Any, Optional
from app.models.nlu import SymptomEntity
from app.services.nlu.clinical_parser_client import ClinicalParserClient
from app.utils.logger import logger


class EntityNormalizer:
    """实体归一化器"""
    
    def __init__(self, clinical_parser_client: Optional[ClinicalParserClient] = None):
        """
        初始化实体归一化器
        
        Args:
            clinical_parser_client: 临床解析服务客户端，如果为None则自动创建
        """
        self.clinical_parser_client = clinical_parser_client or ClinicalParserClient()
    
    async def normalize(
        self,
        entities: List[SymptomEntity],
        original_text: str,
        user_id: str = "default_user",
        session_id: str = "default_session"
    ) -> List[SymptomEntity]:
        """
        归一化实体列表
        
        Args:
            entities: 原始实体列表（从LLM提取）
            original_text: 原始输入文本
            user_id: 用户ID
            session_id: 会话ID
            
        Returns:
            归一化后的实体列表（包含CUI、ICD等编码）
        """
        if not entities:
            return []
        
        try:
            # 调用临床解析服务进行归一化
            concepts = await self.clinical_parser_client.normalize_concepts(
                original_text, user_id, session_id
            )
            
            if not concepts:
                logger.debug("临床解析服务未返回概念，使用原始实体")
                return entities
            
            # 构建概念映射表（按原始文本匹配）
            concept_map = {}
            for concept in concepts:
                original_text_key = concept.get("originalText", "").lower()
                if original_text_key:
                    concept_map[original_text_key] = concept
            
            # 归一化实体
            normalized_entities = []
            for entity in entities:
                normalized_entity = self._normalize_single_entity(entity, concept_map, concepts)
                normalized_entities.append(normalized_entity)
            
            # 添加临床解析服务识别到但LLM未提取的实体
            llm_extracted_texts = {e.original_text.lower() for e in entities}
            for concept in concepts:
                original_text_key = concept.get("originalText", "").lower()
                if original_text_key and original_text_key not in llm_extracted_texts:
                    # 这是一个新发现的实体
                    new_entity = self._concept_to_entity(concept)
                    if new_entity:
                        normalized_entities.append(new_entity)
            
            logger.info(f"实体归一化完成: 原始实体数={len(entities)}, 归一化后实体数={len(normalized_entities)}")
            return normalized_entities
            
        except Exception as e:
            logger.error(f"实体归一化失败: {e}", exc_info=True)
            # 失败时返回原始实体
            return entities
    
    def _normalize_single_entity(
        self,
        entity: SymptomEntity,
        concept_map: Dict[str, Dict[str, Any]],
        all_concepts: List[Dict[str, Any]]
    ) -> SymptomEntity:
        """
        归一化单个实体
        
        Args:
            entity: 原始实体
            concept_map: 概念映射表
            all_concepts: 所有概念列表
            
        Returns:
            归一化后的实体
        """
        # 尝试精确匹配
        original_text_lower = entity.original_text.lower()
        matched_concept = concept_map.get(original_text_lower)
        
        # 如果精确匹配失败，尝试模糊匹配（基于标准术语）
        if not matched_concept:
            for concept in all_concepts:
                normalized_term = concept.get("normalizedSymptom", "").lower()
                if normalized_term and entity.standard_term.lower() in normalized_term:
                    matched_concept = concept
                    break
        
        # 如果找到匹配的概念，更新实体信息
        if matched_concept:
            # 更新标准术语（使用临床解析服务的归一化结果）
            if matched_concept.get("normalizedSymptom"):
                entity.standard_term = matched_concept["normalizedSymptom"]
            
            # 添加编码信息
            if matched_concept.get("cui"):
                entity.cui = matched_concept["cui"]
            
            # 更新置信度（取LLM和临床解析服务的较高值）
            clinical_confidence = matched_concept.get("confidence", 0.0)
            entity.confidence = max(entity.confidence, clinical_confidence)
            
            # 更新上下文信息
            if not entity.context:
                entity.context = {}
            entity.context["clinical_parser_confidence"] = clinical_confidence
            entity.context["concept_type"] = matched_concept.get("conceptType", "symptom")
        
        return entity
    
    def _concept_to_entity(self, concept: Dict[str, Any]) -> Optional[SymptomEntity]:
        """
        将临床解析服务的概念转换为SymptomEntity
        
        Args:
            concept: 概念字典
            
        Returns:
            SymptomEntity或None
        """
        # 只处理症状类型的概念
        concept_type = concept.get("conceptType", "")
        if concept_type != "symptom":
            return None
        
        original_text = concept.get("originalText", "")
        normalized_term = concept.get("normalizedSymptom", original_text)
        
        if not original_text:
            return None
        
        return SymptomEntity(
            original_text=original_text,
            standard_term=normalized_term,
            cui=concept.get("cui"),
            confidence=concept.get("confidence", 0.8),
            context={
                "concept_type": concept_type,
                "clinical_parser_source": True
            }
        )
    
    def merge_entities(self, entity_lists: List[List[SymptomEntity]]) -> List[SymptomEntity]:
        """
        合并多个实体列表，去重
        
        Args:
            entity_lists: 实体列表的列表
            
        Returns:
            合并去重后的实体列表
        """
        # 使用CUI或标准术语进行去重
        seen_entities = {}
        merged_entities = []
        
        for entity_list in entity_lists:
            for entity in entity_list:
                # 优先使用CUI作为唯一标识
                key = entity.cui if entity.cui else entity.standard_term.lower()
                
                if key not in seen_entities:
                    seen_entities[key] = entity
                    merged_entities.append(entity)
                else:
                    # 如果已存在，合并置信度（取较高值）
                    existing_entity = seen_entities[key]
                    existing_entity.confidence = max(existing_entity.confidence, entity.confidence)
                    # 合并上下文信息
                    if entity.context:
                        existing_entity.context.update(entity.context)
        
        return merged_entities

