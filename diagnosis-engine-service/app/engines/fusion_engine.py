"""
结果融合引擎
"""
from typing import Dict, List
import asyncio
import logging
from app.models.request import DiagnosisEngineRequest
from app.engines.rule_engine import RuleEngine
from app.engines.kg_engine import KnowledgeGraphEngine
from app.engines.statistical_engine import StatisticalModelEngine
from app.engines.llm_engine import LLMEngine
from app.engines.differential_engine import DifferentialEngine

logger = logging.getLogger(__name__)


class FusionEngine:
    """结果融合引擎"""
    
    def __init__(self):
        self.rule_engine = RuleEngine()
        self.kg_engine = KnowledgeGraphEngine()
        self.statistical_engine = StatisticalModelEngine()
        self.llm_engine = LLMEngine()
        self.differential_engine = DifferentialEngine()
    
    async def fuse(self, request: DiagnosisEngineRequest) -> Dict:
        """融合五个引擎的结果"""
        logger.info("开始执行五引擎融合诊断")
        
        # 并行执行五个引擎
        results = await asyncio.gather(
            self.rule_engine.diagnose(request),
            self.kg_engine.diagnose(request),
            self.statistical_engine.diagnose(request),
            self.llm_engine.diagnose(request),
            self.differential_engine.diagnose(request),
            return_exceptions=True
        )
        
        # 融合结果
        fused_result = self._fuse_results(results)
        
        logger.info(f"融合完成，结果: {fused_result}")
        return fused_result
    
    def _fuse_results(self, results: List[Dict]) -> Dict:
        """融合五个引擎的结果"""
        from app.config.settings import settings
        
        weights = {
            'rule': settings.ENGINE_WEIGHT_RULE,
            'kg': settings.ENGINE_WEIGHT_KG,
            'statistical': settings.ENGINE_WEIGHT_STATISTICAL,
            'llm': settings.ENGINE_WEIGHT_LLM,
            'differential': settings.ENGINE_WEIGHT_DIFFERENTIAL
        }
        
        # 加权融合
        fused = {}
        engine_types = ['rule', 'kg', 'statistical', 'llm', 'differential']
        
        for engine_type, result in zip(engine_types, results):
            if isinstance(result, Exception):
                logger.warning(f"{engine_type}引擎执行失败: {str(result)}")
                continue
            
            weight = weights[engine_type]
            possibilities = result.get('possibilities', {})
            
            for disease, score in possibilities.items():
                if disease not in fused:
                    fused[disease] = 0
                fused[disease] += score * weight
        
        # 排序，返回Top 5（作为候选，最终展示Top 3-5，与系统设计方案保持一致）
        sorted_diseases = sorted(fused.items(), key=lambda x: x[1], reverse=True)[:5]
        
        return {
            'possibilities': dict(sorted_diseases),
            'engine_results': {
                'rule': results[0] if not isinstance(results[0], Exception) else None,
                'kg': results[1] if not isinstance(results[1], Exception) else None,
                'statistical': results[2] if not isinstance(results[2], Exception) else None,
                'llm': results[3] if not isinstance(results[3], Exception) else None,
                'differential': results[4] if not isinstance(results[4], Exception) else None
            }
        }
