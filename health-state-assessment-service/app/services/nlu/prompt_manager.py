"""
Prompt管理器：管理NLU相关的Prompt模板
"""
from typing import Dict, Any
from app.utils.logger import logger


class PromptManager:
    """Prompt管理器"""
    
    # 意图识别Prompt模板
    INTENT_RECOGNITION_PROMPT = """你是一个医疗AI助手，需要识别用户的医疗咨询意图。

用户输入：{user_input}

请分析用户的意图，判断用户是想要：
1. **健康筛查**（screening）：用户想要进行体检、健康评估、预防性检查等，没有明确的症状描述
2. **症状诊断**（diagnosis）：用户有明确的症状、不适或困扰，想要了解可能的原因或诊断
3. **混合诉求**（mixed）：用户既有筛查需求，又有轻微症状描述
4. **不确定**（unknown）：无法明确判断用户意图

请以JSON格式返回结果：
{{
    "intent": "screening|diagnosis|mixed|unknown",
    "confidence": 0.0-1.0,
    "reasoning": "识别理由",
    "has_screening_intent": true/false,
    "has_symptom_intent": true/false,
    "screening_keywords": ["关键词列表"],
    "symptom_keywords": ["关键词列表"]
}}"""

    # 实体提取Prompt模板
    ENTITY_EXTRACTION_PROMPT = """你是一个医疗AI助手，需要从用户输入中提取结构化的医疗信息。

用户输入：{user_input}
用户意图：{intent}

请提取以下信息：

1. **症状列表**：识别用户描述的所有症状，包括：
   - 症状名称（使用标准医学术语）
   - 症状位置（如：胸部、腹部、头部等）
   - 症状程度（轻度/中度/重度）
   - 症状持续时间（如：3天、1周等）
   - 症状频率（如：持续、间歇、偶尔等）

2. **基本信息**（如果提及）：
   - 年龄
   - 性别
   - BMI或体重信息

3. **生命体征**（如果提及）：
   - 血压（收缩压/舒张压）
   - 心率
   - 体温
   - 其他生命体征

4. **时间信息**：
   - 症状开始时间
   - 症状持续时间
   - 症状频率

请以JSON格式返回结果：
{{
    "symptoms": [
        {{
            "original_text": "原始文本",
            "standard_term": "标准术语",
            "location": "位置",
            "severity": "轻度|中度|重度",
            "duration": "持续时间",
            "frequency": "频率"
        }}
    ],
    "basic_info": {{
        "age": 年龄或null,
        "gender": "性别"或null,
        "bmi": BMI或null
    }},
    "vital_signs": {{
        "bp": {{"systolic": 收缩压, "diastolic": 舒张压}}或null,
        "heart_rate": 心率或null,
        "temperature": 体温或null
    }},
    "temporal_info": {{
        "start_time": "开始时间",
        "duration": "持续时间",
        "frequency": "频率"
    }}
}}"""

    @classmethod
    def get_intent_prompt(cls, user_input: str) -> str:
        """
        获取意图识别Prompt
        
        Args:
            user_input: 用户输入文本
            
        Returns:
            填充后的Prompt
        """
        return cls.INTENT_RECOGNITION_PROMPT.format(user_input=user_input)
    
    @classmethod
    def get_entity_extraction_prompt(cls, user_input: str, intent: str) -> str:
        """
        获取实体提取Prompt
        
        Args:
            user_input: 用户输入文本
            intent: 用户意图
            
        Returns:
            填充后的Prompt
        """
        return cls.ENTITY_EXTRACTION_PROMPT.format(
            user_input=user_input,
            intent=intent
        )

