-- ============================================
-- 执行追踪服务数据库初始化脚本 (MySQL版本)
-- ============================================

-- 执行追踪表
CREATE TABLE IF NOT EXISTS execution_trace (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '追踪记录ID',
    cdp_id VARCHAR(64) NOT NULL COMMENT 'CDP ID',
    trace_id VARCHAR(64) COMMENT '单次操作追踪ID',
    event_type VARCHAR(50) NOT NULL COMMENT '事件类型 (e.g., SERVICE_CALL_START, SERVICE_CALL_END)',
    service VARCHAR(100) COMMENT '服务名称',
    module VARCHAR(100) COMMENT '模块名称',
    method VARCHAR(100) COMMENT '方法名称',
    step VARCHAR(100) COMMENT '编排步骤名称',
    status VARCHAR(20) COMMENT '状态 (e.g., IN_PROGRESS, SUCCESS, ERROR)',
    duration BIGINT COMMENT '执行耗时 (ms)',
    timestamp DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '事件时间戳',
    input_data JSON COMMENT '输入数据 (JSON)',
    output_data JSON COMMENT '输出数据 (JSON)',
    error_message TEXT COMMENT '错误信息',
    request_url VARCHAR(512) COMMENT '请求URL (针对Feign调用)',

    INDEX idx_cdp_id (cdp_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_service (service)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='执行追踪表';


