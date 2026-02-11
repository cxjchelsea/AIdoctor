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
    
    # 症状/困扰识别Prompt模板
    SYMPTOM_CONCERN_IDENTIFICATION_PROMPT = """你是一个医疗AI助手，需要从用户输入中识别症状和困扰。

用户输入：{user_input}
NLU结果：
- 意图：{intent}
- 症状实体：{symptoms}
- 上下文信息：{context}

请分析：
1. **症状识别**：
   - 识别所有症状（包括轻微症状）
   - 提取症状的时间、程度、频率等信息
   - 识别症状的位置和性质

2. **困扰识别**：
   - 识别心理困扰（担心、焦虑、害怕等）
   - 识别功能变化（如"走几步就喘"）
   - 识别异常感觉（如"感觉不对劲"）

3. **情况判断**：
   - 情况A：明确无症状（纯筛查/体检规划）
   - 情况B：存在症状/困扰（不论轻重）
   - 情况C：不确定/模糊/混合诉求

请以JSON格式返回结果：
{{
    "status": "no_symptom|has_symptom|uncertain",
    "symptoms": [
        {{
            "original_text": "原始文本",
            "standard_term": "标准术语",
            "temporal_info": {{"start_time": "...", "duration": "...", "frequency": "..."}},
            "severity": "轻度|中度|重度",
            "location": "位置",
            "context": {{}}
        }}
    ],
    "concerns": [
        {{
            "type": "心理|功能变化|异常感觉",
            "description": "困扰描述",
            "severity": "轻度|中度|重度"
        }}
    ],
    "confidence": 0.0-1.0,
    "reasoning": "识别理由"
}}"""

    # 情况判断Prompt模板
    SITUATION_JUDGMENT_PROMPT = """你是一个医疗AI助手，需要判断用户的健康状态情况。

用户输入：{user_input}
NLU结果：
- 意图：{intent}
- 意图置信度：{intent_confidence}

症状/困扰识别结果：
- 状态：{symptom_status}
- 症状：{symptoms}
- 困扰：{concerns}
- 识别置信度：{symptom_confidence}

请综合判断用户属于以下哪种情况：
1. **情况A（no_symptom）**：明确无症状（纯筛查/体检规划）
   - 意图为"screening"且无症状
   - 明确表达无症状（如"我想做个体检"）
   - 置信度 ≥ 0.8

2. **情况B（has_symptom）**：存在症状/困扰（不论轻重）
   - 意图为"diagnosis"且有症状
   - 存在明确的症状或困扰
   - 置信度 ≥ 0.8

3. **情况C（uncertain）**：不确定/模糊/混合诉求
   - 意图为"mixed"或"unknown"
   - 症状存在但不明确
   - 混合诉求（既有筛查又有症状）
   - 置信度 < 0.8

请以JSON格式返回结果：
{{
    "status": "no_symptom|has_symptom|uncertain",
    "confidence": 0.0-1.0,
    "reasoning": "判断理由"
}}"""

    @classmethod
    def get_symptom_concern_identification_prompt(cls, nlu_result: Dict[str, Any]) -> str:
        """
        获取症状/困扰识别Prompt
        
        Args:
            nlu_result: Step 1的NLU结果
        
        Returns:
            填充后的Prompt
        """
        import json
        user_input = nlu_result.get("original_input", "")
        intent = nlu_result.get("intent", "unknown")
        symptoms = nlu_result.get("symptoms", [])
        context = {
            "temporal_info": nlu_result.get("temporal_info", {}),
            "basic_info": nlu_result.get("basic_info", {})
        }
        
        return cls.SYMPTOM_CONCERN_IDENTIFICATION_PROMPT.format(
            user_input=user_input,
            intent=intent,
            symptoms=json.dumps(symptoms, ensure_ascii=False),
            context=json.dumps(context, ensure_ascii=False)
        )
    
    @classmethod
    def get_situation_judgment_prompt(
        cls,
        symptom_result: Any,
        nlu_result: Dict[str, Any]
    ) -> str:
        """
        获取情况判断Prompt
        
        Args:
            symptom_result: 症状/困扰识别结果
            nlu_result: Step 1的NLU结果
        
        Returns:
            填充后的Prompt
        """
        import json
        user_input = nlu_result.get("original_input", "")
        intent = nlu_result.get("intent", "unknown")
        intent_confidence = nlu_result.get("intent_confidence", 0.5)
        symptom_status = symptom_result.status
        symptoms = [s.original_text for s in symptom_result.symptoms]
        concerns = [c.description for c in symptom_result.concerns]
        symptom_confidence = symptom_result.confidence
        
        return cls.SITUATION_JUDGMENT_PROMPT.format(
            user_input=user_input,
            intent=intent,
            intent_confidence=intent_confidence,
            symptom_status=symptom_status,
            symptoms=json.dumps(symptoms, ensure_ascii=False),
            concerns=json.dumps(concerns, ensure_ascii=False),
            symptom_confidence=symptom_confidence
        )

