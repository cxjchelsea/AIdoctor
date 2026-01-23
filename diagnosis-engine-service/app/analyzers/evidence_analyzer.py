"""
证据分析器
分析证据强度，构建证据链
"""
from typing import Dict, List, Optional
import logging

logger = logging.getLogger(__name__)


class EvidenceAnalyzer:
    """证据分析器"""
    
    def __init__(self):
        """初始化证据分析器"""
        logger.info("证据分析器初始化完成")
    
    def analyze(
        self,
        possibilities: Dict[str, float],
        engine_results: Optional[Dict] = None,
        classification_result: Optional[Dict] = None
    ) -> Dict:
        """
        分析证据强度和构建证据链
        
        Args:
            possibilities: 疾病可能性字典
            engine_results: 各引擎结果（可选）
            classification_result: 分类结果（可选）
            
        Returns:
            证据分析结果
        """
        logger.info("开始证据分析")
        
        if not possibilities:
            return {
                "evidence_chains": [],
                "evidence_strength": {}
            }
        
        # 1. 为每个疾病构建证据链
        evidence_chains = []
        evidence_strength = {}
        
        for disease, confidence in possibilities.items():
            chain = self._build_evidence_chain(
                disease=disease,
                confidence=confidence,
                engine_results=engine_results
            )
            evidence_chains.append(chain)
            evidence_strength[disease] = chain.get('strength', 'medium')
        
        # 2. 按证据强度排序
        evidence_chains.sort(
            key=lambda x: self._strength_to_score(x.get('strength', 'medium')),
            reverse=True
        )
        
        return {
            "evidence_chains": evidence_chains,
            "evidence_strength": evidence_strength,
            "summary": self._generate_evidence_summary(evidence_chains)
        }
    
    def _build_evidence_chain(
        self,
        disease: str,
        confidence: float,
        engine_results: Optional[Dict] = None
    ) -> Dict:
        """
        构建单个疾病的证据链
        
        Args:
            disease: 疾病名称
            confidence: 置信度
            engine_results: 各引擎结果
            
        Returns:
            证据链
        """
        supporting_evidence = []
        opposing_evidence = []
        
        # 从各引擎结果中提取证据
        if engine_results:
            # 从规则引擎提取
            rule_result = engine_results.get('rule', {})
            if rule_result and disease in rule_result.get('possibilities', {}):
                supporting_evidence.append({
                    "source": "rule_engine",
                    "type": "rule_match",
                    "description": "规则匹配"
                })
            
            # 从知识图谱引擎提取
            kg_result = engine_results.get('kg', {})
            if kg_result and disease in kg_result.get('possibilities', {}):
                reasoning_paths = kg_result.get('reasoning_paths', [])
                if reasoning_paths:
                    supporting_evidence.append({
                        "source": "knowledge_graph",
                        "type": "reasoning_path",
                        "description": f"知识图谱推理路径（{len(reasoning_paths)}条）",
                        "paths": reasoning_paths[:3]  # 只保留前3条
                    })
            
            # 从LLM引擎提取
            llm_result = engine_results.get('llm', {})
            if llm_result:
                supporting = llm_result.get('supporting_evidence', {}).get(disease, [])
                opposing = llm_result.get('opposing_evidence', {}).get(disease, [])
                
                if supporting:
                    supporting_evidence.append({
                        "source": "llm_engine",
                        "type": "supporting_evidence",
                        "description": f"LLM支持证据（{len(supporting)}项）",
                        "items": supporting
                    })
                
                if opposing:
                    opposing_evidence.append({
                        "source": "llm_engine",
                        "type": "opposing_evidence",
                        "description": f"LLM反对证据（{len(opposing)}项）",
                        "items": opposing
                    })
        
        # 计算证据强度
        strength = self._calculate_evidence_strength(
            confidence=confidence,
            supporting_count=len(supporting_evidence),
            opposing_count=len(opposing_evidence)
        )
        
        return {
            "disease": disease,
            "confidence": confidence,
            "supporting_evidence": supporting_evidence,
            "opposing_evidence": opposing_evidence,
            "strength": strength,
            "evidence_count": len(supporting_evidence) + len(opposing_evidence)
        }
    
    def _calculate_evidence_strength(
        self,
        confidence: float,
        supporting_count: int,
        opposing_count: int
    ) -> str:
        """
        计算证据强度
        
        Args:
            confidence: 置信度
            supporting_count: 支持证据数量
            opposing_count: 反对证据数量
            
        Returns:
            证据强度（strong/medium/weak）
        """
        # 综合考虑置信度和证据数量
        if confidence >= 0.7 and supporting_count >= 2 and opposing_count == 0:
            return "strong"
        elif confidence >= 0.5 and supporting_count >= 1:
            return "medium"
        else:
            return "weak"
    
    def _strength_to_score(self, strength: str) -> float:
        """将证据强度转换为分数"""
        strength_map = {
            "strong": 3.0,
            "medium": 2.0,
            "weak": 1.0
        }
        return strength_map.get(strength, 1.0)
    
    def _generate_evidence_summary(self, evidence_chains: List[Dict]) -> Dict:
        """
        生成证据摘要
        
        Args:
            evidence_chains: 证据链列表
            
        Returns:
            证据摘要
        """
        strong_count = sum(1 for chain in evidence_chains if chain.get('strength') == 'strong')
        medium_count = sum(1 for chain in evidence_chains if chain.get('strength') == 'medium')
        weak_count = sum(1 for chain in evidence_chains if chain.get('strength') == 'weak')
        
        return {
            "total_diseases": len(evidence_chains),
            "strong_evidence": strong_count,
            "medium_evidence": medium_count,
            "weak_evidence": weak_count
        }

