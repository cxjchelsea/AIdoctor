-- Disposable verification schema only; NOT a Flyway migration.
CREATE TABLE u07_test_permission_epoch (
 tenant_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 epoch BIGINT NOT NULL, allowed TINYINT NOT NULL CHECK (allowed IN (0,1))
) ENGINE=InnoDB;
CREATE TABLE u07_test_authority_record (
 record_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 record_type VARCHAR(32) NOT NULL, action_id VARCHAR(64) NOT NULL, issuer_id VARCHAR(128) NOT NULL,
 policy_id VARCHAR(128) NOT NULL, manifest_digest CHAR(64) NOT NULL,
 tenant_id VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 permission_epoch BIGINT NOT NULL, source_token VARCHAR(128) NOT NULL,
 storage_key VARCHAR(128) NOT NULL, binding_bytes BLOB NOT NULL,
 binding_fingerprint CHAR(64) NOT NULL, answer_cipher BLOB NULL,
 key_ref VARCHAR(64) NULL, occurred_at VARCHAR(64) NOT NULL,
 FOREIGN KEY (tenant_id) REFERENCES u07_test_permission_epoch(tenant_id)
) ENGINE=InnoDB;
CREATE TABLE u07_canonical_event_binding (
 canonical_event_id VARCHAR(128) NOT NULL PRIMARY KEY,
 storage_idempotency_key VARCHAR(128) NOT NULL UNIQUE,
 event_type VARCHAR(64) NOT NULL, consultation_id VARCHAR(128) NOT NULL,
 tenant_id VARCHAR(128) NOT NULL, binding_fingerprint CHAR(64) NOT NULL,
 payload_digest VARCHAR(128) NOT NULL, canonical_binding_bytes BLOB NOT NULL,
 synthetic_answer_bytes BLOB NULL, answer_payload_digest CHAR(64) NULL,
 key_ref VARCHAR(64) NULL, source_record_ref VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 occurred_at VARCHAR(64) NOT NULL, target_answer_event_id VARCHAR(128) NULL,
 FOREIGN KEY (canonical_event_id) REFERENCES canonical_business_event(event_id),
 FOREIGN KEY (source_record_ref) REFERENCES u07_test_authority_record(record_id),
 FOREIGN KEY (target_answer_event_id) REFERENCES canonical_business_event(event_id),
 CHECK ((event_type='USER_ANSWER' AND synthetic_answer_bytes IS NOT NULL AND answer_payload_digest IS NOT NULL
         AND key_ref IS NOT NULL AND target_answer_event_id IS NULL)
     OR (event_type='RESUME_REQUEST' AND synthetic_answer_bytes IS NULL AND answer_payload_digest IS NULL
         AND key_ref IS NULL AND target_answer_event_id IS NOT NULL))
) ENGINE=InnoDB;
CREATE USER 'synthetic_issuer'@'%' IDENTIFIED BY 'synthetic-only-issuer';
CREATE USER 'synthetic_adapter'@'%' IDENTIFIED BY 'synthetic-only-adapter';
GRANT SELECT,INSERT ON u07_source_binding.u07_test_authority_record TO 'synthetic_issuer'@'%';
GRANT SELECT,INSERT,UPDATE ON u07_source_binding.u07_test_permission_epoch TO 'synthetic_issuer'@'%';
GRANT SELECT ON u07_source_binding.u07_test_authority_record TO 'synthetic_adapter'@'%';
GRANT SELECT ON u07_source_binding.u07_test_permission_epoch TO 'synthetic_adapter'@'%';
GRANT SELECT,INSERT ON u07_source_binding.canonical_business_event TO 'synthetic_adapter'@'%';
GRANT SELECT,INSERT ON u07_source_binding.u07_canonical_event_binding TO 'synthetic_adapter'@'%';
