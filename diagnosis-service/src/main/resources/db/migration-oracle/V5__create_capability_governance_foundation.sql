CREATE TABLE clinical_capability_binding (
    binding_id VARCHAR2(128) PRIMARY KEY,
    capability_id VARCHAR2(64) NOT NULL,
    capability_version VARCHAR2(64) NOT NULL,
    capability_set_version VARCHAR2(64) NOT NULL,
    scope_version VARCHAR2(64) NOT NULL,
    contract_version VARCHAR2(64) NOT NULL,
    population_scope VARCHAR2(64) NOT NULL,
    region_scope VARCHAR2(64) NOT NULL,
    language_scope VARCHAR2(32) NOT NULL,
    channel_scope VARCHAR2(32) NOT NULL,
    binding_status VARCHAR2(32) NOT NULL,
    effective_from TIMESTAMP NOT NULL,
    effective_until TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_cap_binding_capability
    ON clinical_capability_binding (capability_id, binding_status);

CREATE TABLE clinical_capability_call_trace (
    capability_call_id VARCHAR2(128) PRIMARY KEY,
    consultation_id VARCHAR2(128) NOT NULL,
    thread_id VARCHAR2(128) NOT NULL,
    run_id VARCHAR2(128) NOT NULL,
    event_id VARCHAR2(128) NOT NULL,
    unit_id VARCHAR2(32) NOT NULL,
    capability_id VARCHAR2(64) NOT NULL,
    binding_id VARCHAR2(128) NOT NULL,
    capability_result_ref VARCHAR2(128) NULL,
    decision_ref VARCHAR2(128) NULL,
    proposal_ref VARCHAR2(128) NULL,
    commit_ref VARCHAR2(128) NULL,
    clinical_state_version_before NUMBER(10) NULL,
    clinical_state_version_after NUMBER(10) NULL,
    call_status VARCHAR2(32) NOT NULL,
    reason_code VARCHAR2(128) NULL,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP NULL
);

CREATE INDEX idx_cap_trace_run
    ON clinical_capability_call_trace (consultation_id, run_id, unit_id);
CREATE INDEX idx_cap_trace_binding
    ON clinical_capability_call_trace (binding_id, capability_id);
