"""
大模型引擎（路径约束）
使用大语言模型进行诊断，但受知识图谱路径约束
集成LangChain进行LLM调用管理
"""

import json
import logging
from typing import Dict, Any, List, Optional
from .base_engine import BaseEngine
from app.kg_reasoning_engine import PathInjector, PathRetriever
from app.utils.llm_client import LangChainLLMClient
from app.utils.prompt_templates import PromptTemplateManager

logger = logging.getLogger(__name__)


class LLMEngine(BaseEngine):
    """大模型引擎（路径约束）"""
    
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
            llm_client: LangChain LLM客户端，如果为None则自动创建
            template_manager: 提示词模板管理器，如果为None则自动创建
            path_injector: 路径注入器
            path_retriever: 路径检索器
        """
        self.llm_client = llm_client or LangChainLLMClient()
        self.template_manager = template_manager or PromptTemplateManager()
        self.path_injector = path_injector or PathInjector()
        self.path_retriever = path_retriever
        
        logger.info("LLM引擎初始化完成（使用LangChain）")
    
    async def diagnose_async(
        self,
        symptoms: List[str],
        signs: Dict[str, Any],
        context: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        基于大模型进行诊断（路径约束）- 异步版本
        
        Args:
            symptoms: 症状列表
            signs: 体征信息
            context: 上下文信息
            
        Returns:
            诊断结果
        """
        try:
            # 1. 检索推理路径
            if self.path_retriever:
                paths = self.path_retriever.retrieve_disease_paths(symptoms)
            else:
                paths = []
            
            # 2. 格式化路径文本
            paths_text = self._format_paths(paths) if paths else None
            
            # 3. 使用模板管理器构建提示词
            prompt = self.template_manager.format_diagnosis_reasoning(
                symptoms=symptoms,
                signs=signs,
                context=context,
                paths=paths_text
            )
            
            # 4. 如果路径注入器需要，可以进一步处理
            if paths and self.path_injector:
                prompt = self.path_injector.inject_paths(prompt, paths)
            
            # 5. 调用LLM
            llm_result = await self.llm_client.generate(prompt)
            
            # 6. 解析结果
            parsed_result = self._parse_llm_result(llm_result)
            
            return {
                'diseases': parsed_result.get('diseases', []),
                'confidence': parsed_result.get('confidence', 0.0),
                'engine': self.get_engine_name(),
                'reasoning': parsed_result.get('reasoning', ''),
                'raw_result': llm_result
            }
        except Exception as e:
            logger.error(f"LLM引擎诊断失败: {str(e)}", exc_info=True)
            return {
                'diseases': [],
                'confidence': 0.0,
                'engine': self.get_engine_name(),
                'error': str(e)
            }
    
    def diagnose(
        self,
        symptoms: List[str],
        signs: Dict[str, Any],
        context: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        基于大模型进行诊断（路径约束）- 同步版本
        
        Args:
            symptoms: 症状列表
            signs: 体征信息
            context: 上下文信息
            
        Returns:
            诊断结果
        """
        import asyncio
        try:
            loop = asyncio.get_event_loop()
        except RuntimeError:
            loop = asyncio.new_event_loop()
            asyncio.set_event_loop(loop)
        
        return loop.run_until_complete(
            self.diagnose_async(symptoms, signs, context)
        )
    
    def _format_paths(self, paths: List[Dict[str, Any]]) -> str:
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
        for i, path in enumerate(paths[:5], 1):  # 最多5条路径
            path_desc = path.get('description', f'路径{i}')
            scores = path.get('scores', {})
            
            path_str = f"路径{i}: {path_desc}"
            if scores:
                path_str += f" (先验: {scores.get('prior', 0):.3f}, "
                path_str += f"似然: {scores.get('likelihood', 0):.3f}, "
                path_str += f"后验: {scores.get('posterior', 0):.3f})"
            
            formatted.append(path_str)
        
        return "\n".join(formatted)
    
    def _parse_llm_result(self, result: str) -> Dict[str, Any]:
        """
        解析LLM结果
        
        Args:
            result: LLM返回的文本
            
        Returns:
            解析后的结构化结果
        """
        try:
            # 尝试解析JSON格式的结果
            # LLM可能返回JSON代码块，需要提取
            if "```json" in result:
                json_start = result.find("```json") + 7
                json_end = result.find("```", json_start)
                json_str = result[json_start:json_end].strip()
            elif "```" in result:
                json_start = result.find("```") + 3
                json_end = result.find("```", json_start)
                json_str = result[json_start:json_end].strip()
            else:
                json_str = result.strip()
            
            # 尝试解析JSON
            parsed = json.loads(json_str)
            
            # 提取疾病列表
            diseases = []
            
            # 首要假设
            if 'primary_diagnosis' in parsed:
                primary = parsed['primary_diagnosis']
                diseases.append({
                    'disease': primary.get('disease', ''),
                    'confidence': primary.get('confidence', 0.0),
                    'type': 'primary',
                    'supporting_evidence': primary.get('supporting_evidence', []),
                    'contradicting_evidence': primary.get('contradicting_evidence', [])
                })
            
            # 备选诊断
            if 'alternative_diagnoses' in parsed:
                for alt in parsed['alternative_diagnoses']:
                    diseases.append({
                        'disease': alt.get('disease', ''),
                        'confidence': alt.get('confidence', 0.0),
                        'type': 'alternative',
                        'supporting_evidence': alt.get('supporting_evidence', [])
                    })
            
            # 必须排除的诊断
            if 'critical_exclusions' in parsed:
                for excl in parsed['critical_exclusions']:
                    diseases.append({
                        'disease': excl.get('disease', ''),
                        'confidence': excl.get('confidence', 0.0),
                        'type': 'exclusion',
                        'reason': excl.get('reason', '')
                    })
            
            # 计算平均置信度
            if diseases:
                confidence = sum(d.get('confidence', 0.0) for d in diseases) / len(diseases)
            else:
                confidence = 0.0
            
            return {
                'diseases': diseases,
                'confidence': confidence,
                'reasoning': parsed.get('reasoning', '')
            }
        except json.JSONDecodeError:
            # 如果不是JSON格式，尝试简单解析
            logger.warning("LLM返回结果不是JSON格式，尝试简单解析")
            return {
                'diseases': [],
                'confidence': 0.0,
                'reasoning': result,
                'raw_text': result
            }
        except Exception as e:
            logger.error(f"解析LLM结果失败: {str(e)}", exc_info=True)
            return {
                'diseases': [],
                'confidence': 0.0,
                'error': str(e),
                'raw_text': result
            }
    
    def get_engine_name(self) -> str:
        """获取引擎名称"""
        return "llm_engine"

