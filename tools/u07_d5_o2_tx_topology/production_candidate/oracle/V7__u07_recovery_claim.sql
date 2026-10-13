-- RELEASE CANDIDATE ONLY. Not in db/migration-oracle.
-- Storage parity only; current U06DurableRecoveryAuthority is MySQL SQL-specific
-- (DATE_ADD, CURRENT_TIMESTAMP(6), INTERVAL) and MUST NOT be enabled on Oracle.
CREATE TABLE u07_d5o2_recovery_claim (
  effect_id VARCHAR2(128 CHAR) NOT NULL,
  command_fingerprint VARCHAR2(128 CHAR) NOT NULL,
  claim_status VARCHAR2(80 CHAR) NOT NULL,
  generation NUMBER(19) DEFAULT 0 NOT NULL,
  owner_token VARCHAR2(64 CHAR),
  lease_until TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL,
  CONSTRAINT pk_u07_recovery_claim PRIMARY KEY (effect_id)
);
CREATE INDEX idx_u07_recovery_expiry
  ON u07_d5o2_recovery_claim (claim_status, lease_until);
