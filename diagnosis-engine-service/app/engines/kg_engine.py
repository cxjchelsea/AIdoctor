"""
深度知识图谱推理引擎
"""
from typing import Dict, List, Optional
import logging
from app.models.request import DiagnosisEngineRequest
from app.kg_reasoning_engine import KGReasoningEngine, Neo4jClient
from app.config.settings import settings

logger = logging.getLogger(__name__)


class KnowledgeGraphEngine:
    """深度知识图谱推理引擎"""
    
    def __init__(self, kg_reasoning_engine: Optional[KGReasoningEngine] = None):
        """
        初始化知识图谱引擎
        
        Args:
            kg_reasoning_engine: 知识图谱推理引擎实例，如果为None则自动创建
        """
        if kg_reasoning_engine:
            self.kg_reasoning_engine = kg_reasoning_engine
        else:
            # 自动创建Neo4j客户端和推理引擎
            try:
                kg_client = Neo4jClient(
                    uri=settings.NEO4J_URI,
                    user=settings.NEO4J_USER,
                    password=settings.NEO4J_PASSWORD,
                    max_connection_lifetime=settings.NEO4J_MAX_CONNECTION_LIFETIME,
                    max_connection_pool_size=settings.NEO4J_MAX_CONNECTION_POOL_SIZE,
                    connection_acquisition_timeout=settings.NEO4J_CONNECTION_TIMEOUT
                )
                self.kg_reasoning_engine = KGReasoningEngine(kg_client=kg_client)
                logger.info("知识图谱引擎初始化完成（使用Neo4j）")
            except Exception as e:
                logger.warning(f"Neo4j连接失败，知识图谱引擎将返回空结果: {str(e)}")
                self.kg_reasoning_engine = None
    
    async def diagnose(self, request: DiagnosisEngineRequest) -> Dict:
        """
        知识图谱查询
        
        Args:
            request: 诊断请求
            
        Returns:
            诊断结果
        """
        if not self.kg_reasoning_engine:
            return {
                'possibilities': {},
                'engine_type': 'knowledge_graph',
                'error': '知识图谱引擎未初始化'
            }
        
        # 提取症状信息
        symptom_info = request.symptom_info or {}
        symptoms = symptom_info.get('symptoms', [])
        
        if not symptoms:
            return {
                'possibilities': {},
                'engine_type': 'knowledge_graph'
            }
        
        # 提取症状CUI编码
        symptom_cuis = []
        if isinstance(symptoms, list):
            for symptom in symptoms:
                if isinstance(symptom, dict):
                    cui = symptom.get('cui') or symptom.get('name', '')
                    symptom_cuis.append(cui)
                elif isinstance(symptom, str):
                    symptom_cuis.append(symptom)
        
        if not symptom_cuis:
            return {
                'possibilities': {},
                'engine_type': 'knowledge_graph'
            }
        
        # 构建证据信息
        evidence = {
            'symptoms': symptom_cuis,
            'vital_signs': request.vital_signs or {},
            'examination_results': request.examination_results or [],
            'health_profile': request.health_profile or {}
        }
        
        # 执行推理
        try:
            reasoning_result = self.kg_reasoning_engine.reasoning(
                symptom_cuis=symptom_cuis,
                evidence=evidence,
                max_hops=settings.PATH_MAX_HOPS,
                max_paths=settings.MAX_DIAGNOSIS_CANDIDATES
            )
            
            # 从推理结果中提取疾病和置信度
            possibilities = {}
            top_paths = reasoning_result.get('top_paths', [])
            
            for path in top_paths:
                disease_name = path.get('disease_name')
                if disease_name:
                    # 使用后验概率作为置信度
                    posterior = path.get('scores', {}).get('posterior', 0.0)
                    if disease_name not in possibilities:
                        possibilities[disease_name] = 0.0
                    # 取最大置信度
                    possibilities[disease_name] = max(possibilities[disease_name], posterior)
            
            return {
                'possibilities': possibilities,
                'engine_type': 'knowledge_graph',
                'reasoning_paths': top_paths,
                'statistics': reasoning_result.get('statistics', {})
            }
        except Exception as e:
            logger.error(f"知识图谱推理失败: {str(e)}", exc_info=True)
            return {
                'possibilities': {},
                'engine_type': 'knowledge_graph',
                'error': str(e)
            }

