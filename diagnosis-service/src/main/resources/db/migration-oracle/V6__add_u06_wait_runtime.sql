ALTER TABLE clinical_consultation ADD (current_wait_effect_id VARCHAR2(128 CHAR));

CREATE TABLE clinical_consultation_wait_effect (
 wait_effect_id VARCHAR2(128 CHAR) NOT NULL, parent_delivered_wait_effect_id VARCHAR2(128 CHAR) NOT NULL,
 consultation_id VARCHAR2(128 CHAR) NOT NULL, question_id VARCHAR2(128 CHAR) NOT NULL, delivery_id VARCHAR2(128 CHAR) NOT NULL,
 payload_fingerprint VARCHAR2(128 CHAR) NOT NULL, idempotency_key VARCHAR2(128 CHAR) NOT NULL,
 expected_prior_lifecycle VARCHAR2(32 CHAR) NOT NULL, expected_prior_row_version NUMBER(19) NOT NULL,
 committed_lifecycle VARCHAR2(32 CHAR) NOT NULL, committed_row_version NUMBER(19) NOT NULL,
 effect_status VARCHAR2(32 CHAR) NOT NULL, created_at TIMESTAMP NOT NULL, committed_at TIMESTAMP NOT NULL,
 CONSTRAINT pk_u06_consult_wait PRIMARY KEY (wait_effect_id),
 CONSTRAINT uk_u06_wait_parent UNIQUE (parent_delivered_wait_effect_id),
 CONSTRAINT uk_u06_wait_idem UNIQUE (idempotency_key)
);
CREATE INDEX idx_u06_wait_consultation ON clinical_consultation_wait_effect (consultation_id);
CREATE INDEX idx_u06_wait_question ON clinical_consultation_wait_effect (question_id);
CREATE INDEX idx_u06_wait_delivery ON clinical_consultation_wait_effect (delivery_id);

CREATE TABLE clinical_runtime_thread_state (
 thread_id VARCHAR2(128 CHAR) NOT NULL, consultation_id VARCHAR2(128 CHAR) NOT NULL,
 row_version NUMBER(19) DEFAULT 0 NOT NULL, runtime_status VARCHAR2(32 CHAR) NOT NULL,
 current_run_id VARCHAR2(128 CHAR), current_wait_checkpoint_id VARCHAR2(128 CHAR),
 current_wait_effect_id VARCHAR2(128 CHAR), updated_at TIMESTAMP NOT NULL, created_at TIMESTAMP NOT NULL,
 CONSTRAINT pk_u06_runtime_thread PRIMARY KEY (thread_id),
 CONSTRAINT uk_u06_thread_consult UNIQUE (consultation_id)
);
CREATE TABLE clinical_runtime_wait_checkpoint (
 checkpoint_id VARCHAR2(128 CHAR) NOT NULL, consultation_id VARCHAR2(128 CHAR) NOT NULL,
 thread_id VARCHAR2(128 CHAR) NOT NULL, run_id VARCHAR2(128 CHAR) NOT NULL, question_id VARCHAR2(128 CHAR) NOT NULL,
 pending_question_ref VARCHAR2(256 CHAR) NOT NULL, question_selection_effect_id VARCHAR2(128 CHAR) NOT NULL,
 question_delivery_effect_id VARCHAR2(128 CHAR) NOT NULL, question_delivered_wait_effect_id VARCHAR2(128 CHAR) NOT NULL,
 delivery_id VARCHAR2(128 CHAR) NOT NULL, delivery_confirmation_ref VARCHAR2(128 CHAR) NOT NULL,
 clinical_state_version NUMBER(10) NOT NULL, consultation_wait_effect_id VARCHAR2(128 CHAR) NOT NULL,
 dependency_binding_ref VARCHAR2(128 CHAR), question_policy_ref VARCHAR2(128 CHAR),
 payload_fingerprint VARCHAR2(128 CHAR) NOT NULL, checkpoint_status VARCHAR2(32 CHAR) NOT NULL,
 created_at TIMESTAMP NOT NULL, CONSTRAINT pk_u06_wait_checkpoint PRIMARY KEY (checkpoint_id),
 CONSTRAINT uk_u06_cp_wait_effect UNIQUE (question_delivered_wait_effect_id)
);
CREATE INDEX idx_u06_cp_thread ON clinical_runtime_wait_checkpoint (thread_id);
CREATE INDEX idx_u06_cp_run ON clinical_runtime_wait_checkpoint (run_id);
CREATE INDEX idx_u06_cp_consult ON clinical_runtime_wait_checkpoint (consultation_id);

