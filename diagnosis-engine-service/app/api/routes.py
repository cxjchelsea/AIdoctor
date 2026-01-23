"""
诊断引擎服务路由
"""
from fastapi import APIRouter, HTTPException
from typing import Dict, Any
import logging
from app.models.request import DiagnosisEngineRequest
from app.models.response import DiagnosisEngineResult
from app.services.diagnosis_service import DiagnosisService
from app.engines.fusion_engine import FusionEngine
from app.classifiers.three_layer_classifier import ThreeLayerClassifier
from app.kg_reasoning_engine import KGReasoningEngine, Neo4jClient
from app.config.settings import settings

router = APIRouter()
logger = logging.getLogger(__name__)

# 初始化服务实例
diagnosis_service = DiagnosisService()
fusion_engine = FusionEngine()
classifier = ThreeLayerClassifier()


@router.post("/engine/diagnose", response_model=Dict[str, Any])
async def diagnose(request: DiagnosisEngineRequest) -> Dict[str, Any]:
    """
    五引擎融合诊断（完整流程）
    
    包括：
    - 五引擎融合诊断
    - 三层分层分类
    - 推理组织
    - 证据分析
    """
    try:
    result = await diagnosis_service.diagnose(request)
        return result
    except Exception as e:
        logger.error(f"诊断失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"诊断失败: {str(e)}")


@router.post("/engine/rule-based", response_model=Dict[str, Any])
async def rule_based_diagnose(request: DiagnosisEngineRequest) -> Dict[str, Any]:
    """规则引擎诊断"""
    try:
    result = await fusion_engine.rule_engine.diagnose(request)
        return result
    except Exception as e:
        logger.error(f"规则引擎诊断失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"规则引擎诊断失败: {str(e)}")


@router.post("/engine/knowledge-graph", response_model=Dict[str, Any])
async def kg_diagnose(request: DiagnosisEngineRequest) -> Dict[str, Any]:
    """知识图谱查询"""
    try:
    result = await fusion_engine.kg_engine.diagnose(request)
        return result
    except Exception as e:
        logger.error(f"知识图谱查询失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"知识图谱查询失败: {str(e)}")


@router.post("/engine/statistical", response_model=Dict[str, Any])
async def statistical_diagnose(request: DiagnosisEngineRequest) -> Dict[str, Any]:
    """统计模型推理"""
    try:
    result = await fusion_engine.statistical_engine.diagnose(request)
        return result
    except Exception as e:
        logger.error(f"统计模型推理失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"统计模型推理失败: {str(e)}")


@router.post("/engine/llm", response_model=Dict[str, Any])
async def llm_diagnose(request: DiagnosisEngineRequest) -> Dict[str, Any]:
    """大模型推理"""
    try:
    result = await fusion_engine.llm_engine.diagnose(request)
        return result
    except Exception as e:
        logger.error(f"大模型推理失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"大模型推理失败: {str(e)}")


@router.post("/engine/differential", response_model=Dict[str, Any])
async def differential_diagnose(request: DiagnosisEngineRequest) -> Dict[str, Any]:
    """鉴别诊断"""
    try:
    result = await fusion_engine.differential_engine.diagnose(request)
        return result
    except Exception as e:
        logger.error(f"鉴别诊断失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"鉴别诊断失败: {str(e)}")


@router.post("/classify/three-layer", response_model=Dict[str, Any])
async def three_layer_classify(request: DiagnosisEngineRequest) -> Dict[str, Any]:
    """
    三层分层分类
    
    将诊断候选集分为三层：
    - 首要假设（1个）
    - 主要备选（1-2个）
    - 必须排除（0-1个）
    """
    try:
        # 先执行融合诊断获取候选集
        fusion_result = await fusion_engine.fuse(request)
        possibilities = fusion_result.get('possibilities', {})
        
        # 执行三层分类
        classification_result = classifier.classify(possibilities)
        
        return {
            "classification": classification_result,
            "source_possibilities": possibilities
        }
    except Exception as e:
        logger.error(f"三层分层分类失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"三层分层分类失败: {str(e)}")


@router.post("/kg/paths/retrieve", response_model=Dict[str, Any])
async def retrieve_paths(request: DiagnosisEngineRequest) -> Dict[str, Any]:
    """
    知识图谱路径检索
    
    从症状检索到疾病的推理路径
    """
    try:
        # 提取症状CUI
        symptom_info = request.symptom_info or {}
        symptoms = symptom_info.get('symptoms', [])
        symptom_cuis = []
        
        if isinstance(symptoms, list):
            for symptom in symptoms:
                if isinstance(symptom, dict):
                    cui = symptom.get('cui', '')
                    if cui:
                        symptom_cuis.append(cui)
                elif isinstance(symptom, str):
                    symptom_cuis.append(symptom)
        
        if not symptom_cuis:
            return {"paths": [], "error": "未提供症状信息"}
        
        # 创建知识图谱推理引擎
        kg_client = Neo4jClient(
            uri=settings.NEO4J_URI,
            user=settings.NEO4J_USER,
            password=settings.NEO4J_PASSWORD
        )
        kg_engine = KGReasoningEngine(kg_client=kg_client)
        
        # 检索路径
        evidence = {
            'symptoms': symptom_cuis,
            'vital_signs': request.vital_signs or {},
            'examination_results': request.examination_results or []
        }
        
        reasoning_result = kg_engine.reasoning(
            symptom_cuis=symptom_cuis,
            evidence=evidence,
            max_hops=settings.PATH_MAX_HOPS,
            max_paths=settings.MAX_DIAGNOSIS_CANDIDATES
        )
        
        return reasoning_result
    except Exception as e:
        logger.error(f"路径检索失败: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"路径检索失败: {str(e)}")
