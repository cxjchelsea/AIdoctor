-- Disposable tool database only. Source/Foundation DDL loaded independently without copying.
CREATE TABLE f8_guard (scope_key CHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,lock_token BIGINT NOT NULL DEFAULT 0) ENGINE=InnoDB;
CREATE TABLE f8_action (
 scope_key CHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 tenant VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,consultation VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,actor VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
 epoch BIGINT NOT NULL,read_allowed BOOLEAN NOT NULL,finalize_allowed BOOLEAN NOT NULL,
 issuer VARCHAR(64) NOT NULL,policy VARCHAR(64) NOT NULL,manifest CHAR(64) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE f8_owner (
 ref VARCHAR(128) COLLATE utf8mb4_bin PRIMARY KEY,kind VARCHAR(32) NOT NULL,
 env VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,profile VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
 tenant VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,consultation VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
 actor VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,question VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,parent_wait VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
 pending_ref VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,eligibility_ref VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
 version BIGINT NOT NULL,clinical_version BIGINT NOT NULL,state VARCHAR(32) NOT NULL,current_wait VARCHAR(128) COLLATE utf8mb4_bin,
 deadline DATETIME(6) NOT NULL,issuer VARCHAR(64) NOT NULL,policy VARCHAR(64) NOT NULL,manifest CHAR(64) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE f8_decision (
 id CHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,canonical_id VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
 contract VARCHAR(64) NOT NULL,fingerprint CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 env VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,profile VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,tenant VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
 consultation VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,question VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,parent_wait VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
 wait_key CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 verdict VARCHAR(16) NOT NULL,winner_id CHAR(64) CHARACTER SET ascii COLLATE ascii_bin,sampled_at DATETIME(6) NOT NULL,predicate_at DATETIME(6) NOT NULL,
 owner_versions VARCHAR(128) NOT NULL,action_epoch BIGINT NOT NULL,
 UNIQUE(canonical_id,contract),CHECK(verdict IN ('ACCEPTED','DUPLICATE','EXPIRED','REJECTED')),CHECK(sampled_at=predicate_at)
) ENGINE=InnoDB;
CREATE TABLE f8_claim (
 wait_key CHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
 env VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,profile VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,tenant VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
 consultation VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,question VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,parent_wait VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
 canonical_id VARCHAR(128) COLLATE utf8mb4_bin,decision_id CHAR(64) CHARACTER SET ascii COLLATE ascii_bin,generation BIGINT NOT NULL,
 UNIQUE(env,profile,tenant,consultation,question,parent_wait)
) ENGINE=InnoDB;
CREATE TABLE f8_applied (
 decision_id CHAR(64) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,wait_key CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 canonical_id VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,digest CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,generation BIGINT NOT NULL,
 issuer VARCHAR(64) NOT NULL,policy VARCHAR(64) NOT NULL,manifest CHAR(64) NOT NULL
) ENGINE=InnoDB;
CREATE USER 'f8_adapter'@'%' IDENTIFIED BY 'synthetic-f8-only';
CREATE USER 'f8_owner_issuer'@'%' IDENTIFIED BY 'synthetic-f8-owner-only';
GRANT SELECT ON u07_f8_adapter.* TO 'f8_adapter'@'%';
GRANT INSERT ON u07_f8_adapter.f8_decision TO 'f8_adapter'@'%';
GRANT INSERT,UPDATE ON u07_f8_adapter.f8_claim TO 'f8_adapter'@'%';
GRANT UPDATE(lock_token) ON u07_f8_adapter.f8_guard TO 'f8_adapter'@'%';
GRANT SELECT ON u07_f8_adapter.* TO 'f8_owner_issuer'@'%';
GRANT INSERT,UPDATE ON u07_f8_adapter.f8_owner TO 'f8_owner_issuer'@'%';
GRANT INSERT,UPDATE ON u07_f8_adapter.f8_action TO 'f8_owner_issuer'@'%';
GRANT INSERT,UPDATE ON u07_f8_adapter.f8_guard TO 'f8_owner_issuer'@'%';
GRANT INSERT ON u07_f8_adapter.f8_applied TO 'f8_owner_issuer'@'%';
