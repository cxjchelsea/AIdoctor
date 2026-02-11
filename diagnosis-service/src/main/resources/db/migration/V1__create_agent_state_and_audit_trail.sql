-- 创建AgentState表
-- 参考文档：《6.数据模型设计/AgentState数据结构设计.md》

CREATE TABLE IF NOT EXISTS agent_state (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    cdp_id VARCHAR(64) NOT NULL,
    current_step INT,
    work_mode VARCHAR(32),
    thresholds CLOB,
    budget CLOB,
    failure_backoff CLOB,
    tried_tools CLOB,
    evidence_fusion_state CLOB,
    stop_conditions CLOB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id),
    INDEX idx_cdp_id (cdp_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主Agent策略状态表';

-- 创建AuditTrail表
-- 参考文档：《6.数据模型设计/AuditTrail数据结构设计.md》

CREATE TABLE IF NOT EXISTS audit_trail (
    id VARCHAR(64) PRIMARY KEY,
    cdp_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    event_type VARCHAR(32) NOT NULL COMMENT 'tool_call/cdp_update/agent_decision',
    tool_call CLOB COMMENT '工具调用记录（event_type=tool_call时使用）',
    cdp_update CLOB COMMENT 'CDP更新记录（event_type=cdp_update时使用）',
    agent_decision CLOB COMMENT '主Agent决策记录（event_type=agent_decision时使用）',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cdp_id (cdp_id),
    INDEX idx_session_id (session_id),
    INDEX idx_event_type (event_type),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计轨迹表';

