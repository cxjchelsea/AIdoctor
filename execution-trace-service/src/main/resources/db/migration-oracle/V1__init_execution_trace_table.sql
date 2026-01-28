CREATE TABLE execution_trace (
                                 id NUMBER(19) GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                 cdp_id VARCHAR2(64) NOT NULL,
                                 trace_id VARCHAR2(64),
                                 event_type VARCHAR2(50) NOT NULL,
                                 service VARCHAR2(100),
                                 module VARCHAR2(100),
                                 method VARCHAR2(100),
                                 step VARCHAR2(100),
                                 status VARCHAR2(20),
                                 duration NUMBER(19),
                                 event_timestamp TIMESTAMP(6) NOT NULL,
                                 input_data CLOB,
                                 output_data CLOB,
                                 error_message CLOB,
                                 request_url VARCHAR2(512)
);

-- 创建索引
CREATE INDEX idx_trace_cdp_id ON execution_trace(cdp_id);
CREATE INDEX idx_trace_timestamp ON execution_trace(event_timestamp);
CREATE INDEX idx_trace_service ON execution_trace(service);

-- 添加表注释
COMMENT ON TABLE execution_trace IS '执行追踪表';
COMMENT ON COLUMN execution_trace.id IS '追踪记录ID';
COMMENT ON COLUMN execution_trace.cdp_id IS 'CDP ID';
COMMENT ON COLUMN execution_trace.trace_id IS '单次操作追踪ID';
COMMENT ON COLUMN execution_trace.event_type IS '事件类型 (e.g., SERVICE_CALL_START, SERVICE_CALL_END)';
COMMENT ON COLUMN execution_trace.service IS '服务名称';
COMMENT ON COLUMN execution_trace.module IS '模块名称';
COMMENT ON COLUMN execution_trace.method IS '方法名称';
COMMENT ON COLUMN execution_trace.step IS '编排步骤名称';
COMMENT ON COLUMN execution_trace.status IS '状态 (e.g., IN_PROGRESS, SUCCESS, ERROR)';
COMMENT ON COLUMN execution_trace.duration IS '执行耗时 (ms)';
COMMENT ON COLUMN execution_trace.event_timestamp IS '事件时间戳';
COMMENT ON COLUMN execution_trace.input_data IS '输入数据 (JSON)';
COMMENT ON COLUMN execution_trace.output_data IS '输出数据 (JSON)';
COMMENT ON COLUMN execution_trace.error_message IS '错误信息';
COMMENT ON COLUMN execution_trace.request_url IS '请求URL (针对Feign调用)';
