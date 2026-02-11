"""
健康状态判定服务（tool_0）

核心目标：判断"这个人，现在需要被当成'病人'对待吗？"
这是医生的第一职责，发生在"诊断之前"。
"""
import uuid
from typing import Dict, Any, List, Optional
from datetime import datetime

from app.models.request import HealthStateAssessmentRequest
from app.models.response import HealthStateAssessmentResponse, WellnessPlan, EntryAssessmentResult
from app.services.entry_assessment import perform_entry_assessment_async
from app.detectors.symptom_severity_detector import assess_symptom_severity, SeverityLevel
from app.detectors.risk_signal_detector import early_risk_screening, RiskLevel
from app.detectors.red_flag_detector import detect_red_flags
from app.rules.work_mode_rules import determine_work_mode, WorkMode
from app.services.wellness_plan_generator import generate_wellness_plan
from app.utils.logger import logger


class HealthStateAssessmentService:
    """健康状态判定服务"""
    
    # 高危症状关键词（红旗信号）
    HIGH_RISK_SYMPTOMS = {
        "胸痛": ["胸痛", "胸闷", "心绞痛", "心脏不适"],
        "呼吸困难": ["呼吸困难", "气促", "喘不上气", "窒息感"],
        "意识不清": ["意识不清", "昏迷", "晕厥", "意识模糊"],
        "剧烈头痛": ["剧烈头痛", "突发头痛", "爆炸样头痛"],
        "大出血": ["大出血", "咯血", "呕血", "便血"],
        "高热": ["高热", "40度以上"],
    }
    
    # 高风险症状组合
    HIGH_RISK_COMBINATIONS = [
        ["胸痛", "气短", "出汗"],  # 心梗三联征
        ["剧烈头痛", "恶心", "呕吐"],  # 脑出血可能
        ["呼吸困难", "胸痛", "晕厥"],  # 肺栓塞可能
    ]
    
    # 中等严重症状
    MODERATE_SEVERITY_SYMPTOMS = [
        "胸痛", "腹痛", "头痛", "发热", "咳嗽", 
        "气短", "乏力", "恶心", "呕吐", "腹泻"
    ]
    
    # 早期风险信号关键词（用于健康管理）
    EARLY_RISK_SIGNALS = {
        "高血压风险": ["家族史", "高盐饮食", "肥胖", "缺乏运动"],
        "糖尿病风险": ["家族史", "肥胖", "多饮", "多尿"],
        "心血管风险": ["家族史", "高脂饮食", "吸烟", "饮酒"],
    }
    
    def __init__(self):
        """初始化服务"""
        pass
    
    async def assess(self, request: HealthStateAssessmentRequest) -> HealthStateAssessmentResponse:
        """
        健康状态判定
        
        判断"这个人，现在需要被当成'病人'对待吗？"
        
        流程：
        1. 执行入口判定（P0模块，Step 1-5）
        2. 根据入口判定结果进行健康状态判定
        3. 返回完整的判定结果
        """
        # ========== 第一步：执行入口判定（P0模块）==========
        user_input_dict = {
            "userInput": request.userInput or "",
            "basicInfo": request.basicInfo or {},
            "symptoms": request.symptoms or [],
            "userId": request.userId,  # 传递userId用于临床解析服务调用
            "sessionId": request.cdpId or f"session_{request.userId}"  # 使用cdpId作为sessionId，如果没有则生成
        }
        
        entry_assessment_result = await perform_entry_assessment_async(user_input_dict)
        entry_assessment_data = entry_assessment_result.get("entry_assessment", {})
        path_result = entry_assessment_result.get("path_result", {})
        
        # 如果命中危险信号，直接返回退出结果
        if entry_assessment_data.get("redFlagsHit", False):
            return HealthStateAssessmentResponse(
                entryAssessment=EntryAssessmentResult(**entry_assessment_data),
                needsClinicalMode=True,
                workMode=WorkMode.CLINICAL_MODE,
                riskLevel=RiskLevel.L1,
                assessmentReason=path_result.get("message", "检测到危险信号，建议立即就医"),
                redFlags=entry_assessment_data.get("redFlagsList", []),
                wellnessPlan=None,
                cdpId=request.cdpId or path_result.get("cdp_id") or self._generate_cdp_id(request.userId)
            )
        
        # ========== 第二步：基于入口判定结果进行健康状态判定 ==========
        # 提取信息
        symptoms = request.symptoms or []
        user_input = request.userInput or ""
        basic_info = request.basicInfo or {}
        vital_signs = request.vitalSigns or {}
        
        # 从入口判定结果中获取症状信息
        entry_symptoms = entry_assessment_data.get("symptoms", [])
        if entry_symptoms:
            symptoms = list(set(symptoms + entry_symptoms))
        
        # 从userInput中提取症状（如果没有提供symptoms）
        if not symptoms and user_input:
            symptoms = self._extract_symptoms_from_text(user_input)
        
        # 症状严重程度评估
        severity = self._assess_symptom_severity(symptoms, user_input)
        
        # 风险早筛
        risk_signals = self._early_risk_screening(basic_info, user_input)
        
        # 红旗信号识别（与入口判定结果合并）
        red_flags = self._detect_red_flags(symptoms, user_input)
        entry_red_flags = entry_assessment_data.get("redFlagsList", [])
        red_flags = list(set(red_flags + entry_red_flags))
        
        # 工作态判定（根据入口判定的路径选择结果调整）
        path_selected = entry_assessment_data.get("pathSelected", "B")
        if path_selected == "A":
            # A路径（健康筛查）→ 健康管理态
            work_mode = WorkMode.WELLNESS_MODE
        elif path_selected == "B":
            # B路径（症状诊断）→ 临床诊疗态
            work_mode = WorkMode.CLINICAL_MODE
        else:
            # 默认使用原有逻辑
            work_mode = self._determine_work_mode(severity, risk_signals, red_flags, vital_signs)
        
        # 计算风险等级
        risk_level = self._calculate_risk_level(severity, risk_signals, red_flags, vital_signs)
        
        # 生成CDP ID（优先使用请求中传入的CDP ID，其次使用入口判定的CDP ID，最后生成新的）
        cdp_id = request.cdpId or path_result.get("cdp_id") or self._generate_cdp_id(request.userId)
        
        # 生成判定理由
        assessment_reason = self._generate_reason(
            severity, risk_signals, red_flags, work_mode, risk_level
        )
        
        # 如果是健康管理态，生成健康管理计划
        wellness_plan = None
        if work_mode == WorkMode.WELLNESS_MODE:
            wellness_plan = self._generate_wellness_plan(risk_signals, basic_info)
        
        # 构建响应
        return HealthStateAssessmentResponse(
            entryAssessment=EntryAssessmentResult(**entry_assessment_data),
            needsClinicalMode=(work_mode == WorkMode.CLINICAL_MODE),
            workMode=work_mode,
            riskLevel=risk_level,
            assessmentReason=assessment_reason,
            redFlags=red_flags,
            wellnessPlan=wellness_plan,
            cdpId=cdp_id
        )
    
    def _extract_symptoms_from_text(self, text: str) -> List[str]:
        """从文本中提取症状关键词"""
        if not text:
            return []
        
        symptoms = []
        # 简单关键词匹配（实际应该使用NER模型）
        symptom_keywords = [
            "胸痛", "胸闷", "腹痛", "头痛", "发热", "咳嗽", 
            "气短", "乏力", "恶心", "呕吐", "腹泻", "心悸",
            "头晕", "失眠", "食欲不振", "体重下降", "关节痛"
        ]
        
        for keyword in symptom_keywords:
            if keyword in text:
                symptoms.append(keyword)
        
        return symptoms
    
    def _assess_symptom_severity(
        self, 
        symptoms: List[str], 
        user_input: str
    ) -> str:
        """
        症状严重程度评估
        
        使用症状严重程度判定规则库（0.1）
        判断症状是否在正常范围
        """
        result = assess_symptom_severity(symptoms, user_input)
        return result["severity_level"]
    
    def _early_risk_screening(
        self, 
        basic_info: Dict[str, Any], 
        user_input: str
    ) -> Dict[str, Any]:
        """
        早期风险信号识别
        
        使用早期风险信号识别库（0.2）
        识别早期风险信号（家族史、行为、慢性暴露）
        """
        return early_risk_screening(basic_info, user_input)
    
    def _detect_red_flags(
        self, 
        symptoms: List[str], 
        user_input: str
    ) -> List[str]:
        """
        红旗信号识别
        
        使用红旗信号库（0.3）
        识别高危症状组合
        """
        return detect_red_flags(symptoms, user_input)
    
    def _determine_work_mode(
        self,
        severity: str,
        risk_signals: Dict[str, Any],
        red_flags: List[str],
        vital_signs: Dict[str, Any]
    ) -> str:
        """
        工作态判定
        
        使用工作态判定规则库（0.4）
        """
        from app.rules.work_mode_rules import determine_work_mode
        return determine_work_mode(severity, risk_signals, red_flags, vital_signs)
    
    def _calculate_risk_level(
        self,
        severity: str,
        risk_signals: Dict[str, Any],
        red_flags: List[str],
        vital_signs: Dict[str, Any]
    ) -> str:
        """
        计算风险等级
        
        L1: 极高风险（需立即处理）
        L2: 高风险（需尽快处理）
        L3: 中风险（建议关注）
        L4: 低风险（正常管理）
        """
        # 有红旗信号 → L1或L2
        if red_flags:
            # 检查生命体征是否异常
            if vital_signs:
                bp = vital_signs.get("bp", {})
                if isinstance(bp, dict):
                    systolic = bp.get("systolic")
                    if systolic and (systolic >= 180 or systolic < 90):
                        return RiskLevel.L1
                    
                    heart_rate = vital_signs.get("heartRate")
                    if heart_rate and (heart_rate >= 120 or heart_rate <= 50):
                        return RiskLevel.L1
            
            return RiskLevel.L2
        
        # 症状严重程度高 → L2
        if severity == SeverityLevel.HIGH:
            return RiskLevel.L2
        
        # 症状严重程度中 → L2或L3
        if severity == SeverityLevel.MODERATE:
            risk_level = risk_signals.get("risk_level", RiskLevel.L4)
            if risk_level in [RiskLevel.L1, RiskLevel.L2]:
                return RiskLevel.L2
            return RiskLevel.L3
        
        # 使用风险早筛的结果
        risk_level = risk_signals.get("risk_level", RiskLevel.L4)
        if severity == SeverityLevel.LOW:
            # 轻度症状 + 高风险因素 → L3
            if risk_level == RiskLevel.L3:
                return RiskLevel.L3
            return RiskLevel.L4
        
        # 正常范围
        return risk_level
    
    def _generate_cdp_id(self, user_id: str) -> str:
        """生成CDP ID"""
        timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
        random_part = str(uuid.uuid4())[:8]
        return f"cdp_{user_id}_{timestamp}_{random_part}"
    
    def _generate_reason(
        self,
        severity: str,
        risk_signals: Dict[str, Any],
        red_flags: List[str],
        work_mode: str,
        risk_level: str
    ) -> str:
        """生成判定理由"""
        reasons = []
        
        if red_flags:
            reasons.append(f"识别到红旗信号：{', '.join(red_flags)}")
        
        if severity == SeverityLevel.HIGH:
            reasons.append("症状严重程度高")
        elif severity == SeverityLevel.MODERATE:
            reasons.append("症状严重程度中等")
        
        risk_factors = risk_signals.get("risk_factors", [])
        if risk_factors:
            reasons.append(f"存在风险因素：{', '.join(risk_factors[:3])}")
        
        if work_mode == WorkMode.CLINICAL_MODE:
            if reasons:
                return f"{'；'.join(reasons)}，建议进入临床诊疗态"
            return "症状需要临床诊疗，建议进入临床诊疗态"
        else:
            if reasons:
                return f"{'；'.join(reasons)}，症状在正常范围，建议健康管理"
            return "症状在正常范围，建议健康管理"
    
    def _generate_wellness_plan(
        self,
        risk_signals: Dict[str, Any],
        basic_info: Dict[str, Any]
    ) -> WellnessPlan:
        """
        生成健康管理计划
        
        仅在健康管理态时调用
        """
        return generate_wellness_plan(risk_signals, basic_info)

