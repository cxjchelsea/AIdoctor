-- U07 Legacy DBA read-only evidence: Oracle. Execute ONLY with approved read-only account.
-- Assumes current Oracle schema owns Flyway history; DBA must verify actual owner.
-- Do not query or export any clinical/PHI table records.
-- Q1: history table existence; if absent, DO NOT execute Q2.
SELECT COUNT(*) AS history_table_count
FROM user_tables
WHERE table_name = 'FLYWAY_SCHEMA_HISTORY';

-- Q2: only after Q1=1; authorized DBA must sanitize any unexpected values.
SELECT installed_rank, version, checksum, success, type
FROM flyway_schema_history
ORDER BY installed_rank;

-- Q3: legacy table existence (different Oracle V1 foundation from MySQL).
SELECT table_name
FROM user_tables
WHERE table_name IN ('AGENT_STATE', 'AUDIT_TRAIL', 'CDP', 'CDP_VERSION')
ORDER BY table_name;

-- Q4: only SQL data types for the nine MySQL legacy fields, if present.
SELECT table_name, column_name, data_type, nullable
FROM user_tab_columns
WHERE (
    (table_name = 'AGENT_STATE'
     AND column_name IN ('THRESHOLDS', 'BUDGET', 'FAILURE_BACKOFF',
                         'TRIED_TOOLS', 'EVIDENCE_FUSION_STATE', 'STOP_CONDITIONS'))
    OR
    (table_name = 'AUDIT_TRAIL'
     AND column_name IN ('TOOL_CALL', 'CDP_UPDATE', 'AGENT_DECISION'))
  )
ORDER BY table_name, column_name;
