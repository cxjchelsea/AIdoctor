"""
推理路径可视化器
可视化DR.KNOWS推理路径
"""
import logging
from typing import Dict, Any, List, Optional
from app.utils.specific_exceptions import ReasoningPathVisualizationFailedException

logger = logging.getLogger(__name__)


class PathVisualizer:
    """推理路径可视化器"""
    
    def visualize(self, cdp_data: Dict[str, Any]) -> List[Dict[str, Any]]:
        """
        可视化推理路径
        
        Args:
            cdp_data: CDP数据
            
        Returns:
            可视化路径列表
        """
        try:
            # 1. 获取推理路径
            reasoning_paths = cdp_data.get("reasoning_paths", [])
            
            # 2. 格式化路径
            formatted_paths = []
            for idx, path in enumerate(reasoning_paths):
                formatted_path = self._format_path(path, idx)
                if formatted_path:
                    formatted_paths.append(formatted_path)
            
            # 3. 如果没有现有路径，生成默认路径
            if not formatted_paths:
                formatted_paths = self._generate_default_paths(cdp_data)
            
            return formatted_paths
        except Exception as e:
            logger.error(f"推理路径可视化失败: {str(e)}", exc_info=True)
            raise ReasoningPathVisualizationFailedException(str(e))
    
    def _format_path(self, path: Any, index: int) -> Optional[Dict[str, Any]]:
        """
        格式化单个推理路径
        
        Args:
            path: 路径数据
            index: 路径索引
            
        Returns:
            格式化后的路径
        """
        if isinstance(path, dict):
            path_id = path.get("pathId", f"path_{index:03d}")
            description = path.get("description", path.get("path", ""))
            confidence = path.get("confidence", path.get("score", 0.0))
            
            # 提取路径节点
            nodes = path.get("nodes", [])
            edges = path.get("edges", [])
            
            return {
                "pathId": path_id,
                "description": description or f"推理路径 {index + 1}",
                "confidence": float(confidence),
                "nodes": nodes if isinstance(nodes, list) else [],
                "edges": edges if isinstance(edges, list) else []
            }
        elif isinstance(path, str):
            return {
                "pathId": f"path_{index:03d}",
                "description": path,
                "confidence": 0.0,
                "nodes": [],
                "edges": []
            }
        
        return None
    
    def _generate_default_paths(self, cdp_data: Dict[str, Any]) -> List[Dict[str, Any]]:
        """
        生成默认推理路径
        
        Args:
            cdp_data: CDP数据
            
        Returns:
            默认路径列表
        """
        paths = []
        patient_state = cdp_data.get("patient_state", {})
        ddx = cdp_data.get("ddx", {})
        
        # 从症状到诊断的路径
        symptoms = patient_state.get("symptoms", [])
        primary_hypothesis = ddx.get("primary_hypothesis", [])
        
        if symptoms and primary_hypothesis:
            symptom_list = symptoms[:3] if isinstance(symptoms, list) else []
            disease_list = primary_hypothesis[:2] if isinstance(primary_hypothesis, list) else []
            
            for idx, symptom in enumerate(symptom_list):
                symptom_name = symptom.get("name", str(symptom)) if isinstance(symptom, dict) else str(symptom)
                
                for disease in disease_list:
                    disease_name = disease.get("disease", disease.get("name", str(disease))) if isinstance(disease, dict) else str(disease)
                    confidence = disease.get("confidence", disease.get("score", 0.8)) if isinstance(disease, dict) else 0.8
                    
                    paths.append({
                        "pathId": f"path_{len(paths):03d}",
                        "description": f"{symptom_name} → {disease_name}",
                        "confidence": float(confidence) if isinstance(confidence, (int, float)) else 0.8,
                        "nodes": [
                            {"type": "symptom", "name": symptom_name},
                            {"type": "disease", "name": disease_name}
                        ],
                        "edges": [
                            {"from": "symptom", "to": "disease", "weight": float(confidence) if isinstance(confidence, (int, float)) else 0.8}
                        ]
                    })
        
        return paths[:5]  # 最多返回5条路径