CREATE TABLE u06_governed_execution_trace (
 trace_id VARCHAR2(128 CHAR) NOT NULL, consultation_id VARCHAR2(128 CHAR) NOT NULL,
 admission_id VARCHAR2(128 CHAR) NOT NULL, mode VARCHAR2(64 CHAR) NOT NULL,
 execution_profile VARCHAR2(64 CHAR) NOT NULL, lifecycle_status VARCHAR2(64 CHAR) NOT NULL,
 outcome_status VARCHAR2(64 CHAR), payload_fingerprint VARCHAR2(128 CHAR) NOT NULL,
 created_at VARCHAR2(64 CHAR) NOT NULL, updated_at VARCHAR2(64 CHAR) NOT NULL,
 CONSTRAINT pk_u06_trace PRIMARY KEY (trace_id)
);
CREATE TABLE u06_delivery_authority (
 selection_effect_id VARCHAR2(128 CHAR) NOT NULL, delivery_effect_id VARCHAR2(128 CHAR) NOT NULL,
 delivery_id VARCHAR2(128 CHAR) NOT NULL, authority_status VARCHAR2(64 CHAR) NOT NULL,
 created_at VARCHAR2(64 CHAR) NOT NULL, CONSTRAINT pk_u06_delivery_auth PRIMARY KEY (selection_effect_id),
 CONSTRAINT uk_u06_delivery_auth_eff UNIQUE (delivery_effect_id)
);
CREATE TABLE u06_delivery_intent (
 delivery_effect_id VARCHAR2(128 CHAR) NOT NULL, delivery_id VARCHAR2(128 CHAR) NOT NULL,
 question_id VARCHAR2(128 CHAR) NOT NULL, content_fingerprint VARCHAR2(128 CHAR) NOT NULL,
 endpoint_ref VARCHAR2(128 CHAR) NOT NULL, idempotency_key VARCHAR2(128 CHAR) NOT NULL,
 intent_status VARCHAR2(32 CHAR) NOT NULL, created_at VARCHAR2(64 CHAR) NOT NULL,
 CONSTRAINT pk_u06_delivery_intent PRIMARY KEY (delivery_effect_id),
 CONSTRAINT uk_u06_delivery_intent_id UNIQUE (delivery_id),
 CONSTRAINT uk_u06_delivery_intent_i UNIQUE (idempotency_key)
);
CREATE TABLE u06_delivery_attempt (
 attempt_id VARCHAR2(180 CHAR) NOT NULL, delivery_effect_id VARCHAR2(128 CHAR) NOT NULL,
 delivery_id VARCHAR2(128 CHAR) NOT NULL, attempt_status VARCHAR2(64 CHAR) NOT NULL,
 created_at VARCHAR2(64 CHAR) NOT NULL, CONSTRAINT pk_u06_delivery_attempt PRIMARY KEY (attempt_id)
);
CREATE TABLE u06_delivery_receipt (
 receipt_id VARCHAR2(180 CHAR) NOT NULL, delivery_effect_id VARCHAR2(128 CHAR) NOT NULL,
 delivery_id VARCHAR2(128 CHAR) NOT NULL, receipt_status VARCHAR2(64 CHAR) NOT NULL,
 evidence_type VARCHAR2(32 CHAR) NOT NULL, created_at VARCHAR2(64 CHAR) NOT NULL,
 CONSTRAINT pk_u06_delivery_receipt PRIMARY KEY (receipt_id)
);
CREATE TABLE u06_delivery_confirmation (
 confirmation_evaluation_id VARCHAR2(128 CHAR) NOT NULL, delivery_effect_id VARCHAR2(128 CHAR) NOT NULL,
 delivery_id VARCHAR2(128 CHAR) NOT NULL, confirmation_status VARCHAR2(32 CHAR) NOT NULL,
 confirmation_fingerprint VARCHAR2(128 CHAR) NOT NULL, created_at VARCHAR2(64 CHAR) NOT NULL,
 CONSTRAINT pk_u06_delivery_confirm PRIMARY KEY (confirmation_evaluation_id)
);
CREATE TABLE u06_delivery_ledger (
 delivery_effect_id VARCHAR2(128 CHAR) NOT NULL, delivery_id VARCHAR2(128 CHAR) NOT NULL,
 confirmation_status VARCHAR2(32 CHAR) NOT NULL, canonical_fingerprint VARCHAR2(128 CHAR) NOT NULL,
 created_at VARCHAR2(64 CHAR) NOT NULL, CONSTRAINT pk_u06_delivery_ledger PRIMARY KEY (delivery_effect_id)
);
