-- ============================================
-- 诊断服务数据库初始化脚本 (Oracle版本)
-- 用于开发/生产环境
-- ============================================

-- CDP（Clinical Decision Package）表 - 临床决策包
CREATE TABLE cdp (
    id VARCHAR2(64) NOT NULL,
    patient_id VARCHAR2(64) NOT NULL,
    session_id VARCHAR2(64) NOT NULL,
    version NUMBER(10) NOT NULL DEFAULT 1,
    cdp_status VARCHAR2(64),
    
    -- JSON字段（使用JSON类型，Oracle 12c+）
    health_state_assessment CLOB,
    wellness_plan CLOB,
    patient_state CLOB,
    ddx CLOB,
    evidence_graph CLOB,
    workup_plan CLOB,
    management_plan CLOB,
    triage CLOB,
    uncertainty CLOB,
    audit CLOB,
    
    -- 时间戳
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT pk_cdp PRIMARY KEY (id)
);

-- 创建索引
CREATE INDEX idx_cdp_patient_id ON cdp(patient_id);
CREATE INDEX idx_cdp_session_id ON cdp(session_id);
CREATE INDEX idx_cdp_version ON cdp(version);
CREATE INDEX idx_cdp_created_at ON cdp(created_at);

-- 添加表注释
COMMENT ON TABLE cdp IS '临床决策包表';
COMMENT ON COLUMN cdp.id IS 'CDP ID';
COMMENT ON COLUMN cdp.patient_id IS '患者ID';
COMMENT ON COLUMN cdp.session_id IS '会话ID';
COMMENT ON COLUMN cdp.version IS '版本号';
COMMENT ON COLUMN cdp.cdp_status IS 'CDP状态';
COMMENT ON COLUMN cdp.health_state_assessment IS '健康状态判定结果（JSON）';
COMMENT ON COLUMN cdp.wellness_plan IS '健康管理计划（JSON）';
COMMENT ON COLUMN cdp.patient_state IS '患者状态（JSON）';
COMMENT ON COLUMN cdp.ddx IS '鉴别诊断列表（JSON）';
COMMENT ON COLUMN cdp.evidence_graph IS '证据图（JSON）';
COMMENT ON COLUMN cdp.workup_plan IS '检查计划（JSON）';
COMMENT ON COLUMN cdp.management_plan IS '治疗计划（JSON）';
COMMENT ON COLUMN cdp.triage IS '风险评估（JSON）';
COMMENT ON COLUMN cdp.uncertainty IS '不确定性信息（JSON）';
COMMENT ON COLUMN cdp.audit IS '审计信息（JSON）';

-- CDP版本历史表
CREATE TABLE cdp_version (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cdp_id VARCHAR2(64) NOT NULL,
    version_number NUMBER(10) NOT NULL,
    cdp_data CLOB,
    create_time TIMESTAMP(6)
);

CREATE INDEX idx_cdp_version_cdp_id ON cdp_version(cdp_id);
CREATE INDEX idx_cdp_version_number ON cdp_version(version_number);

COMMENT ON TABLE cdp_version IS 'CDP版本历史表';
COMMENT ON COLUMN cdp_version.id IS '版本记录ID';
COMMENT ON COLUMN cdp_version.cdp_id IS 'CDP ID';
COMMENT ON COLUMN cdp_version.version_number IS '版本号';
COMMENT ON COLUMN cdp_version.cdp_data IS 'CDP数据的JSON序列化字符串';

-- 诊断记录表
CREATE TABLE diagnosis_record (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id VARCHAR2(64) NOT NULL,
    family_id VARCHAR2(64),
    cdp_id VARCHAR2(64),
    work_mode VARCHAR2(32),
    diagnosis_type VARCHAR2(32) NOT NULL,
    status VARCHAR2(32) NOT NULL,
    
    -- 症状信息
    chief_complaint CLOB,
    symptom_duration VARCHAR2(64),
    symptom_severity NUMBER(10),
    symptom_frequency VARCHAR2(32),
    symptom_location CLOB,
    accompanying_symptoms CLOB,
    symptom_features CLOB,
    
    -- 体征数据
    vital_signs CLOB,
    physical_exam CLOB,
    
    -- 检查结果
    examination_results CLOB,
    
    -- 诊断结果
    diagnosis_result CLOB,
    
    -- 对话记录
    dialogue_history CLOB,
    questioning_count NUMBER(10) DEFAULT 0,
    
    -- 元数据
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP(6)
);

