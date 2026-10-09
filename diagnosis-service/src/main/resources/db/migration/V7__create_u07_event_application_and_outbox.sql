-- U07 D1 schema-only prototype. No runtime activation, data backfill or PHI.
-- Reuse existing canonical_business_event (MySQL V2); do not create a second identity ledger.
-- Application and outbox tables are intentionally non-dispatching in D1.
CREATE TABLE u07_event_application (
 event_id VARCHAR(128) NOT NULL,
 consultation_id VARCHAR(128) NOT NULL,
 question_id VARCHAR(128) NOT NULL,
 parent_wait_effect_id VARCHAR(128) NOT NULL,
 payload_digest VARCHAR(128) NOT NULL,
 source_event_ref VARCHAR(128) NOT NULL,
 source_version_ref VARCHAR(128) NOT NULL,
 source_state_version BIGINT NOT NULL,
 business_policy_ref VARCHAR(128) NULL,
 trace_ref VARCHAR(128) NULL,
 phase VARCHAR(32) NOT NULL,
 decision VARCHAR(32) NULL,
 row_version BIGINT NOT NULL DEFAULT 0,
 owner_token VARCHAR(128) NULL,
 lease_expires_at DATETIME NULL,
 receipt_ref VARCHAR(128) NULL,
 effect_id VARCHAR(128) NULL,
 effect_fingerprint VARCHAR(128) NULL,
 created_at DATETIME NOT NULL,
 updated_at DATETIME NOT NULL,
 terminal_at DATETIME NULL,
 PRIMARY KEY (event_id),
 UNIQUE KEY uk_u07_app_effect (effect_id),
 KEY idx_u07_app_consultation (consultation_id),
 KEY idx_u07_app_wait (parent_wait_effect_id),
 KEY idx_u07_app_phase (phase, updated_at)
);

CREATE TABLE u07_effect_outbox (
 effect_id VARCHAR(128) NOT NULL,
 event_id VARCHAR(128) NOT NULL,
 target_type VARCHAR(64) NOT NULL,
 payload_ref_or_digest VARCHAR(256) NOT NULL,
 effect_status VARCHAR(32) NOT NULL,
 attempt BIGINT NOT NULL DEFAULT 0,
 owner_token VARCHAR(128) NULL,
 lease_expires_at DATETIME NULL,
 last_error_class VARCHAR(128) NULL,
 created_at DATETIME NOT NULL,
 updated_at DATETIME NOT NULL,
 PRIMARY KEY (effect_id),
 KEY idx_u07_outbox_event (event_id),
 KEY idx_u07_outbox_poll (effect_status, lease_expires_at)
);
