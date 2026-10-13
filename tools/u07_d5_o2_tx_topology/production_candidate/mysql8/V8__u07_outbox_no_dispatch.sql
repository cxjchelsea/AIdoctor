-- D5-O2 candidate only. NOT in Flyway auto-migration. MySQL8 InnoDB.
-- Safety-only outbox: no SEND/READY state or dispatch eligibility.
CREATE TABLE u07_d5o2_outbox_fence (
  effect_id VARCHAR(128) NOT NULL,
  command_fingerprint VARCHAR(128) NOT NULL,
  fence_status VARCHAR(64) NOT NULL,
  fence_generation BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (effect_id),
  KEY idx_u07_outbox_fence_status (fence_status)
) ENGINE=InnoDB;
