package com.aidoctor.diagnosis.constant;

/**
 * 错误码常量
 * 按照《AI医生系统-错误处理规范.md》定义
 */
public class ErrorCode {
    
    // ========== 诊断服务错误码（2000-2099） ==========
    
    /** 诊断记录不存在 */
    public static final int DIAGNOSIS_NOT_FOUND = 2001;
    
    /** 诊断状态不允许此操作 */
    public static final int DIAGNOSIS_STATUS_INVALID = 2002;
    
    /** 诊断状态不允许此操作 */
    public static final int DIAGNOSIS_STATUS_ERROR = 2002;
    
    /** 信息完整度不足，无法进行分析 */
    public static final int INSUFFICIENT_INFORMATION = 2003;
    
    /** 诊断分析中，请稍候 */
    public static final int DIAGNOSIS_ANALYZING = 2004;
    
    /** 诊断已完成，无法修改 */
    public static final int DIAGNOSIS_COMPLETED = 2005;
    
    /** 诊断已取消 */
    public static final int DIAGNOSIS_CANCELLED = 2006;
    
    /** 用户ID不能为空 */
    public static final int USER_ID_REQUIRED = 2007;
    
    /** 诊断类型无效 */
    public static final int INVALID_DIAGNOSIS_TYPE = 2008;
    
    /** 症状信息不完整 */
    public static final int INCOMPLETE_SYMPTOM_INFO = 2009;
    
    /** 必填信息缺口未补齐 */
    public static final int REQUIRED_INFO_GAP_NOT_FILLED = 2010;
    
    /** 工作态无效 */
    public static final int INVALID_WORK_MODE = 2011;
    
    /** CDP关联失败 */
    public static final int CDP_ASSOCIATION_FAILED = 2012;
    
    // ========== 检查服务错误码（2000-2999） ==========
    
    /** 检查记录不存在 */
    public static final int EXAMINATION_NOT_FOUND = 2001;
    
    /** 文件格式不支持 */
    public static final int UNSUPPORTED_FILE_FORMAT = 2002;
    
    /** 文件大小超限 */
    public static final int FILE_SIZE_EXCEEDED = 2003;
    
    /** 检查方案不存在 */
    public static final int EXAMINATION_PLAN_NOT_FOUND = 2004;
    
    /** 检查类型无效 */
    public static final int INVALID_EXAMINATION_TYPE = 2005;
    
    /** OCR识别失败 */
    public static final int OCR_RECOGNITION_FAILED = 2006;
    
    /** 文件解析失败 */
    public static final int FILE_PARSE_FAILED = 2007;
    
    /** 检查结果解读失败 */
    public static final int INTERPRETATION_FAILED = 2008;
    
    // ========== 脑区0：健康状态判定服务错误码（1000-1099） ==========
    
    /** 健康状态判定失败 */
    public static final int HEALTH_STATE_ASSESSMENT_FAILED = 1001;
    
    /** 症状严重程度评估失败 */
    public static final int SYMPTOM_SEVERITY_ASSESSMENT_FAILED = 1002;
    
    /** 风险早筛失败 */
    public static final int RISK_SCREENING_FAILED = 1003;
    
    /** 工作态判定失败 */
    public static final int WORK_MODE_DETERMINATION_FAILED = 1004;
    
    /** 红旗信号识别失败 */
    public static final int RED_FLAG_DETECTION_FAILED = 1005;
    
    /** 健康管理计划生成失败 */
    public static final int WELLNESS_PLAN_GENERATION_FAILED = 1006;
    
    // ========== 脑区A：病例理解服务错误码（1100-1199） ==========
    
    /** 医学概念识别失败 */
    public static final int MEDICAL_CONCEPT_RECOGNITION_FAILED = 1101;
    
    /** 概念归一化失败 */
    public static final int CONCEPT_NORMALIZATION_FAILED = 1102;
    
    /** 多模态理解失败 */
    public static final int MULTIMODAL_UNDERSTANDING_FAILED = 1103;
    
    /** 结构化提取失败 */
    public static final int STRUCTURED_EXTRACTION_FAILED = 1104;
    
    /** 歧义表达判定失败 */
    public static final int AMBIGUITY_DETERMINATION_FAILED = 1105;
    
    /** OCR识别失败（脑区A调用） */
    public static final int OCR_RECOGNITION_FAILED_BRAIN_A = 1106;
    
    // ========== 脑区B：主动问诊服务错误码（1200-1299） ==========
    
