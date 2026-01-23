"""
诊断服务
整合五引擎融合诊断、三层分层分类、推理组织、证据分析等功能
"""
from typing import Dict, List, Optional
import logging
from app.engines.fusion_engine import FusionEngine
from app.models.request import DiagnosisEngineRequest
from app.classifiers.three_layer_classifier import ThreeLayerClassifier
from app.analyzers.reasoning_organizer import ReasoningOrganizer
from app.analyzers.evidence_analyzer import EvidenceAnalyzer
from app.config.settings import settings

logger = logging.getLogger(__name__)


class DiagnosisService:
    """诊断服务"""
    
    def __init__(self):
        """初始化诊断服务"""
        # 初始化融合引擎（融合引擎内部会初始化五个子引擎）
        self.fusion_engine = FusionEngine()
        
        # 初始化三层分层分类器
        self.classifier = ThreeLayerClassifier()
        
        # 初始化推理组织器
        self.reasoning_organizer = ReasoningOrganizer()
        
        # 初始化证据分析器
        self.evidence_analyzer = EvidenceAnalyzer()
        
        logger.info("诊断服务初始化完成")
    
    async def diagnose(self, request: DiagnosisEngineRequest) -> Dict:
        """
        执行完整诊断分析流程
        
        流程：
        1. 五引擎融合诊断
        2. 三层分层分类
        3. 推理组织（可选）
        4. 证据分析（可选）
        
        Args:
            request: 诊断请求
            
        Returns:
            完整诊断结果
        """
        logger.info("开始执行诊断分析")
        
        try:
            # 1. 五引擎融合诊断
            fusion_result = await self.fusion_engine.fuse(request)
            possibilities = fusion_result.get('possibilities', {})
            engine_results = fusion_result.get('engine_results', {})
            
            # 2. 三层分层分类
            classification_result = self.classifier.classify(possibilities)
            
            # 3. 推理组织（可选）
            reasoning_groups = None
            try:
                reasoning_groups = self.reasoning_organizer.organize(
                    possibilities=possibilities,
                    engine_results=engine_results
                )
            except Exception as e:
                logger.warning(f"推理组织失败: {str(e)}")
            
            # 4. 证据分析（可选）
            evidence_analysis = None
            try:
                evidence_analysis = self.evidence_analyzer.analyze(
                    possibilities=possibilities,
                    engine_results=engine_results,
                    classification_result=classification_result
                )
            except Exception as e:
                logger.warning(f"证据分析失败: {str(e)}")
            
            # 5. 构建最终结果
            result = {
                'possibilities': possibilities,
                'classification': classification_result,
                'engine_results': engine_results,
                'reasoning_groups': reasoning_groups,
                'evidence_analysis': evidence_analysis,
                'metadata': {
                    'total_candidates': len(possibilities),
                    'max_confidence': max(possibilities.values()) if possibilities else 0.0
                }
            }
            
            logger.info(f"诊断分析完成，候选疾病数: {len(possibilities)}")
        return result
            
        except Exception as e:
            logger.error(f"诊断分析失败: {str(e)}", exc_info=True)
            return {
                'possibilities': {},
                'error': str(e),
                'classification': {
                    'primary_hypothesis': None,
                    'main_alternatives': [],
                    'must_exclude': None
                }
            }
