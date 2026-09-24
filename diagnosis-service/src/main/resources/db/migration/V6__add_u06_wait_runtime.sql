ALTER TABLE clinical_consultation ADD COLUMN current_wait_effect_id VARCHAR(128) NULL;

CREATE TABLE clinical_consultation_wait_effect (
 wait_effect_id VARCHAR(128) NOT NULL, parent_delivered_wait_effect_id VARCHAR(128) NOT NULL,
 consultation_id VARCHAR(128) NOT NULL, question_id VARCHAR(128) NOT NULL, delivery_id VARCHAR(128) NOT NULL,
 payload_fingerprint VARCHAR(128) NOT NULL, idempotency_key VARCHAR(128) NOT NULL,
 expected_prior_lifecycle VARCHAR(32) NOT NULL, expected_prior_row_version BIGINT NOT NULL,
 committed_lifecycle VARCHAR(32) NOT NULL, committed_row_version BIGINT NOT NULL, effect_status VARCHAR(32) NOT NULL,
 created_at DATETIME NOT NULL, committed_at DATETIME NOT NULL,
 PRIMARY KEY (wait_effect_id), UNIQUE KEY uk_u06_wait_parent (parent_delivered_wait_effect_id),
 UNIQUE KEY uk_u06_wait_idem (idempotency_key), KEY idx_u06_wait_consultation (consultation_id),
 KEY idx_u06_wait_question (question_id), KEY idx_u06_wait_delivery (delivery_id)
);
CREATE TABLE clinical_runtime_thread_state (
 thread_id VARCHAR(128) NOT NULL, consultation_id VARCHAR(128) NOT NULL, row_version BIGINT NOT NULL DEFAULT 0,
 runtime_status VARCHAR(32) NOT NULL, current_run_id VARCHAR(128) NULL, current_wait_checkpoint_id VARCHAR(128) NULL,
 current_wait_effect_id VARCHAR(128) NULL, updated_at DATETIME NOT NULL, created_at DATETIME NOT NULL,
 PRIMARY KEY (thread_id), UNIQUE KEY uk_u06_thread_consultation (consultation_id)
);
CREATE TABLE clinical_runtime_wait_checkpoint (
 checkpoint_id VARCHAR(128) NOT NULL, consultation_id VARCHAR(128) NOT NULL, thread_id VARCHAR(128) NOT NULL,
 run_id VARCHAR(128) NOT NULL, question_id VARCHAR(128) NOT NULL, pending_question_ref VARCHAR(256) NOT NULL,
 question_selection_effect_id VARCHAR(128) NOT NULL, question_delivery_effect_id VARCHAR(128) NOT NULL,
 question_delivered_wait_effect_id VARCHAR(128) NOT NULL, delivery_id VARCHAR(128) NOT NULL,
 delivery_confirmation_ref VARCHAR(128) NOT NULL, clinical_state_version INT NOT NULL,
 consultation_wait_effect_id VARCHAR(128) NOT NULL, dependency_binding_ref VARCHAR(128) NULL,
 question_policy_ref VARCHAR(128) NULL, payload_fingerprint VARCHAR(128) NOT NULL,
 checkpoint_status VARCHAR(32) NOT NULL, created_at DATETIME NOT NULL,
 PRIMARY KEY (checkpoint_id), UNIQUE KEY uk_u06_checkpoint_wait_effect (question_delivered_wait_effect_id),
 KEY idx_u06_checkpoint_thread (thread_id), KEY idx_u06_checkpoint_run (run_id),
 KEY idx_u06_checkpoint_consultation (consultation_id)
);
CREATE TABLE u06_governed_execution_trace (
 trace_id VARCHAR(128) NOT NULL, consultation_id VARCHAR(128) NOT NULL, admission_id VARCHAR(128) NOT NULL,
 mode VARCHAR(64) NOT NULL, execution_profile VARCHAR(64) NOT NULL, lifecycle_status VARCHAR(64) NOT NULL,
 outcome_status VARCHAR(64) NULL, payload_fingerprint VARCHAR(128) NOT NULL,
 created_at VARCHAR(64) NOT NULL, updated_at VARCHAR(64) NOT NULL, PRIMARY KEY (trace_id)
);
CREATE TABLE u06_delivery_authority (
 selection_effect_id VARCHAR(128) NOT NULL, delivery_effect_id VARCHAR(128) NOT NULL,
 delivery_id VARCHAR(128) NOT NULL, authority_status VARCHAR(64) NOT NULL, created_at VARCHAR(64) NOT NULL,
 PRIMARY KEY (selection_effect_id), UNIQUE KEY uk_u06_delivery_authority_effect (delivery_effect_id)
);
CREATE TABLE u06_delivery_intent (
 delivery_effect_id VARCHAR(128) NOT NULL, delivery_id VARCHAR(128) NOT NULL, question_id VARCHAR(128) NOT NULL,
 content_fingerprint VARCHAR(128) NOT NULL, endpoint_ref VARCHAR(128) NOT NULL, idempotency_key VARCHAR(128) NOT NULL,
 intent_status VARCHAR(32) NOT NULL, created_at VARCHAR(64) NOT NULL, PRIMARY KEY (delivery_effect_id),
 UNIQUE KEY uk_u06_delivery_intent_id (delivery_id), UNIQUE KEY uk_u06_delivery_intent_idem (idempotency_key)
);
CREATE TABLE u06_delivery_attempt (
 attempt_id VARCHAR(180) NOT NULL, delivery_effect_id VARCHAR(128) NOT NULL, delivery_id VARCHAR(128) NOT NULL,
 attempt_status VARCHAR(64) NOT NULL, created_at VARCHAR(64) NOT NULL, PRIMARY KEY (attempt_id)
);
CREATE TABLE u06_delivery_receipt (
 receipt_id VARCHAR(180) NOT NULL, delivery_effect_id VARCHAR(128) NOT NULL, delivery_id VARCHAR(128) NOT NULL,
 receipt_status VARCHAR(64) NOT NULL, evidence_type VARCHAR(32) NOT NULL, created_at VARCHAR(64) NOT NULL,
 PRIMARY KEY (receipt_id)
);
CREATE TABLE u06_delivery_confirmation (
 confirmation_evaluation_id VARCHAR(128) NOT NULL, delivery_effect_id VARCHAR(128) NOT NULL,
 delivery_id VARCHAR(128) NOT NULL, confirmation_status VARCHAR(32) NOT NULL,
 confirmation_fingerprint VARCHAR(128) NOT NULL, created_at VARCHAR(64) NOT NULL,
 PRIMARY KEY (confirmation_evaluation_id)
);
CREATE TABLE u06_delivery_ledger (
 delivery_effect_id VARCHAR(128) NOT NULL, delivery_id VARCHAR(128) NOT NULL,
 confirmation_status VARCHAR(32) NOT NULL, canonical_fingerprint VARCHAR(128) NOT NULL,
 created_at VARCHAR(64) NOT NULL, PRIMARY KEY (delivery_effect_id)
);
