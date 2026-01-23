"""
结果融合引擎
融合多个引擎的诊断结果
"""

from typing import Dict, Any, List
from .base_engine import BaseEngine


class FusionEngine:
    """结果融合引擎"""
    
    def __init__(self, engines: List[BaseEngine]):
        """
        初始化融合引擎
        
        Args:
            engines: 引擎列表
        """
        self.engines = engines
    
    def fuse_results(
        self,
        symptoms: List[str],
        signs: Dict[str, Any],
        context: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        融合多个引擎的诊断结果
        
        Args:
            symptoms: 症状列表
            signs: 体征信息
            context: 上下文信息
            
        Returns:
            融合后的诊断结果
        """
        # 调用所有引擎
        results = []
        for engine in self.engines:
            try:
                result = engine.diagnose(symptoms, signs, context)
                results.append(result)
            except Exception as e:
                # 记录错误但继续执行其他引擎
                print(f"Engine {engine.get_engine_name()} failed: {e}")
        
        # 融合结果
        fused_result = self._fuse(results)
        
        return fused_result
    
    def _fuse(self, results: List[Dict[str, Any]]) -> Dict[str, Any]:
        """
        融合多个结果
        
        Args:
            results: 引擎结果列表
            
        Returns:
            融合后的结果
        """
        # TODO: 实现融合算法
        # 可以使用加权平均、投票、贝叶斯融合等方法
        
        all_diseases = {}
        
        # 收集所有疾病及其置信度
        for result in results:
            for disease in result.get('diseases', []):
                disease_name = disease.get('name', '')
                confidence = disease.get('confidence', 0.0)
                engine_name = result.get('engine', '')
                
                if disease_name not in all_diseases:
                    all_diseases[disease_name] = {
                        'name': disease_name,
                        'confidences': {},
                        'total_confidence': 0.0
                    }
                
                all_diseases[disease_name]['confidences'][engine_name] = confidence
        
        # 计算融合后的置信度（简单平均）
        fused_diseases = []
        for disease_name, disease_data in all_diseases.items():
            confidences = list(disease_data['confidences'].values())
            avg_confidence = sum(confidences) / len(confidences) if confidences else 0.0
            
            fused_diseases.append({
                'name': disease_name,
                'confidence': avg_confidence,
                'engine_confidences': disease_data['confidences']
            })
        
        # 按置信度排序
        fused_diseases.sort(key=lambda x: x['confidence'], reverse=True)
        
        return {
            'diseases': fused_diseases,
            'engine_count': len(results),
            'fusion_method': 'weighted_average'
        }

