"""
实体融合模块：融合多种实体提取方法的结果（LLM + NER模型 + 临床解析服务）
"""
from typing import List, Dict, Any
from app.models.nlu import SymptomEntity
from app.utils.logger import logger


class EntityFusion:
    """实体融合器：融合多源实体提取结果"""
    
    @staticmethod
    def fuse_entities(
        llm_entities: List[SymptomEntity],
        ner_entities: List[SymptomEntity],
        clinical_entities: List[SymptomEntity],
        fusion_strategy: str = "weighted_vote"
    ) -> List[SymptomEntity]:
        """
        融合多源实体提取结果
        
        Args:
            llm_entities: LLM提取的实体列表
            ner_entities: NER模型提取的实体列表
            clinical_entities: 临床解析服务提取的实体列表
            fusion_strategy: 融合策略（weighted_vote/majority_vote/max_confidence）
            
        Returns:
            融合后的实体列表
        """
        if fusion_strategy == "weighted_vote":
            return EntityFusion._weighted_vote_fusion(
                llm_entities, ner_entities, clinical_entities
            )
        elif fusion_strategy == "majority_vote":
            return EntityFusion._majority_vote_fusion(
                llm_entities, ner_entities, clinical_entities
            )
        elif fusion_strategy == "max_confidence":
            return EntityFusion._max_confidence_fusion(
                llm_entities, ner_entities, clinical_entities
            )
        else:
            logger.warning(f"未知的融合策略: {fusion_strategy}，使用默认策略")
            return EntityFusion._weighted_vote_fusion(
                llm_entities, ner_entities, clinical_entities
            )
    
    @staticmethod
    def _weighted_vote_fusion(
        llm_entities: List[SymptomEntity],
        ner_entities: List[SymptomEntity],
        clinical_entities: List[SymptomEntity]
    ) -> List[SymptomEntity]:
        """
        加权投票融合策略
        
        权重分配：
        - 临床解析服务：0.5（最高，因为提供标准编码）
        - LLM：0.3（语义理解能力强）
        - NER模型：0.2（基础实体识别）
        """
        # 权重配置
        weights = {
            'clinical': 0.5,
            'llm': 0.3,
            'ner': 0.2
        }
        
        # 合并所有实体
        all_entities = []
        for entity in llm_entities:
            entity.context = entity.context or {}
            entity.context['source'] = 'llm'
            entity.context['weight'] = weights['llm']
            all_entities.append(entity)
        
        for entity in ner_entities:
            entity.context = entity.context or {}
            entity.context['source'] = 'ner'
            entity.context['weight'] = weights['ner']
            all_entities.append(entity)
        
        for entity in clinical_entities:
            entity.context = entity.context or {}
            entity.context['source'] = 'clinical'
            entity.context['weight'] = weights['clinical']
            all_entities.append(entity)
        
        # 基于标准术语或CUI进行分组
        entity_groups = {}
        for entity in all_entities:
            # 使用CUI作为唯一标识（如果可用），否则使用标准术语
            key = entity.cui if entity.cui else entity.standard_term.lower()
            
            if key not in entity_groups:
                entity_groups[key] = []
            entity_groups[key].append(entity)
        
        # 融合每个组的实体
        fused_entities = []
        for key, group in entity_groups.items():
            if len(group) == 1:
                # 只有一个实体，直接使用
                fused_entities.append(group[0])
            else:
                # 多个实体，进行融合
                fused_entity = EntityFusion._merge_entity_group(group)
                fused_entities.append(fused_entity)
        
        # 按置信度排序
        fused_entities.sort(key=lambda x: x.confidence, reverse=True)
        
        logger.info(
            f"实体融合完成: LLM={len(llm_entities)}, NER={len(ner_entities)}, "
            f"临床={len(clinical_entities)}, 融合后={len(fused_entities)}"
        )
        
        return fused_entities
    
    @staticmethod
    def _majority_vote_fusion(
        llm_entities: List[SymptomEntity],
        ner_entities: List[SymptomEntity],
        clinical_entities: List[SymptomEntity]
    ) -> List[SymptomEntity]:
        """
        多数投票融合策略
        
        如果多个源都识别到同一个实体，则保留该实体
        """
        # 合并所有实体
        all_entities = []
        all_entities.extend(llm_entities)
        all_entities.extend(ner_entities)
        all_entities.extend(clinical_entities)
        
        # 基于标准术语或CUI进行分组
        entity_groups = {}
        for entity in all_entities:
            key = entity.cui if entity.cui else entity.standard_term.lower()
            
            if key not in entity_groups:
                entity_groups[key] = []
            entity_groups[key].append(entity)
        
        # 只保留被多个源识别的实体
        fused_entities = []
        for key, group in entity_groups.items():
            if len(group) >= 2:  # 至少2个源识别到
                fused_entity = EntityFusion._merge_entity_group(group)
                fused_entities.append(fused_entity)
        
        return fused_entities
    
    @staticmethod
    def _max_confidence_fusion(
        llm_entities: List[SymptomEntity],
        ner_entities: List[SymptomEntity],
        clinical_entities: List[SymptomEntity]
    ) -> List[SymptomEntity]:
        """
        最大置信度融合策略
        
        对于每个实体，选择置信度最高的版本
        """
        # 合并所有实体
        all_entities = []
        all_entities.extend(llm_entities)
        all_entities.extend(ner_entities)
        all_entities.extend(clinical_entities)
        
        # 基于标准术语或CUI进行分组
        entity_groups = {}
        for entity in all_entities:
            key = entity.cui if entity.cui else entity.standard_term.lower()
            
            if key not in entity_groups:
                entity_groups[key] = []
            entity_groups[key].append(entity)
        
        # 选择每个组中置信度最高的实体
        fused_entities = []
        for key, group in entity_groups.items():
            best_entity = max(group, key=lambda x: x.confidence)
            fused_entities.append(best_entity)
        
        return fused_entities
    
    @staticmethod
    def _merge_entity_group(group: List[SymptomEntity]) -> SymptomEntity:
        """
        合并实体组中的多个实体
        
        Args:
            group: 同一实体的多个版本
            
        Returns:
            合并后的实体
        """
        if not group:
            raise ValueError("实体组不能为空")
        
        if len(group) == 1:
            return group[0]
        
        # 选择最佳实体作为基础（优先选择有CUI的，其次选择置信度最高的）
        base_entity = None
        for entity in group:
            if entity.cui:
                base_entity = entity
                break
        
        if not base_entity:
            base_entity = max(group, key=lambda x: x.confidence)
        
        # 合并信息
        merged_entity = SymptomEntity(
            original_text=base_entity.original_text,
            standard_term=base_entity.standard_term,
            cui=base_entity.cui,
            confidence=base_entity.confidence,
            context=base_entity.context.copy() if base_entity.context else {}
        )
        
        # 计算加权平均置信度
        total_weight = 0
        weighted_confidence = 0
        sources = []
        
        for entity in group:
            weight = entity.context.get('weight', 0.33) if entity.context else 0.33
            weighted_confidence += entity.confidence * weight
            total_weight += weight
            sources.append(entity.context.get('source', 'unknown') if entity.context else 'unknown')
        
        if total_weight > 0:
            merged_entity.confidence = weighted_confidence / total_weight
        
        # 更新上下文信息
        merged_entity.context['sources'] = list(set(sources))
        merged_entity.context['fusion_count'] = len(group)
        
        # 合并其他属性（选择最完整的）
        for entity in group:
            if entity.location and not merged_entity.location:
                merged_entity.location = entity.location
            if entity.severity and not merged_entity.severity:
                merged_entity.severity = entity.severity
            if entity.duration and not merged_entity.duration:
                merged_entity.duration = entity.duration
            if entity.frequency and not merged_entity.frequency:
                merged_entity.frequency = entity.frequency
        
        return merged_entity

