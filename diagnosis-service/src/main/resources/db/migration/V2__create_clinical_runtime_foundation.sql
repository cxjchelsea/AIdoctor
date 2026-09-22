CREATE TABLE clinical_runtime_binding (
    consultation_id VARCHAR(128) NOT NULL,
    cdp_id VARCHAR(128) NOT NULL,
    runtime_authority VARCHAR(64) NOT NULL,
    thread_id VARCHAR(128) NOT NULL,
    scope_version VARCHAR(64) NOT NULL,
    capability_set_version VARCHAR(64) NOT NULL,
    contract_version VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (consultation_id),
    UNIQUE KEY uk_runtime_binding_thread (thread_id)
);

CREATE TABLE canonical_business_event (
    event_id VARCHAR(128) NOT NULL,
    consultation_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    payload_digest VARCHAR(128) NOT NULL,
    received_at TIMESTAMP NOT NULL,
    PRIMARY KEY (event_id),
    UNIQUE KEY uk_canonical_event_idempotency (idempotency_key),
    KEY idx_canonical_event_consultation (consultation_id)
);

CREATE TABLE clinical_runtime_run (
    run_id VARCHAR(128) NOT NULL,
    thread_id VARCHAR(128) NOT NULL,
    consultation_id VARCHAR(128) NOT NULL,
    event_id VARCHAR(128) NOT NULL,
    based_on_clinical_state_version INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (run_id),
    KEY idx_runtime_run_consultation (consultation_id),
    KEY idx_runtime_run_event (event_id),
    KEY idx_runtime_run_thread (thread_id)
);
