"""
严重程度判定规则（0.1）
定义症状严重程度的判定规则
"""

# 严重程度判定规则
SEVERITY_RULES = {
    "mild": {
        "threshold": 0.3,
        "description": "轻度症状"
    },
    "moderate": {
        "threshold": 0.6,
        "description": "中度症状"
    },
    "severe": {
        "threshold": 1.0,
        "description": "重度症状"
    }
}


def get_severity_level(score: float) -> str:
    """
    根据分数获取严重程度级别
    
    Args:
        score: 严重程度分数（0-1）
        
    Returns:
        严重程度级别
    """
    if score >= SEVERITY_RULES["severe"]["threshold"]:
        return "severe"
    elif score >= SEVERITY_RULES["moderate"]["threshold"]:
        return "moderate"
    else:
        return "mild"

