-- RELEASE CANDIDATE ONLY. Not in db/migration; requires production Flyway baseline review.
-- MySQL 8 / InnoDB, compatible with U06DurableRecoveryAuthority SQL.
CREATE TABLE u07_d5o2_recovery_claim (
  effect_id VARCHAR(128) NOT NULL,
  command_fingerprint VARCHAR(128) NOT NULL,
  claim_status VARCHAR(80) NOT NULL,
  generation BIGINT NOT NULL DEFAULT 0,
  owner_token VARCHAR(64) NULL,
  lease_until DATETIME(6) NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (effect_id),
  KEY idx_recovery_status_expiry (claim_status, lease_until)
) ENGINE=InnoDB;
-- Never grant DELETE or DDL to recovery writer.
-- Never grant network or real delivery authority because of this table.