    /** 信息缺口识别失败 */
    public static final int INFORMATION_GAP_IDENTIFICATION_FAILED = 1201;
    
    /** 智能追问生成失败 */
    public static final int INTELLIGENT_QUESTIONING_GENERATION_FAILED = 1202;
    
    /** 自然语言理解失败（NLU） */
    public static final int NLU_FAILED = 1203;
    
    /** 自然语言生成失败（NLG） */
    public static final int NLG_FAILED = 1204;
    
    /** 对话上下文管理失败 */
    public static final int DIALOG_CONTEXT_MANAGEMENT_FAILED = 1205;
    
    /** 追问次数超限 */
    public static final int QUESTIONING_COUNT_EXCEEDED = 1206;
    
    // ========== 脑区C：鉴别诊断引擎错误码（3000-3999） ==========
    
    /** 诊断引擎服务不可用 */
    public static final int ENGINE_UNAVAILABLE = 3001;
    
    /** 诊断引擎调用超时 */
    public static final int ENGINE_TIMEOUT = 3002;
    
    /** 规则引擎执行失败 */
    public static final int RULE_ENGINE_FAILED = 3003;
    
    /** 知识图谱查询失败 */
    public static final int KNOWLEDGE_GRAPH_FAILED = 3004;
    
    /** 知识图谱路径检索失败（DR.KNOWS核心） */
    public static final int KG_PATH_RETRIEVAL_FAILED = 3005;
    
    /** 路径评分排序失败（DR.KNOWS核心） */
    public static final int PATH_SCORING_SORTING_FAILED = 3006;
    
    /** 路径注入LLM失败（DR.KNOWS核心） */
    public static final int PATH_INJECTION_LLM_FAILED = 3007;
    
    /** 统计模型推理失败 */
    public static final int STATISTICAL_MODEL_FAILED = 3008;
    
    /** 大模型调用失败 */
    public static final int LLM_CALL_FAILED = 3009;
    
    /** 鉴别诊断引擎失败 */
    public static final int DIFFERENTIAL_ENGINE_FAILED = 3010;
    
    /** 多引擎融合失败 */
    public static final int MULTI_ENGINE_FUSION_FAILED = 3011;
    
    /** 推理子组组织失败 */
    public static final int REASONING_GROUP_ORGANIZATION_FAILED = 3012;
    
    /** 分流路径设计失败 */
    public static final int DIVERSION_PATH_DESIGN_FAILED = 3013;
    
    /** 证据分析失败 */
    public static final int EVIDENCE_ANALYSIS_FAILED = 3014;
    
    // ========== 脑区D：检查建议引擎错误码（1300-1399） ==========
    
    /** 检查价值评估失败 */
    public static final int EXAMINATION_VALUE_ASSESSMENT_FAILED = 1301;
    
    /** 信息增益计算失败 */
    public static final int INFORMATION_GAIN_CALCULATION_FAILED = 1302;
    
    /** 检查优先级排序失败 */
    public static final int EXAMINATION_PRIORITY_SORTING_FAILED = 1303;
    
    /** 验证计划生成失败 */
    public static final int VERIFICATION_PLAN_GENERATION_FAILED = 1304;
    
    // ========== 脑区E：治疗推理引擎错误码（1400-1499） ==========
    
    /** 治疗方案推理失败 */
    public static final int TREATMENT_PLAN_REASONING_FAILED = 1401;
    
    /** 药物推荐失败 */
    public static final int DRUG_RECOMMENDATION_FAILED = 1402;
    
    /** 非药物治疗建议生成失败 */
    public static final int NON_DRUG_TREATMENT_ADVICE_GENERATION_FAILED = 1403;
    
    // ========== 脑区F：风险评估引擎错误码（1500-1599） ==========
    
    /** 高危识别失败 */
    public static final int HIGH_RISK_IDENTIFICATION_FAILED = 1501;
    
    /** 严重程度评估失败 */
    public static final int SEVERITY_ASSESSMENT_FAILED = 1502;
    
    /** 紧急程度分级失败 */
    public static final int URGENCY_GRADING_FAILED = 1503;
    
    /** 复评与升级规则执行失败 */
    public static final int RE_EVALUATION_UPGRADE_RULE_EXECUTION_FAILED = 1504;
    
    // ========== 脑区G：可解释性服务错误码（1600-1699） ==========
    
    /** 证据链构建失败 */
    public static final int EVIDENCE_CHAIN_CONSTRUCTION_FAILED = 1601;
    
