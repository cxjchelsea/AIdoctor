"""
证据链构建器
从CDP中提取证据，构建完整的证据链结构
"""
import logging
from typing import Dict, Any, List, Optional
from app.models.response import Evidence, EvidenceChain
from app.utils.specific_exceptions import EvidenceChainConstructionFailedException

logger = logging.getLogger(__name__)


class EvidenceChainBuilder:
    """证据链构建器"""
    
    def build(self, cdp_data: Dict[str, Any]) -> EvidenceChain:
        """
        构建证据链
        
        Args:
            cdp_data: CDP数据
            
        Returns:
            证据链
        """
        try:
            # 1. 提取证据
            evidence_list = self._extract_evidence(cdp_data)
            
            # 2. 证据分类
            positive_evidence = [e for e in evidence_list if e.get("type") == "positive"]
            negative_evidence = [e for e in evidence_list if e.get("type") == "negative"]
            
            # 3. 构建证据-疾病关联
            evidence_items = []
            ddx = cdp_data.get("ddx", {})
            
            for evidence in evidence_list:
                # 计算证据支持强度
                strength = self._calculate_evidence_strength(evidence, ddx)
                
                # 确定支持的疾病和反对的疾病
                supporting_diseases = self._get_supporting_diseases(evidence, ddx)
                contradicting_diseases = self._get_contradicting_diseases(evidence, ddx)
                
                evidence_items.append(
                    Evidence(
                        item=evidence.get("item", ""),
                        type=evidence.get("type", "symptom"),
                        strength=strength,
                        supportingDiseases=supporting_diseases,
                        contradictingDiseases=contradicting_diseases
                    )
                )
            
            # 4. 生成推理路径文本
            reasoning_paths = self._generate_reasoning_paths(cdp_data)
            
            return EvidenceChain(
                evidence=evidence_items,
                reasoningPaths=reasoning_paths
            )
        except Exception as e:
            logger.error(f"证据链构建失败: {str(e)}", exc_info=True)
            raise EvidenceChainConstructionFailedException(str(e))
    
    def _extract_evidence(self, cdp_data: Dict[str, Any]) -> List[Dict[str, Any]]:
        """
        从CDP中提取证据
        
        Args:
            cdp_data: CDP数据
            
        Returns:
            证据列表
        """
        evidence_list = []
        patient_state = cdp_data.get("patient_state", {})
        
        # 提取症状
        symptoms = patient_state.get("symptoms", [])
        if isinstance(symptoms, list):
            for symptom in symptoms:
                if isinstance(symptom, dict):
                    evidence_list.append({
                        "item": symptom.get("name", str(symptom)),
                        "type": "symptom",
                        "category": "positive",
                        "value": symptom.get("value", True)
                    })
                else:
                    evidence_list.append({
                        "item": str(symptom),
                        "type": "symptom",
                        "category": "positive",
                        "value": True
                    })
        
        # 提取体征
        signs = patient_state.get("signs", {})
        if isinstance(signs, dict):
            for key, value in signs.items():
                if value:
                    evidence_list.append({
                        "item": key,
                        "type": "sign",
                        "category": "positive" if value else "negative",
                        "value": value
                    })
        
        # 提取检查结果
        examination_results = patient_state.get("examination_results", [])
        if isinstance(examination_results, list):
            for result in examination_results:
                if isinstance(result, dict):
                    evidence_list.append({
                        "item": result.get("name", str(result)),
                        "type": "examination",
                        "category": "positive" if result.get("abnormal", False) else "negative",
                        "value": result.get("value")
                    })
        
        # 从evidence_graph中提取已有证据
        evidence_graph = cdp_data.get("evidence_graph", {})
        if isinstance(evidence_graph, dict):
            existing_evidence = evidence_graph.get("evidence", [])
            if isinstance(existing_evidence, list):
                evidence_list.extend(existing_evidence)
        
        return evidence_list
    
    def _calculate_evidence_strength(
        self,
        evidence: Dict[str, Any],
        ddx: Dict[str, Any]
    ) -> str:
        """
        计算证据强度
        
        Args:
            evidence: 证据
            ddx: 鉴别诊断
            
        Returns:
            证据强度（strong/medium/weak）
        """
        # 简单的强度评估逻辑
        evidence_type = evidence.get("type", "")
        category = evidence.get("category", "positive")
        
        if evidence_type == "symptom" and category == "positive":
            return "strong"
        elif evidence_type == "examination" and category == "positive":
            return "strong"
        elif evidence_type == "sign" and category == "positive":
            return "medium"
        else:
            return "weak"
    
    def _get_supporting_diseases(
        self,
        evidence: Dict[str, Any],
        ddx: Dict[str, Any]
    ) -> List[str]:
        """
        获取证据支持的疾病列表
        
        Args:
            evidence: 证据
            ddx: 鉴别诊断
            
        Returns:
            支持的疾病列表
        """
        supporting_diseases = []
        
        # 从DDx中查找相关疾病
        primary_hypothesis = ddx.get("primary_hypothesis", [])
        if isinstance(primary_hypothesis, list):
            for item in primary_hypothesis:
                if isinstance(item, dict):
                    disease_name = item.get("disease", item.get("name", ""))
                    if disease_name:
                        supporting_diseases.append(disease_name)
                elif isinstance(item, str):
                    supporting_diseases.append(item)
        
        major_alternatives = ddx.get("major_alternatives", [])
        if isinstance(major_alternatives, list):
            for item in major_alternatives:
                if isinstance(item, dict):
                    disease_name = item.get("disease", item.get("name", ""))
                    if disease_name and disease_name not in supporting_diseases:
                        supporting_diseases.append(disease_name)
                elif isinstance(item, str) and item not in supporting_diseases:
                    supporting_diseases.append(item)
        
        return supporting_diseases[:3]  # 最多返回3个
    
    def _get_contradicting_diseases(
        self,
        evidence: Dict[str, Any],
        ddx: Dict[str, Any]
    ) -> List[str]:
        """
        获取证据反对的疾病列表
        
        Args:
            evidence: 证据
            ddx: 鉴别诊断
            
        Returns:
            反对的疾病列表
        """
        # 如果证据是阴性证据，则反对相关疾病
        category = evidence.get("category", "positive")
        if category == "negative":
            # 返回所有候选疾病（简化逻辑）
            contradicting_diseases = []
            ddx_all = ddx.get("primary_hypothesis", []) + ddx.get("major_alternatives", [])
            for item in ddx_all:
                if isinstance(item, dict):
                    disease_name = item.get("disease", item.get("name", ""))
                    if disease_name:
                        contradicting_diseases.append(disease_name)
                elif isinstance(item, str):
                    contradicting_diseases.append(item)
            return contradicting_diseases[:2]  # 最多返回2个
        
        return []
    
    def _generate_reasoning_paths(self, cdp_data: Dict[str, Any]) -> List[str]:
        """
        生成推理路径文本
        
        Args:
            cdp_data: CDP数据
            
        Returns:
            推理路径文本列表
        """
        reasoning_paths = []
        
        # 从CDP中获取推理路径
        existing_paths = cdp_data.get("reasoning_paths", [])
        if isinstance(existing_paths, list):
            for path in existing_paths:
                if isinstance(path, dict):
                    description = path.get("description", path.get("path", ""))
                    confidence = path.get("confidence", 0.0)
                    if description:
                        reasoning_paths.append(f"{description}（置信度：{confidence:.2f}）")
                elif isinstance(path, str):
                    reasoning_paths.append(path)
        
        # 如果没有现有路径，从证据和DDx生成简单路径
        if not reasoning_paths:
            patient_state = cdp_data.get("patient_state", {})
            symptoms = patient_state.get("symptoms", [])
            ddx = cdp_data.get("ddx", {})
            
            primary_hypothesis = ddx.get("primary_hypothesis", [])
            if symptoms and primary_hypothesis:
                symptom_names = [s.get("name", str(s)) if isinstance(s, dict) else str(s) for s in symptoms[:2]]
                disease_names = [d.get("disease", d.get("name", str(d))) if isinstance(d, dict) else str(d) for d in primary_hypothesis[:2]]
                
                for symptom in symptom_names:
                    for disease in disease_names:
                        reasoning_paths.append(f"症状：{symptom} → 疾病：{disease}")
        
        return reasoning_paths[:5]  # 最多返回5条路径

