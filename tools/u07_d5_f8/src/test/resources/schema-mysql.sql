CREATE TABLE guard (id VARCHAR(40) PRIMARY KEY, version BIGINT NOT NULL, lock_token BIGINT NOT NULL DEFAULT 0);
CREATE TABLE owner_fact (
 id VARCHAR(40) PRIMARY KEY, scope_id VARCHAR(40) NOT NULL, version BIGINT NOT NULL,
 historical_wait VARCHAR(40) NOT NULL, current_wait VARCHAR(40), digest VARCHAR(40) NOT NULL,
 deadline DATETIME(6) NOT NULL, terminal VARCHAR(20), delivered BOOLEAN NOT NULL,
 authority_complete BOOLEAN NOT NULL
);
CREATE TABLE decision (
 id VARCHAR(40) PRIMARY KEY, answer_id VARCHAR(40) NOT NULL UNIQUE, scope_id VARCHAR(40) NOT NULL,
 wait_id VARCHAR(40) NOT NULL, digest VARCHAR(40) NOT NULL, verdict VARCHAR(20) NOT NULL,
 sampled_at DATETIME(6) NOT NULL, predicate_at DATETIME(6) NOT NULL,
 owner_version BIGINT NOT NULL, winner_id VARCHAR(40), CHECK (sampled_at=predicate_at)
);
CREATE TABLE wait_claim (
 wait_id VARCHAR(40) PRIMARY KEY, scope_id VARCHAR(40) NOT NULL,
 answer_id VARCHAR(40), decision_id VARCHAR(40), generation BIGINT NOT NULL
);
CREATE TABLE applied_evidence (
 decision_id VARCHAR(40) PRIMARY KEY, scope_id VARCHAR(40) NOT NULL,
 wait_id VARCHAR(40) NOT NULL, digest VARCHAR(40) NOT NULL, generation BIGINT NOT NULL,
 issuer VARCHAR(40) NOT NULL
);
CREATE USER 'f8_consumer'@'%' IDENTIFIED BY 'synthetic-consumer-only';
GRANT SELECT ON u07_f8.* TO 'f8_consumer'@'%';
GRANT INSERT ON u07_f8.decision TO 'f8_consumer'@'%';
GRANT INSERT, UPDATE ON u07_f8.wait_claim TO 'f8_consumer'@'%';

-- MySQL FOR UPDATE needs SELECT plus UPDATE privilege; no owner/version mutation grant.
GRANT UPDATE (lock_token) ON u07_f8.guard TO 'f8_consumer'@'%';
