-- ============================================
-- 添加 execution_trace 列到 cdp 表
-- ============================================

-- 为 cdp 表添加 execution_trace 列
ALTER TABLE cdp 
ADD execution_trace CLOB;

-- 添加字段注释
COMMENT ON COLUMN cdp.execution_trace IS '执行追踪摘要（JSON格式）';

