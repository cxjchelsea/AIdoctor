-- ============================================
-- 添加 execution_trace 列到 cdp 表
-- ============================================

-- 为 cdp 表添加 execution_trace 列
ALTER TABLE cdp 
ADD COLUMN execution_trace JSON COMMENT '执行追踪摘要（JSON格式）' 
AFTER audit;

