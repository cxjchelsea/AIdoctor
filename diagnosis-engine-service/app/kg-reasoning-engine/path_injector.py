"""
路径注入LLM
将知识图谱推理路径注入到大语言模型中，用于增强推理
"""

from typing import List, Dict, Any, Optional


class PathInjector:
    """路径注入LLM"""
    
    def __init__(self, llm_client=None):
        """
        初始化路径注入器
        
        Args:
            llm_client: 大语言模型客户端
        """
        self.llm_client = llm_client
    
    def inject_paths(
        self,
        prompt: str,
        paths: List[Dict[str, Any]],
        max_paths: int = 5
    ) -> str:
        """
        将推理路径注入到提示词中
        
        Args:
            prompt: 原始提示词
            paths: 推理路径列表
            max_paths: 最大注入路径数
            
        Returns:
            增强后的提示词
        """
        # 选择评分最高的路径
        sorted_paths = sorted(
            paths,
            key=lambda p: p.get('scores', {}).get('posterior', 0.0),
            reverse=True
        )[:max_paths]
        
        # 格式化路径信息
        path_text = self._format_paths(sorted_paths)
        
        # 构建增强提示词
        enhanced_prompt = f"""{prompt}

基于以下知识图谱推理路径进行诊断：
{path_text}
"""
        return enhanced_prompt
    
    def _format_paths(self, paths: List[Dict[str, Any]]) -> str:
        """
        格式化路径信息为文本
        
        Args:
            paths: 路径列表
            
        Returns:
            格式化后的路径文本
        """
        formatted = []
        for i, path in enumerate(paths, 1):
            path_str = f"路径{i}: {path.get('description', 'N/A')}"
            if 'scores' in path:
                scores = path['scores']
                path_str += f" (先验: {scores.get('prior', 0):.3f}, "
                path_str += f"似然: {scores.get('likelihood', 0):.3f}, "
                path_str += f"后验: {scores.get('posterior', 0):.3f})"
            formatted.append(path_str)
        
        return "\n".join(formatted)