    /** 推理路径可视化失败 */
    public static final int REASONING_PATH_VISUALIZATION_FAILED = 1602;
    
    /** 证据来源标注失败 */
    public static final int EVIDENCE_SOURCE_ANNOTATION_FAILED = 1603;
    
    /** 终点结论包生成失败 */
    public static final int CONCLUSION_PACKAGE_GENERATION_FAILED = 1604;
    
    // ========== CDP管理错误码（1700-1799） ==========
    
    /** CDP创建失败 */
    public static final int CDP_CREATION_FAILED = 1701;
    
    /** CDP更新失败 */
    public static final int CDP_UPDATE_FAILED = 1702;
    
    /** CDP版本控制失败 */
    public static final int CDP_VERSION_CONTROL_FAILED = 1703;
    
    /** CDP不存在 */
    public static final int CDP_NOT_FOUND = 1704;
    
    /** CDP版本历史获取失败 */
    public static final int CDP_VERSION_HISTORY_GET_FAILED = 1705;
    
    /** CDP回放失败 */
    public static final int CDP_REPLAY_FAILED = 1706;
    
    /** CDP回填失败 */
    public static final int CDP_BACKFILL_FAILED = 1707;
    
    /** CDP重排失败 */
    public static final int CDP_RERANK_FAILED = 1708;
    
    /** CDP回退失败 */
    public static final int CDP_ROLLBACK_FAILED = 1709;
    
    // ========== 健康筛查流程（A路径）错误码（1800-1899） ==========
    
    /** 需求分类失败 */
    public static final int DEMAND_CLASSIFICATION_FAILED = 1801;
    
    /** 需求类型无效 */
    public static final int INVALID_DEMAND_TYPE = 1802;
    
    /** 健康画像收集失败 */
    public static final int HEALTH_PROFILE_COLLECTION_FAILED = 1803;
    
    /** 健康画像不完整 */
    public static final int INCOMPLETE_HEALTH_PROFILE = 1804;
    
    /** 必填信息缺失 */
    public static final int REQUIRED_INFO_MISSING = 1805;
    
    /** 分支执行失败 */
    public static final int BRANCH_EXECUTION_FAILED = 1806;
    
    /** 筛查建议生成失败 */
    public static final int SCREENING_ADVICE_GENERATION_FAILED = 1807;
    
    /** 健康目标管理失败 */
    public static final int HEALTH_GOAL_MANAGEMENT_FAILED = 1809;
    
    /** 计划性健康需求识别失败 */
    public static final int PLANNED_HEALTH_NEED_IDENTIFICATION_FAILED = 1810;
    
    /** 统一结果生成失败 */
    public static final int UNIFIED_RESULT_GENERATION_FAILED = 1811;
    
    /** 随访计划设置失败 */
    public static final int FOLLOW_UP_PLAN_SETUP_FAILED = 1812;
    
    /** 健康筛查记录不存在 */
    public static final int WELLNESS_SCREENING_RECORD_NOT_FOUND = 1813;
    
    // ========== OCR服务错误码（4000-4999） ==========
    
    /** OCR识别失败 */
    public static final int OCR_FAILED = 4001;
    
    /** 文件解析失败 */
    public static final int OCR_FILE_PARSE_FAILED = 4002;
    
    /** 图片格式不支持 */
    public static final int UNSUPPORTED_IMAGE_FORMAT = 4003;
    
    /** 图片质量过低 */
    public static final int LOW_IMAGE_QUALITY = 4004;
    
    /** 文字识别置信度过低 */
    public static final int LOW_OCR_CONFIDENCE = 4005;
    
    // ========== 通用错误码（5000-5999） ==========
    
    /** 参数验证失败 */
    public static final int VALIDATION_FAILED = 5001;
    
    /** 数据格式错误 */
    public static final int DATA_FORMAT_ERROR = 5002;
    
    /** 数据库操作失败 */
    public static final int DATABASE_OPERATION_FAILED = 5003;
    
    /** 缓存操作失败 */
    public static final int CACHE_OPERATION_FAILED = 5004;
    
    /** 外部服务调用失败 */
    public static final int EXTERNAL_SERVICE_FAILED = 5005;
    
    /** 服务不可用 */
    public static final int SERVICE_UNAVAILABLE = 5006;
    
    // ========== HTTP状态码 ==========
    
    /** 成功 */
    public static final int SUCCESS = 200;
    
    /** 服务器内部错误 */
    public static final int INTERNAL_ERROR = 500;
}

