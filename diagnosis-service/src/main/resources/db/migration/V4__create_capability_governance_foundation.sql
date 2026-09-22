CREATE TABLE clinical_capability_binding (
    binding_id VARCHAR(128) PRIMARY KEY,
    capability_id VARCHAR(64) NOT NULL,
    capability_version VARCHAR(64) NOT NULL,
    capability_set_version VARCHAR(64) NOT NULL,
    scope_version VARCHAR(64) NOT NULL,
    contract_version VARCHAR(64) NOT NULL,
    population_scope VARCHAR(64) NOT NULL,
    region_scope VARCHAR(64) NOT NULL,
    language_scope VARCHAR(32) NOT NULL,
    channel_scope VARCHAR(32) NOT NULL,
    binding_status VARCHAR(32) NOT NULL,
    effective_from DATETIME NOT NULL,
    effective_until DATETIME NULL,
    created_at DATETIME NOT NULL
);

CREATE INDEX idx_capability_binding_capability
    ON clinical_capability_binding (capability_id, binding_status);

CREATE TABLE clinical_capability_call_trace (
    capability_call_id VARCHAR(128) PRIMARY KEY,
    consultation_id VARCHAR(128) NOT NULL,
    thread_id VARCHAR(128) NOT NULL,
    run_id VARCHAR(128) NOT NULL,
    event_id VARCHAR(128) NOT NULL,
    unit_id VARCHAR(32) NOT NULL,
    capability_id VARCHAR(64) NOT NULL,
    binding_id VARCHAR(128) NOT NULL,
    capability_result_ref VARCHAR(128) NULL,
    decision_ref VARCHAR(128) NULL,
    proposal_ref VARCHAR(128) NULL,
    commit_ref VARCHAR(128) NULL,
    clinical_state_version_before INT NULL,
    clinical_state_version_after INT NULL,
    call_status VARCHAR(32) NOT NULL,
    reason_code VARCHAR(128) NULL,
    started_at DATETIME NOT NULL,
    finished_at DATETIME NULL
);

CREATE INDEX idx_capability_trace_run
    ON clinical_capability_call_trace (consultation_id, run_id, unit_id);
CREATE INDEX idx_capability_trace_binding
    ON clinical_capability_call_trace (binding_id, capability_id);
