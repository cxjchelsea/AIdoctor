-- ============================================
-- 诊断服务数据库初始化脚本 (MySQL版本)
-- 用于本地开发环境
-- ============================================

-- CDP（Clinical Decision Package）表 - 临床决策包
CREATE TABLE IF NOT EXISTS cdp (
    id VARCHAR(64) NOT NULL PRIMARY KEY COMMENT 'CDP ID',
    patient_id VARCHAR(64) NOT NULL COMMENT '患者ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    version INTEGER NOT NULL DEFAULT 1 COMMENT '版本号',
    cdp_status VARCHAR(64) COMMENT 'CDP状态',
    
    -- JSON字段（使用JSON类型）
    health_state_assessment JSON COMMENT '健康状态判定结果',
    wellness_plan JSON COMMENT '健康管理计划',
    patient_state JSON COMMENT '患者状态',
    ddx JSON COMMENT '鉴别诊断列表',
    evidence_graph JSON COMMENT '证据图',
    workup_plan JSON COMMENT '检查计划',
    management_plan JSON COMMENT '治疗计划',
    triage JSON COMMENT '风险评估',
    uncertainty JSON COMMENT '不确定性信息',
    audit JSON COMMENT '审计信息',
    
    -- 时间戳
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    
    -- 索引
    INDEX idx_patient_id (patient_id),
    INDEX idx_session_id (session_id),
    INDEX idx_version (version),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='临床决策包表';

-- CDP版本历史表
CREATE TABLE IF NOT EXISTS cdp_version (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '版本记录ID',
    cdp_id VARCHAR(64) NOT NULL COMMENT 'CDP ID',
    version_number INTEGER NOT NULL COMMENT '版本号',
    cdp_data LONGTEXT COMMENT 'CDP数据的JSON序列化字符串',
    create_time DATETIME(6) COMMENT '创建时间',
    
    INDEX idx_cdp_id (cdp_id),
    INDEX idx_version_number (version_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CDP版本历史表';

-- 诊断记录表
CREATE TABLE IF NOT EXISTS diagnosis_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '诊断记录ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    family_id VARCHAR(64) COMMENT '家庭ID',
    cdp_id VARCHAR(64) COMMENT 'CDP ID（关联CDP）',
    work_mode VARCHAR(32) COMMENT '工作态（wellness_mode/clinical_mode）',
    diagnosis_type VARCHAR(32) NOT NULL COMMENT '诊断类型',
    status VARCHAR(32) NOT NULL COMMENT '诊断状态',
    
    -- 症状信息
    chief_complaint TEXT COMMENT '主诉',
    symptom_duration VARCHAR(64) COMMENT '症状持续时间',
    symptom_severity INTEGER COMMENT '症状严重程度',
    symptom_frequency VARCHAR(32) COMMENT '症状频率',
    symptom_location TEXT COMMENT '症状位置',
    accompanying_symptoms TEXT COMMENT '伴随症状',
    symptom_features JSON COMMENT '症状特征',
    
    -- 体征数据
    vital_signs JSON COMMENT '生命体征',
    physical_exam JSON COMMENT '体格检查',
    
    -- 检查结果
    examination_results JSON COMMENT '检查结果',
    
    -- 诊断结果
    diagnosis_result JSON COMMENT '诊断结果',
    
    -- 对话记录
    dialogue_history JSON COMMENT '对话历史',
    questioning_count INTEGER DEFAULT 0 COMMENT '追问次数',
    
    -- 元数据
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    completed_at DATETIME(6) COMMENT '完成时间',
    
    -- 索引
    INDEX idx_user_id (user_id),
    INDEX idx_family_id (family_id),
    INDEX idx_cdp_id (cdp_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诊断记录表';

-- 检查记录表
CREATE TABLE IF NOT EXISTS examination_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '检查记录ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    family_id VARCHAR(64) COMMENT '家庭ID',
    examination_type VARCHAR(32) NOT NULL COMMENT '检查类型',
    
    -- 检查方案
    plan_id BIGINT COMMENT '方案ID',
    plan_name VARCHAR(255) COMMENT '方案名称',
    plan_items JSON COMMENT '方案项目',
    
    -- 报告信息
    report_type VARCHAR(32) COMMENT '报告类型',
    report_file_path VARCHAR(512) COMMENT '报告文件路径',
    report_ocr_result JSON COMMENT 'OCR识别结果',
    report_structured_data JSON COMMENT '结构化数据',
    
    -- 解读结果
    interpretation_result JSON COMMENT '解读结果',
    
    -- 元数据
    examination_date DATE COMMENT '检查日期',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    
    -- 索引
    INDEX idx_user_id (user_id),
    INDEX idx_family_id (family_id),
    INDEX idx_examination_type (examination_type),
    INDEX idx_examination_date (examination_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查记录表';

-- 检查方案表
CREATE TABLE IF NOT EXISTS examination_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '方案ID',
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    plan_name VARCHAR(255) NOT NULL COMMENT '方案名称',
    plan_type VARCHAR(32) NOT NULL COMMENT '方案类型',
    
    -- 方案内容
    plan_items JSON COMMENT '方案项目',
    target_conditions JSON COMMENT '目标条件',
    
    -- 元数据
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    
    -- 索引
    INDEX idx_user_id (user_id),
    INDEX idx_plan_type (plan_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查方案表';

-- 随访计划表
CREATE TABLE IF NOT EXISTS follow_up_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '随访计划ID',
    patient_id BIGINT COMMENT '患者ID',
    plan_content LONGTEXT COMMENT '计划内容',
    next_follow_up_time DATETIME(6) COMMENT '下次随访时间',
    status VARCHAR(255) COMMENT '状态',
    create_time DATETIME(6) COMMENT '创建时间',
    update_time DATETIME(6) COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='随访计划表';

-- 健康状态判定记录表
CREATE TABLE IF NOT EXISTS health_state_assessment_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    patient_id BIGINT COMMENT '患者ID',
    assessment_result LONGTEXT COMMENT '判定结果',
    path_selection VARCHAR(255) COMMENT '路径选择',
    create_time DATETIME(6) COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康状态判定记录表';

-- 健康筛查记录表
CREATE TABLE IF NOT EXISTS wellness_screening_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    patient_id BIGINT COMMENT '患者ID',
    demand_type VARCHAR(255) COMMENT '需求类型',
    health_profile LONGTEXT COMMENT '健康画像',
    screening_result LONGTEXT COMMENT '筛查结果',
    create_time DATETIME(6) COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康筛查记录表';
