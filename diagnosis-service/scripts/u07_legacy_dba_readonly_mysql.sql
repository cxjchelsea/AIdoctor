-- U07 Legacy DBA read-only evidence: MySQL. Execute ONLY with approved read-only account.
-- No business/PHI data; never copy connection details into tickets.
-- This document is a query TEMPLATE; each SELECT can be executed separately.
-- Q1: engine metadata (operator report major version only).
SELECT VERSION() AS engine_version;

-- Q2: whether Flyway history exists (if absent, DO NOT execute Q3).
SELECT COUNT(*) AS history_table_count
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'flyway_schema_history';

-- Q3: SELECT only from Flyway's administrative metadata after Q2=1.
-- Owner MUST sanitize migration descriptions/scripts and only share summary flags.
SELECT installed_rank, version, checksum, success, type
FROM flyway_schema_history
ORDER BY installed_rank;

-- Q4: legacy tables existence.
SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('agent_state', 'audit_trail')
ORDER BY table_name;

-- Q5: only nine legacy column DATA TYPES; NEVER select the column values.
SELECT table_name, column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND (
    (table_name = 'agent_state'
     AND column_name IN ('thresholds', 'budget', 'failure_backoff',
                         'tried_tools', 'evidence_fusion_state', 'stop_conditions'))
    OR
    (table_name = 'audit_trail'
     AND column_name IN ('tool_call', 'cdp_update', 'agent_decision'))
  )
ORDER BY table_name, column_name;
