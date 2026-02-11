"""
AI诊断入口判定（P0模块）
整合5个步骤的完整流程
"""
from typing import Dict, Any

from app.services.entry_assessment.step1_receive_input import receive_user_input, receive_user_input_async
from app.services.entry_assessment.step2_identify_symptom import identify_symptom
from app.services.entry_assessment.step3_clarification import clarify_direction
from app.services.entry_assessment.step4_red_flag_check import check_red_flags
from app.services.entry_assessment.step5_path_selection import select_path


async def perform_entry_assessment_async(user_input: dict) -> dict:
    """
    执行完整的入口判定流程（Step 1-5）- 异步版本
    
    Args:
        user_input: 用户输入，包含：
            - userInput: str - 用户原始输入文本
            - basicInfo: dict - 基本信息（可选）
            - symptoms: list - 症状列表（可选）
        
    Returns:
        完整的入口判定结果，包含：
            - entry_assessment: dict - 入口判定详细结果
            - path_result: dict - 路径选择结果
    """
    # Step 1: 接收用户输入（使用异步版本）
    # 如果user_input中没有userId，添加默认值（用于临床解析服务调用）
    if "userId" not in user_input:
        user_input["userId"] = "default_user"
    if "sessionId" not in user_input:
        user_input["sessionId"] = f"session_{user_input.get('userId', 'default_user')}"
    
    input_data = await receive_user_input_async(user_input)
    
    # Step 2: 识别症状/困扰
    symptom_data = identify_symptom(input_data)
    
    # Step 3: 方向澄清（仅对"情况C"触发）
    # 传递nlu_result以便更好地利用上下文信息
    direction_data = clarify_direction(symptom_data, nlu_result=input_data)
    
    # Step 4: 危险信号检查（所有用户都要过一次）
    red_flag_data = check_red_flags(symptom_data)
    
    # Step 5: 输出路径结果
    assessment_data = {
        "symptom_data": symptom_data,
        "direction_data": direction_data,
        "red_flag_data": red_flag_data
    }
    path_result = select_path(assessment_data)
    
    # 构建入口判定结果（entry_assessment）
    entry_assessment = {
        "userInput": input_data.get("original_input", ""),
        "hasSymptom": symptom_data.get("status") == "has_symptom",
        "symptomStatus": symptom_data.get("status", "uncertain"),
        "symptoms": symptom_data.get("symptoms", []),
        "concerns": symptom_data.get("concerns", []),
        "clarificationNeeded": symptom_data.get("status") == "uncertain",
        "clarificationResult": direction_data if direction_data else None,
        "redFlagsHit": red_flag_data.get("red_flags_hit", False),
        "redFlagsList": [
            flag.get("description", str(flag)) 
            for flag in red_flag_data.get("red_flags", [])
        ],
        "pathSelected": path_result.get("path", "B")
    }
    
    return {
        "entry_assessment": entry_assessment,
        "path_result": path_result
    }


def perform_entry_assessment(user_input: dict) -> dict:
    """
    执行完整的入口判定流程（Step 1-5）
    
    Args:
        user_input: 用户输入，包含：
            - userInput: str - 用户原始输入文本
            - basicInfo: dict - 基本信息（可选）
            - symptoms: list - 症状列表（可选）
        
    Returns:
        完整的入口判定结果，包含：
            - entry_assessment: dict - 入口判定详细结果
                - userInput: str
                - hasSymptom: bool
                - symptomStatus: str
                - clarificationNeeded: bool
                - clarificationResult: dict | None
                - redFlagsHit: bool
                - redFlagsList: list
                - pathSelected: str
            - path_result: dict - 路径选择结果
    """
    # Step 1: 接收用户输入
    # 如果user_input中没有userId，添加默认值（用于临床解析服务调用）
    if "userId" not in user_input:
        user_input["userId"] = "default_user"
    if "sessionId" not in user_input:
        user_input["sessionId"] = f"session_{user_input.get('userId', 'default_user')}"
    
    input_data = receive_user_input(user_input)
    
    # Step 2: 识别症状/困扰
    symptom_data = identify_symptom(input_data)
    
    # Step 3: 方向澄清（仅对"情况C"触发）
    # 传递nlu_result以便更好地利用上下文信息
    direction_data = clarify_direction(symptom_data, nlu_result=input_data)
    
    # Step 4: 危险信号检查（所有用户都要过一次）
    red_flag_data = check_red_flags(symptom_data)
    
    # Step 5: 输出路径结果
    assessment_data = {
        "symptom_data": symptom_data,
        "direction_data": direction_data,
        "red_flag_data": red_flag_data
    }
    path_result = select_path(assessment_data)
    
    # 构建入口判定结果（entry_assessment）
    entry_assessment = {
        "userInput": input_data.get("original_input", ""),
        "hasSymptom": symptom_data.get("status") == "has_symptom",
        "symptomStatus": symptom_data.get("status", "uncertain"),
        "symptoms": symptom_data.get("symptoms", []),
        "concerns": symptom_data.get("concerns", []),
        "clarificationNeeded": symptom_data.get("status") == "uncertain",
        "clarificationResult": direction_data if direction_data else None,
        "redFlagsHit": red_flag_data.get("red_flags_hit", False),
        "redFlagsList": [
            flag.get("description", str(flag)) 
            for flag in red_flag_data.get("red_flags", [])
        ],
        "pathSelected": path_result.get("path", "B")
    }
    
    return {
        "entry_assessment": entry_assessment,
        "path_result": path_result
    }

