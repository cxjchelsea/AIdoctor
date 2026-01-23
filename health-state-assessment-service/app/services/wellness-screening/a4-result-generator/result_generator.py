"""
统一结果生成器
"""
from typing import Dict, Any
import logging

logger = logging.getLogger(__name__)


class ResultGenerator:
    """统一结果生成器"""
    
    def generate(self, branch_results: Dict[str, Any]) -> Dict[str, Any]:
        """
        生成统一结果
        
        Args:
            branch_results: 分支执行结果
            
        Returns:
            统一结果
        """
        logger.info(f"生成统一结果: branch_results={branch_results}")
        
        # 简化实现：整合分支执行结果
        branch = branch_results.get("branch", "unknown")
        result = {
            "demand_type": branch,
            "branch_result": branch_results,
            "status": "completed"
        }
        
        # 生成摘要
        if branch == "screening":
            recommendations = branch_results.get("screening_recommendations", [])
            summary = f"根据您的健康画像，建议进行以下筛查：{', '.join(recommendations)}"
        elif branch == "goal_management":
            goals = branch_results.get("health_goals", [])
            summary = f"为您制定了以下健康目标：{', '.join(goals)}"
        else:
            summary = "健康筛查流程已完成"
        
        logger.info(f"统一结果生成完成: summary={summary}")
        
        return {
            "result": result,
            "summary": summary
        }