CREATE INDEX idx_diagnosis_user_id ON diagnosis_record(user_id);
CREATE INDEX idx_diagnosis_family_id ON diagnosis_record(family_id);
CREATE INDEX idx_diagnosis_cdp_id ON diagnosis_record(cdp_id);
CREATE INDEX idx_diagnosis_status ON diagnosis_record(status);
CREATE INDEX idx_diagnosis_created_at ON diagnosis_record(created_at);

COMMENT ON TABLE diagnosis_record IS '诊断记录表';

-- 检查记录表
CREATE TABLE examination_record (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id VARCHAR2(64) NOT NULL,
    family_id VARCHAR2(64),
    examination_type VARCHAR2(32) NOT NULL,
    
    -- 检查方案
    plan_id NUMBER(19),
    plan_name VARCHAR2(255),
    plan_items CLOB,
    
    -- 报告信息
    report_type VARCHAR2(32),
    report_file_path VARCHAR2(512),
    report_ocr_result CLOB,
    report_structured_data CLOB,
    
    -- 解读结果
    interpretation_result CLOB,
    
    -- 元数据
    examination_date DATE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_exam_user_id ON examination_record(user_id);
CREATE INDEX idx_exam_family_id ON examination_record(family_id);
CREATE INDEX idx_exam_type ON examination_record(examination_type);
CREATE INDEX idx_exam_date ON examination_record(examination_date);

COMMENT ON TABLE examination_record IS '检查记录表';

-- 检查方案表
CREATE TABLE examination_plan (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id VARCHAR2(64) NOT NULL,
    plan_name VARCHAR2(255) NOT NULL,
    plan_type VARCHAR2(32) NOT NULL,
    
    -- 方案内容
    plan_items CLOB,
    target_conditions CLOB,
    
    -- 元数据
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_plan_user_id ON examination_plan(user_id);
CREATE INDEX idx_plan_type ON examination_plan(plan_type);

COMMENT ON TABLE examination_plan IS '检查方案表';

-- 随访计划表
CREATE TABLE follow_up_plan (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_id NUMBER(19),
    plan_content CLOB,
    next_follow_up_time TIMESTAMP(6),
    status VARCHAR2(255),
    create_time TIMESTAMP(6),
    update_time TIMESTAMP(6)
);

COMMENT ON TABLE follow_up_plan IS '随访计划表';

-- 健康状态判定记录表
CREATE TABLE health_state_assessment_record (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_id NUMBER(19),
    assessment_result CLOB,
    path_selection VARCHAR2(255),
    create_time TIMESTAMP(6)
);

COMMENT ON TABLE health_state_assessment_record IS '健康状态判定记录表';

-- 健康筛查记录表
CREATE TABLE wellness_screening_record (
    id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    patient_id NUMBER(19),
    demand_type VARCHAR2(255),
    health_profile CLOB,
    screening_result CLOB,
    create_time TIMESTAMP(6)
);

COMMENT ON TABLE wellness_screening_record IS '健康筛查记录表';

-- 创建触发器：自动更新updated_at字段（Oracle）
CREATE OR REPLACE TRIGGER trg_cdp_updated_at
    BEFORE UPDATE ON cdp
    FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

CREATE OR REPLACE TRIGGER trg_diagnosis_record_updated_at
    BEFORE UPDATE ON diagnosis_record
    FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

CREATE OR REPLACE TRIGGER trg_examination_record_updated_at
    BEFORE UPDATE ON examination_record
    FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

CREATE OR REPLACE TRIGGER trg_examination_plan_updated_at
    BEFORE UPDATE ON examination_plan
    FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/
