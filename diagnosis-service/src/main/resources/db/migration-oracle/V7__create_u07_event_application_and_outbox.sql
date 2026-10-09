-- U07 D1 schema-only prototype. No runtime activation, data backfill or PHI.
-- Reuse existing canonical_business_event (Oracle V3); do not create a second identity ledger.
-- Application and outbox tables are intentionally non-dispatching in D1.
CREATE TABLE u07_event_application (
 event_id VARCHAR2(128 CHAR) NOT NULL,
 consultation_id VARCHAR2(128 CHAR) NOT NULL,
 question_id VARCHAR2(128 CHAR) NOT NULL,
 parent_wait_effect_id VARCHAR2(128 CHAR) NOT NULL,
 payload_digest VARCHAR2(128 CHAR) NOT NULL,
 source_event_ref VARCHAR2(128 CHAR) NOT NULL,
 source_version_ref VARCHAR2(128 CHAR) NOT NULL,
 source_state_version NUMBER(19) NOT NULL,
 business_policy_ref VARCHAR2(128 CHAR),
 trace_ref VARCHAR2(128 CHAR),
 phase VARCHAR2(32 CHAR) NOT NULL,
 decision VARCHAR2(32 CHAR),
 row_version NUMBER(19) DEFAULT 0 NOT NULL,
 owner_token VARCHAR2(128 CHAR),
 lease_expires_at TIMESTAMP,
 receipt_ref VARCHAR2(128 CHAR),
 effect_id VARCHAR2(128 CHAR),
 effect_fingerprint VARCHAR2(128 CHAR),
 created_at TIMESTAMP NOT NULL,
 updated_at TIMESTAMP NOT NULL,
 terminal_at TIMESTAMP,
 CONSTRAINT pk_u07_app PRIMARY KEY (event_id),
 CONSTRAINT uk_u07_app_effect UNIQUE (effect_id)
);
CREATE INDEX idx_u07_app_consultation ON u07_event_application (consultation_id);
CREATE INDEX idx_u07_app_wait ON u07_event_application (parent_wait_effect_id);
CREATE INDEX idx_u07_app_phase ON u07_event_application (phase, updated_at);

CREATE TABLE u07_effect_outbox (
 effect_id VARCHAR2(128 CHAR) NOT NULL,
 event_id VARCHAR2(128 CHAR) NOT NULL,
 target_type VARCHAR2(64 CHAR) NOT NULL,
 payload_ref_or_digest VARCHAR2(256 CHAR) NOT NULL,
 effect_status VARCHAR2(32 CHAR) NOT NULL,
 attempt NUMBER(19) DEFAULT 0 NOT NULL,
 owner_token VARCHAR2(128 CHAR),
 lease_expires_at TIMESTAMP,
 last_error_class VARCHAR2(128 CHAR),
 created_at TIMESTAMP NOT NULL,
 updated_at TIMESTAMP NOT NULL,
 CONSTRAINT pk_u07_outbox PRIMARY KEY (effect_id)
);
CREATE INDEX idx_u07_outbox_event ON u07_effect_outbox (event_id);
CREATE INDEX idx_u07_outbox_poll ON u07_effect_outbox (effect_status, lease_expires_at);
