-- RELEASE CANDIDATE ONLY; not in db/migration-oracle.
-- Table structural parity only; current U06 recovery CAS runtime remains MySQL-specific.
CREATE TABLE u07_d5o2_outbox_fence (
  effect_id VARCHAR2(128 CHAR) NOT NULL,
  command_fingerprint VARCHAR2(128 CHAR) NOT NULL,
  fence_status VARCHAR2(64 CHAR) NOT NULL,
  fence_generation NUMBER(19) DEFAULT 0 NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT pk_u07_outbox_fence PRIMARY KEY (effect_id)
);
CREATE INDEX idx_u07_outbox_status ON u07_d5o2_outbox_fence (fence_status);
