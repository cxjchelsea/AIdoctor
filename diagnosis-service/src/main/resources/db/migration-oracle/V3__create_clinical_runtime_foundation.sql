CREATE TABLE clinical_runtime_binding (
    consultation_id VARCHAR2(128) NOT NULL,
    cdp_id VARCHAR2(128) NOT NULL,
    runtime_authority VARCHAR2(64) NOT NULL,
    thread_id VARCHAR2(128) NOT NULL,
    scope_version VARCHAR2(64) NOT NULL,
    capability_set_version VARCHAR2(64) NOT NULL,
    contract_version VARCHAR2(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_clinical_runtime_binding PRIMARY KEY (consultation_id),
    CONSTRAINT uk_runtime_binding_thread UNIQUE (thread_id)
);

CREATE TABLE canonical_business_event (
    event_id VARCHAR2(128) NOT NULL,
    consultation_id VARCHAR2(128) NOT NULL,
    event_type VARCHAR2(64) NOT NULL,
    idempotency_key VARCHAR2(128) NOT NULL,
    payload_digest VARCHAR2(128) NOT NULL,
    received_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_canonical_business_event PRIMARY KEY (event_id),
    CONSTRAINT uk_canonical_event_idempotency UNIQUE (idempotency_key)
);
CREATE INDEX idx_canonical_event_consultation ON canonical_business_event (consultation_id);

CREATE TABLE clinical_runtime_run (
    run_id VARCHAR2(128) NOT NULL,
    thread_id VARCHAR2(128) NOT NULL,
    consultation_id VARCHAR2(128) NOT NULL,
    event_id VARCHAR2(128) NOT NULL,
    based_on_clinical_state_version NUMBER(10) NOT NULL,
    status VARCHAR2(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT pk_clinical_runtime_run PRIMARY KEY (run_id)
);
CREATE INDEX idx_runtime_run_consultation ON clinical_runtime_run (consultation_id);
CREATE INDEX idx_runtime_run_event ON clinical_runtime_run (event_id);
CREATE INDEX idx_runtime_run_thread ON clinical_runtime_run (thread_id);
