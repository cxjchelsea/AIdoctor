"""
Step 5：输出路径结果并跳转
AI诊断入口判定的第五步

输出规则：
- 无症状 + 无危险信号 → 进入健康筛查路径（A）
- 有症状/困扰 + 无危险信号 → 进入症状诊断路径（B）
- 危险信号命中 → 退出线上流程（安全提示）

跳转规则：
- A路径：进入健康筛查流程的起点（A1｜需求分类）
- B路径：进入症状诊断流程的起点（阶段1｜问诊）
"""
from typing import Dict, Any, Optional
import uuid


def select_path(assessment_data: dict) -> dict:
    """
    选择并输出路径结果
    
    Args:
        assessment_data: 前面步骤的评估数据，包含：
            - symptom_data: Step 2的输出
            - direction_data: Step 3的输出（可选）
            - red_flag_data: Step 4的输出
        
    Returns:
        路径选择结果，包含：
            - path: str - "A" | "B" | "exit"
            - path_name: str - 路径名称
            - next_step: str - 下一步骤
            - cdp_id: str - CDP ID
            - reason: str - 选择原因
            - message: str - 提示信息
    """
    symptom_data = assessment_data.get("symptom_data", {})
    direction_data = assessment_data.get("direction_data")
    red_flag_data = assessment_data.get("red_flag_data", {})
    
    symptom_status = symptom_data.get("status", "")
    red_flags_hit = red_flag_data.get("red_flags_hit", False)
    safety_message = red_flag_data.get("safety_message", "")
    
    # 规则1：如果命中危险信号，退出线上流程
    if red_flags_hit:
        # 即使退出流程，也生成CDP ID用于追踪
        cdp_id = f"cdp_{uuid.uuid4().hex[:12]}"
        return {
            "path": "exit",
            "path_name": "退出线上流程",
            "next_step": "线下就医",
            "cdp_id": cdp_id,
            "reason": "检测到危险信号，建议优先线下就医",
            "message": safety_message
        }
    
    # 规则2：根据症状状态和方向澄清结果选择路径
    if direction_data and direction_data.get("clarified"):
        # 如果已澄清，使用澄清结果
        direction = direction_data.get("direction", "B")
        if direction == "A":
            path = "A"
            path_name = "健康筛查路径"
            next_step = "A1｜需求分类"
            reason = "用户选择健康筛查/体检规划"
        else:
            path = "B"
            path_name = "症状诊断路径"
            next_step = "阶段1｜问诊"
            reason = "用户选择症状咨询/问题排查"
    elif symptom_status == "no_symptom":
        # 情况A：明确无症状 → 进入健康筛查路径（A）
        path = "A"
        path_name = "健康筛查路径"
        next_step = "A1｜需求分类"
        reason = "用户明确无症状，进入健康筛查路径"
    elif symptom_status == "has_symptom":
        # 情况B：存在症状/困扰 → 进入症状诊断路径（B）
        path = "B"
        path_name = "症状诊断路径"
        next_step = "阶段1｜问诊"
        reason = "用户存在症状/困扰，进入症状诊断路径"
    else:
        # 情况C：不确定，默认进入B路径（宁可误报，不能漏报）
        path = "B"
        path_name = "症状诊断路径"
        next_step = "阶段1｜问诊"
        reason = "用户意图不明确，默认进入症状诊断路径（安全优先）"
    
    # 生成CDP ID
    cdp_id = f"cdp_{uuid.uuid4().hex[:12]}"
    
    # 生成提示信息
    if path == "A":
        message = "已为您进入健康筛查路径，我们将帮助您进行健康评估和筛查规划。"
    elif path == "B":
        message = "已为您进入症状诊断路径，我们将通过问诊帮助您分析症状。"
    else:
        message = safety_message
    
    return {
        "path": path,
        "path_name": path_name,
        "next_step": next_step,
        "cdp_id": cdp_id,
        "reason": reason,
        "message": message
    }

