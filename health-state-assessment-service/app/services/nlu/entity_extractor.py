"""
实体提取模块：从用户输入中提取症状、基本信息、生命体征等实体
"""
from typing import Optional, Dict, Any, List
from app.utils.llm_client import LangChainLLMClient, LLMConfig, LLMBackend
from app.config.settings import settings
from app.models.nlu import EntityResult, SymptomEntity
from app.services.nlu.prompt_manager import PromptManager
from app.services.nlu.response_parser import ResponseParser
from app.services.nlu.fallback_matcher import FallbackMatcher
from app.services.nlu.entity_normalizer import EntityNormalizer
from app.services.nlu.ner_model import NERModel, SimpleNERModel
from app.services.nlu.entity_fusion import EntityFusion
from app.utils.logger import logger


class EntityExtractor:
    """实体提取器"""
    
    def __init__(
        self,
        llm_client: Optional[LangChainLLMClient] = None,
        entity_normalizer: Optional[EntityNormalizer] = None,
        ner_model: Optional[NERModel] = None
    ):
        """
        初始化实体提取器
        
        Args:
            llm_client: LLM客户端，如果为None则自动创建
            entity_normalizer: 实体归一化器，如果为None则自动创建
            ner_model: NER模型，如果为None则自动创建
        """
        self.nlu_config = settings.nlu
        self.use_llm = self.nlu_config.use_llm
        self.enable_fallback = self.nlu_config.enable_fallback
        self.use_ner = getattr(self.nlu_config, 'use_ner', False)
        self.fusion_strategy = getattr(self.nlu_config, 'fusion_strategy', 'weighted_vote')
        
        # 初始化LLM客户端（如果需要）
        self.llm_client = llm_client
        if self.use_llm and self.llm_client is None:
            try:
                llm_config = LLMConfig(
                    backend=LLMBackend(self.nlu_config.llm_backend),
                    model=self.nlu_config.llm_model,
                    temperature=self.nlu_config.llm_temperature,
                    max_tokens=self.nlu_config.llm_max_tokens,
                    timeout=self.nlu_config.llm_timeout,
                    max_retries=self.nlu_config.llm_max_retries,
                    openai_api_key=self.nlu_config.openai_api_key,
                    openai_base_url=self.nlu_config.openai_base_url,
                    chatglm_api_url=self.nlu_config.chatglm_api_url,
                    chatglm_api_key=self.nlu_config.chatglm_api_key,
                    ollama_base_url=self.nlu_config.ollama_base_url,
                    ollama_model=self.nlu_config.ollama_model,
                    custom_api_url=self.nlu_config.custom_api_url,
                    custom_api_key=self.nlu_config.custom_api_key
                )
                self.llm_client = LangChainLLMClient(config=llm_config)
                logger.info("实体提取器LLM客户端初始化成功")
            except Exception as e:
                logger.warning(f"实体提取器LLM客户端初始化失败，将使用降级方案: {e}")
                self.use_llm = False
                self.llm_client = None
        
        # 初始化实体归一化器
        self.entity_normalizer = entity_normalizer or EntityNormalizer()
        
        # 初始化NER模型（如果需要）
        self.ner_model = ner_model
        if self.use_ner and self.ner_model is None:
            try:
                self.ner_model = NERModel()
                if not self.ner_model.is_available():
                    # 如果NER模型不可用，使用简单NER模型作为降级
                    logger.warning("NER模型不可用，使用简单NER模型作为降级")
                    self.ner_model = SimpleNERModel()
            except Exception as e:
                logger.warning(f"NER模型初始化失败，将不使用NER模型: {e}")
                self.use_ner = False
                self.ner_model = None
    
    async def extract(
        self,
        user_input: str,
        intent: str,
        provided_symptoms: Optional[List[str]] = None,
        basic_info: Optional[Dict[str, Any]] = None,
        user_id: str = "default_user",
        session_id: str = "default_session"
    ) -> EntityResult:
        """
        提取实体
        
        Args:
            user_input: 用户输入文本
            intent: 用户意图
            provided_symptoms: 已提供的症状列表（可选）
            basic_info: 已提供的基本信息（可选）
            user_id: 用户ID（用于临床解析服务调用）
            session_id: 会话ID（用于临床解析服务调用）
        
        Returns:
            EntityResult: 实体提取结果（已归一化）
        """
        if not user_input or not user_input.strip():
            return EntityResult(
                symptoms=[],
                basic_info=basic_info or {},
                vital_signs={},
                temporal_info={},
                severity_info={},
                entities=[]
            )
        
        # 多源实体提取
        llm_entities = []
        ner_entities = []
        fallback_entities = []
        llm_result = None
        
        # 1. LLM提取实体
        if self.use_llm and self.llm_client:
            try:
                logger.info(f"使用LLM进行实体提取: user_input={user_input[:50]}..., intent={intent}")
                llm_result = await self._extract_with_llm(user_input, intent)
                if llm_result:
                    llm_entities = llm_result.symptoms
                    logger.info(
                        f"LLM实体提取成功: 提取到 {len(llm_entities)} 个症状, "
                        f"症状列表={[s.standard_term for s in llm_entities[:5]]}"
                    )
            except Exception as e:
                logger.error(f"LLM实体提取失败: {e}", exc_info=True)
        
        # 2. NER模型提取实体
        if self.use_ner and self.ner_model:
            try:
                ner_entities = await self.ner_model.extract_entities(user_input)
                logger.debug(f"NER模型提取到 {len(ner_entities)} 个实体")
            except Exception as e:
                logger.error(f"NER模型提取失败: {e}", exc_info=True)
        
        # 3. 降级方案提取实体（只有当LLM和NER都调用失败时才使用）
        # 注意：如果LLM调用成功但提取到0个症状，这是正常情况，不应该使用降级方案
        if llm_result is None and not ner_entities and self.enable_fallback:
            logger.info("LLM和NER都未成功，使用降级方案进行实体提取")
            fallback_result = self._extract_with_fallback(user_input, intent, provided_symptoms, basic_info)
            if fallback_result:
                fallback_entities = fallback_result.symptoms
                logger.info(
                    f"降级方案实体提取结果: 提取到 {len(fallback_entities)} 个症状, "
                    f"症状列表={[s.standard_term for s in fallback_entities[:5]]}"
                )
        elif llm_result is not None and len(llm_entities) == 0:
            logger.info("LLM调用成功，但未提取到症状（这是正常情况，例如用户只想体检）")
        
        # 4. 融合多源结果
        if llm_entities or ner_entities:
            # 使用多源融合
            fused_symptoms = EntityFusion.fuse_entities(
                llm_entities,
                ner_entities,
                [],  # 临床解析服务的实体将在归一化阶段添加
                fusion_strategy=self.fusion_strategy
            )
            
            # 创建结果对象
            result = EntityResult(
                symptoms=fused_symptoms,
                basic_info=basic_info or {},
                vital_signs={},
                temporal_info={},
                severity_info={},
                entities=[]
            )
            
            # 从LLM结果中提取其他信息（如果有）
            if llm_result:
                result.basic_info = llm_result.basic_info or basic_info or {}
                result.vital_signs = llm_result.vital_signs or {}
                result.temporal_info = llm_result.temporal_info or {}
        elif fallback_entities:
            # 使用降级结果
            result = EntityResult(
                symptoms=fallback_entities,
                basic_info=basic_info or {},
                vital_signs={},
                temporal_info={},
                severity_info={},
                entities=[]
            )
        else:
            # 所有方法都失败，返回空结果
            return EntityResult(
                symptoms=[],
                basic_info=basic_info or {},
                vital_signs={},
                temporal_info={},
                severity_info={},
                entities=[]
            )
        
        # 合并已提供的信息
        result = self._merge_provided_info(result, provided_symptoms, basic_info)
        
        # 记录最终实体提取结果
        logger.info(
            f"实体提取完成: 总症状数={len(result.symptoms)}, "
            f"症状列表={[s.standard_term for s in result.symptoms[:5]]}, "
            f"基本信息={list(result.basic_info.keys()) if result.basic_info else []}, "
            f"生命体征={list(result.vital_signs.keys()) if result.vital_signs else []}"
        )
        
        # 5. 实体归一化：调用临床解析服务进行归一化
        if result.symptoms:
            try:
                normalized_symptoms = await self.entity_normalizer.normalize(
                    result.symptoms,
                    user_input,
                    user_id,
                    session_id
                )
                
                # 如果归一化后发现了新实体，再次融合
                if normalized_symptoms != result.symptoms:
                    # 从归一化结果中提取临床解析服务发现的实体
                    clinical_entities = [
                        s for s in normalized_symptoms
                        if s.context.get('clinical_parser_source', False)
                    ]
                    
                    # 重新融合（包含临床解析服务的实体）
                    if clinical_entities:
                        existing_entities = [
                            s for s in normalized_symptoms
                            if not s.context.get('clinical_parser_source', False)
                        ]
                        final_symptoms = EntityFusion.fuse_entities(
                            existing_entities,
                            [],
                            clinical_entities,
                            fusion_strategy=self.fusion_strategy
                        )
                        result.symptoms = final_symptoms
                    else:
                        result.symptoms = normalized_symptoms
                else:
                    result.symptoms = normalized_symptoms
                
                # 更新entities列表
                result.entities = self._convert_to_legacy_format(result.symptoms)
                logger.info(f"实体归一化完成: {len(result.symptoms)} 个症状已归一化")
            except Exception as e:
                logger.error(f"实体归一化失败: {e}", exc_info=True)
                # 归一化失败不影响返回结果，使用原始结果
                result.entities = self._convert_to_legacy_format(result.symptoms)
        else:
            result.entities = []
        
        return result
    
    async def _extract_with_llm(
        self,
        user_input: str,
        intent: str
    ) -> Optional[EntityResult]:
        """
        使用LLM提取实体
        
        Args:
            user_input: 用户输入文本
            intent: 用户意图
            
        Returns:
            EntityResult或None（如果失败）
        """
        try:
            # 获取Prompt
            prompt = PromptManager.get_entity_extraction_prompt(user_input, intent)
            
            # 调用LLM
            response = await self.llm_client.generate(prompt)
            
            # 解析响应
            parser = ResponseParser()
            data = parser.parse_json_response(response)
            
            if not data:
                return None
            
            # 验证结果
            if not parser.validate_entity_result(data):
                logger.warning(f"LLM返回的实体提取结果格式无效: {data}")
                return None
            
            # 构造症状实体列表
            symptoms = []
            for symptom_data in data.get("symptoms", []):
                symptoms.append(SymptomEntity(
                    original_text=symptom_data.get("original_text", ""),
                    standard_term=symptom_data.get("standard_term", symptom_data.get("original_text", "")),
                    confidence=0.8,  # LLM提取的默认置信度
                    context={},
                    location=symptom_data.get("location"),
                    severity=symptom_data.get("severity"),
                    duration=symptom_data.get("duration"),
                    frequency=symptom_data.get("frequency")
                ))
            
            # 构造结果
            return EntityResult(
                symptoms=symptoms,
                basic_info=data.get("basic_info", {}),
                vital_signs=data.get("vital_signs", {}),
                temporal_info=data.get("temporal_info", {}),
                severity_info={},  # 可以从symptoms中提取
                entities=self._convert_to_legacy_format(symptoms)
            )
        except Exception as e:
            logger.error(f"LLM实体提取过程出错: {e}", exc_info=True)
            return None
    
    def _extract_with_fallback(
        self,
        user_input: str,
        intent: str,
        provided_symptoms: Optional[List[str]] = None,
        basic_info: Optional[Dict[str, Any]] = None
    ) -> EntityResult:
        """
        使用降级方案提取实体（正则表达式匹配）
        
        Args:
            user_input: 用户输入文本
            intent: 用户意图
            provided_symptoms: 已提供的症状列表
            basic_info: 已提供的基本信息
            
        Returns:
            EntityResult
        """
        # 提取症状实体
        entities = FallbackMatcher.extract_entities(user_input, intent)
        
        # 如果有已提供的症状，添加到实体列表
        if provided_symptoms:
            for symptom in provided_symptoms:
                if symptom not in [e.get("value") for e in entities if e.get("type") == "symptom"]:
                    entities.append({
                        "type": "symptom",
                        "value": symptom,
                        "original_text": symptom
                    })
        
        # 转换为SymptomEntity列表
        symptoms = []
        for entity in entities:
            if entity.get("type") == "symptom":
                symptoms.append(SymptomEntity(
                    original_text=entity.get("original_text", entity.get("value", "")),
                    standard_term=entity.get("value", ""),
                    confidence=0.6,  # 降级方案的置信度较低
                    context={}
                ))
        
        return EntityResult(
            symptoms=symptoms,
            basic_info=basic_info or {},
            vital_signs={},
            temporal_info={},
            severity_info={},
            entities=entities
        )
    
    def _merge_provided_info(
        self,
        result: EntityResult,
        provided_symptoms: Optional[List[str]],
        basic_info: Optional[Dict[str, Any]]
    ) -> EntityResult:
        """
        合并已提供的信息
        
        Args:
            result: LLM提取的结果
            provided_symptoms: 已提供的症状列表
            basic_info: 已提供的基本信息
            
        Returns:
            合并后的结果
        """
        # 合并症状
        if provided_symptoms:
            existing_terms = {s.standard_term for s in result.symptoms}
            for symptom_text in provided_symptoms:
                if symptom_text not in existing_terms:
                    result.symptoms.append(SymptomEntity(
                        original_text=symptom_text,
                        standard_term=symptom_text,
                        confidence=1.0,  # 用户明确提供的症状置信度最高
                        context={}
                    ))
        
        # 合并基本信息
        if basic_info:
            result.basic_info.update(basic_info)
        
        # 更新entities列表
        result.entities = self._convert_to_legacy_format(result.symptoms)
        
        return result
    
    def _convert_to_legacy_format(self, symptoms: List[SymptomEntity]) -> List[Dict[str, Any]]:
        """
        转换为兼容现有格式的实体列表
        
        Args:
            symptoms: 症状实体列表
            
        Returns:
            兼容格式的实体列表
        """
        return [
            {
                "type": "symptom",
                "value": s.standard_term,
                "original_text": s.original_text,
                "confidence": s.confidence
            }
            for s in symptoms
        ]

