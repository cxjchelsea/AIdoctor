-- RELEASE CANDIDATE ONLY. NOT part of default Flyway startup.
-- Promote only after authenticated historical V1-V6 checksums, DBA sign-off,
-- MySQL8 compatibility proof, schema-history review and rollback rehearsal.
-- The evidence tables never grant outbound delivery or clinical authority.
CREATE TABLE u07_f3_owner_evidence (
 consultation_id VARCHAR(64) NOT NULL, cdp_id VARCHAR(64) NOT NULL,
 cdp_version INT NOT NULL, f3_effect_id VARCHAR(128) NOT NULL,
 owner_commit_ref VARCHAR(128) NOT NULL, state VARCHAR(64) NOT NULL,
 PRIMARY KEY (consultation_id, cdp_id, cdp_version)
) ENGINE=InnoDB;
CREATE TABLE u07_f3_barrier_evidence (
 consultation_id VARCHAR(64) NOT NULL, cdp_id VARCHAR(64) NOT NULL,
 cdp_version INT NOT NULL, f3_effect_id VARCHAR(128) NOT NULL,
 owner_commit_ref VARCHAR(128) NOT NULL, barrier_ref VARCHAR(128) NOT NULL,
 outcome VARCHAR(64) NOT NULL,
 PRIMARY KEY (consultation_id, cdp_id, cdp_version)
) ENGINE=InnoDB;
CREATE TABLE u07_f3_revalidation_evidence (
 consultation_id VARCHAR(64) NOT NULL, cdp_id VARCHAR(64) NOT NULL,
 cdp_version INT NOT NULL, f3_effect_id VARCHAR(128) NOT NULL,
 owner_commit_ref VARCHAR(128) NOT NULL, barrier_ref VARCHAR(128) NOT NULL,
 revalidation_ref VARCHAR(128) NOT NULL, outcome VARCHAR(64) NOT NULL,
 route_source_ref VARCHAR(128) NOT NULL,
 route_authorization_ref VARCHAR(128) NOT NULL,
 routing_policy_version VARCHAR(128) NOT NULL,
 PRIMARY KEY (consultation_id, cdp_id, cdp_version)
) ENGINE=InnoDB;
CREATE TABLE u07_a1_attestation (
 consultation_id VARCHAR(64) NOT NULL, cdp_id VARCHAR(64) NOT NULL,
 cdp_version INT NOT NULL, f3_effect_id VARCHAR(128) NOT NULL,
 barrier_ref VARCHAR(128) NOT NULL, revalidation_ref VARCHAR(128) NOT NULL,
 route_source_ref VARCHAR(128) NOT NULL,
 route_authorization_ref VARCHAR(128) NOT NULL,
 routing_policy_version VARCHAR(128) NOT NULL,
 PRIMARY KEY (consultation_id, cdp_id, cdp_version)
) ENGINE=InnoDB;
CREATE TABLE u07_question_delivery_evidence (
 consultation_id VARCHAR(64) NOT NULL, cdp_id VARCHAR(64) NOT NULL,
 question_id VARCHAR(64) NOT NULL, selection_effect_id VARCHAR(128) NOT NULL,
 delivery_id VARCHAR(128) NOT NULL, delivery_effect_id VARCHAR(128) NOT NULL,
 confirmation_ref VARCHAR(128) NOT NULL,
 PRIMARY KEY (consultation_id, cdp_id, question_id)
) ENGINE=InnoDB;
CREATE TABLE u07_question_wait_evidence (
 consultation_id VARCHAR(64) NOT NULL, cdp_id VARCHAR(64) NOT NULL,
 question_id VARCHAR(64) NOT NULL, wait_effect_ref VARCHAR(128) NOT NULL,
 checkpoint_ref VARCHAR(128) NOT NULL, eligibility_ref VARCHAR(128) NOT NULL,
 PRIMARY KEY (consultation_id, cdp_id, question_id)
) ENGINE=InnoDB;
-- Production consumers require independently authenticated evidence.
-- Use INSERT/SELECT scoped grants; never grant UPDATE/DELETE on immutable receipts.
