"""
专业大模型分析引擎
使用公共LLM库和知识图谱路径注入
"""
from typing import Dict, List, Optional
import logging
from app.models.request import DiagnosisEngineRequest
from app.utils.llm_client import LangChainLLMClient, LLMConfig, LLMBackend
from app.utils.prompt_templates import PromptTemplateManager
from app.kg_reasoning_engine import PathInjector, PathRetriever, Neo4jClient
from app.config.settings import settings

logger = logging.getLogger(__name__)


class LLMEngine:
    """专业大模型分析引擎（使用公共LLM库）"""
    
    def __init__(
        self,
        llm_client: Optional[LangChainLLMClient] = None,
        template_manager: Optional[PromptTemplateManager] = None,
        path_injector: Optional[PathInjector] = None,
        path_retriever: Optional[PathRetriever] = None
    ):
        """
        初始化大模型引擎
        
        Args:
            llm_client: LLM客户端，如果为None则自动创建
            template_manager: 模板管理器，如果为None则自动创建
            path_injector: 路径注入器
            path_retriever: 路径检索器
        """
        # 创建LLM配置
        llm_config = LLMConfig(
            backend=LLMBackend(settings.LLM_BACKEND),
            model=settings.LLM_MODEL,
            temperature=settings.LLM_TEMPERATURE,
            max_tokens=settings.LLM_MAX_TOKENS,
            timeout=settings.LLM_TIMEOUT,
            max_retries=settings.LLM_MAX_RETRIES,
            openai_api_key=settings.OPENAI_API_KEY,
            openai_base_url=settings.OPENAI_BASE_URL,
            chatglm_api_url=settings.CHATGLM_API_URL,
            chatglm_api_key=settings.CHATGLM_API_KEY,
            ollama_base_url=settings.OLLAMA_BASE_URL,
            ollama_model=settings.OLLAMA_MODEL,
            custom_api_url=settings.CUSTOM_API_URL,
            custom_api_key=settings.CUSTOM_API_KEY
        )
        
        self.llm_client = llm_client or LangChainLLMClient(config=llm_config)
        self.template_manager = template_manager or PromptTemplateManager()
        self.path_injector = path_injector or PathInjector()
        
        # 如果提供了路径检索器，则使用；否则尝试创建
        if path_retriever:
            self.path_retriever = path_retriever
        else:
            try:
                kg_client = Neo4jClient(
                    uri=settings.NEO4J_URI,
                    user=settings.NEO4J_USER,
                    password=settings.NEO4J_PASSWORD,
                    max_connection_lifetime=settings.NEO4J_MAX_CONNECTION_LIFETIME,
                    max_connection_pool_size=settings.NEO4J_MAX_CONNECTION_POOL_SIZE,
                    connection_acquisition_timeout=settings.NEO4J_CONNECTION_TIMEOUT
                )
                self.path_retriever = PathRetriever(kg_client)
            except Exception as e:
                logger.warning(f"无法创建路径检索器: {str(e)}")
                self.path_retriever = None
        
        logger.info(f"大模型引擎初始化完成（使用{settings.LLM_BACKEND}后端）")
    
    async def diagnose(self, request: DiagnosisEngineRequest) -> Dict:
        """
        大模型推理（使用公共LLM库和路径注入）
        
        Args:
            request: 诊断请求
            
        Returns:
            诊断结果
        """
        try:
            # 1. 提取症状信息
            symptom_info = request.symptom_info or {}
            symptoms = symptom_info.get('symptoms', [])
            
            # 提取症状列表（用于路径检索）
            symptom_list = []
            symptom_cuis = []
            if isinstance(symptoms, list):
                for symptom in symptoms:
                    if isinstance(symptom, dict):
                        symptom_list.append(symptom.get('name', ''))
                        cui = symptom.get('cui', '')
                        if cui:
                            symptom_cuis.append(cui)
                    elif isinstance(symptom, str):
                        symptom_list.append(symptom)
                        symptom_cuis.append(symptom)
            
            # 2. 检索知识图谱路径（如果可用）
            paths = []
            if self.path_retriever and symptom_cuis:
                try:
                    paths = self.path_retriever.retrieve_disease_paths(
                        symptoms=symptom_cuis,
                        max_hops=settings.PATH_MAX_HOPS
                    )
                except Exception as e:
                    logger.warning(f"路径检索失败: {str(e)}")
            
            # 3. 构建提示词（使用模板管理器）
            context = {
                'symptoms': symptom_list,
                'vital_signs': request.vital_signs or {},
                'examination_results': request.examination_results or [],
                'health_profile': request.health_profile or {}
            }
            
            # 格式化路径文本
            paths_text = None
            if paths:
                paths_text = self._format_paths(paths[:5])  # 最多5条路径
            
            # 使用模板管理器构建提示词
            prompt = self.template_manager.format_diagnosis_reasoning(
                symptoms=symptom_list,
                signs=request.vital_signs or {},
                context=context,
                paths=paths_text
            )
            
            # 4. 路径注入（如果可用）
            if paths and self.path_injector:
                prompt = self.path_injector.inject_paths(prompt, paths, max_paths=5)
            
            # 5. 调用LLM
            llm_response = await self.llm_client.generate(prompt)
            
            # 6. 解析结果
            parsed_result = self._parse_response(llm_response)
            
            return {
                'possibilities': parsed_result.get('possibilities', {}),
                'supporting_evidence': parsed_result.get('supporting_evidence', {}),
                'opposing_evidence': parsed_result.get('opposing_evidence', {}),
                'missing_info': parsed_result.get('missing_info', []),
                'recommended_tests': parsed_result.get('recommended_tests', []),
                'engine_type': 'llm',
                'reasoning_paths': paths[:5] if paths else []
            }
        except Exception as e:
            logger.error(f"LLM引擎诊断失败: {str(e)}", exc_info=True)
            return {
                'possibilities': {},
                'engine_type': 'llm',
                'error': str(e)
            }
    
    def _format_paths(self, paths: List[Dict]) -> str:
        """
        格式化路径为文本
        
        Args:
            paths: 路径列表
            
        Returns:
            格式化后的路径文本
        """
        if not paths:
            return ""
        
        formatted = []
        for i, path in enumerate(paths, 1):
            path_desc = path.get('description', f'路径{i}')
            scores = path.get('scores', {})
            
            path_str = f"路径{i}: {path_desc}"
            if scores:
                path_str += f" (先验: {scores.get('prior', 0):.3f}, "
                path_str += f"似然: {scores.get('likelihood', 0):.3f}, "
                path_str += f"后验: {scores.get('posterior', 0):.3f})"
            
            formatted.append(path_str)
        
        return "\n".join(formatted)
    
    def _parse_response(self, response: str) -> Dict:
        """
        解析大模型返回结果
        
        Args:
            response: LLM返回的文本
            
        Returns:
            解析后的结构化结果
        """
        import json
        import re
        
        # 尝试提取JSON
        json_match = re.search(r'\{.*\}', response, re.DOTALL)
        if json_match:
            try:
                result = json.loads(json_match.group())
                return {
                    'possibilities': result.get('possibilities', {}),
                    'supporting_evidence': result.get('supporting_evidence', {}),
                    'opposing_evidence': result.get('opposing_evidence', {}),
                    'missing_info': result.get('missing_info', []),
                    'recommended_tests': result.get('recommended_tests', []),
                    'reasoning': result.get('reasoning', '')
                }
            except Exception as e:
                logger.error(f"解析LLM响应失败: {str(e)}")
        
        # 如果解析失败，返回空结果
        return {
            'possibilities': {},
            'supporting_evidence': {},
            'opposing_evidence': {},
            'missing_info': [],
            'recommended_tests': [],
            'reasoning': response
        }

