-- RELEASE CANDIDATE ONLY; not auto-scanned by Oracle Flyway.
-- Schema parity only; runtime remains MySQL8-specific until separately tested on Oracle.
-- No production clinical authority or patient outbound dispatch is enabled.
CREATE TABLE u07_f3_owner_evidence (
  consultation_id VARCHAR2(64 CHAR) NOT NULL,
  cdp_id VARCHAR2(64 CHAR) NOT NULL,
  cdp_version NUMBER(10) NOT NULL,
  f3_effect_id VARCHAR2(128 CHAR) NOT NULL,
  owner_commit_ref VARCHAR2(128 CHAR) NOT NULL,
  state VARCHAR2(64 CHAR) NOT NULL,
  CONSTRAINT pk_f3_owner_evidence PRIMARY KEY (consultation_id, cdp_id, cdp_version)
);
CREATE TABLE u07_f3_barrier_evidence (
  consultation_id VARCHAR2(64 CHAR) NOT NULL,
  cdp_id VARCHAR2(64 CHAR) NOT NULL,
  cdp_version NUMBER(10) NOT NULL,
  f3_effect_id VARCHAR2(128 CHAR) NOT NULL,
  owner_commit_ref VARCHAR2(128 CHAR) NOT NULL,
  barrier_ref VARCHAR2(128 CHAR) NOT NULL,
  outcome VARCHAR2(64 CHAR) NOT NULL,
  CONSTRAINT pk_f3_barrier_evidence PRIMARY KEY (consultation_id, cdp_id, cdp_version)
);
CREATE TABLE u07_f3_revalidation_evidence (
  consultation_id VARCHAR2(64 CHAR) NOT NULL,
  cdp_id VARCHAR2(64 CHAR) NOT NULL,
  cdp_version NUMBER(10) NOT NULL,
  f3_effect_id VARCHAR2(128 CHAR) NOT NULL,
  owner_commit_ref VARCHAR2(128 CHAR) NOT NULL,
  barrier_ref VARCHAR2(128 CHAR) NOT NULL,
  revalidation_ref VARCHAR2(128 CHAR) NOT NULL,
  outcome VARCHAR2(64 CHAR) NOT NULL,
  route_source_ref VARCHAR2(128 CHAR) NOT NULL,
  route_authorization_ref VARCHAR2(128 CHAR) NOT NULL,
  routing_policy_version VARCHAR2(128 CHAR) NOT NULL,
  CONSTRAINT pk_f3_revalidation_evidence PRIMARY KEY (consultation_id, cdp_id, cdp_version)
);
CREATE TABLE u07_a1_attestation (
  consultation_id VARCHAR2(64 CHAR) NOT NULL,
  cdp_id VARCHAR2(64 CHAR) NOT NULL,
  cdp_version NUMBER(10) NOT NULL,
  f3_effect_id VARCHAR2(128 CHAR) NOT NULL,
  barrier_ref VARCHAR2(128 CHAR) NOT NULL,
  revalidation_ref VARCHAR2(128 CHAR) NOT NULL,
  route_source_ref VARCHAR2(128 CHAR) NOT NULL,
  route_authorization_ref VARCHAR2(128 CHAR) NOT NULL,
  routing_policy_version VARCHAR2(128 CHAR) NOT NULL,
  CONSTRAINT pk_a1_attestation PRIMARY KEY (consultation_id, cdp_id, cdp_version)
);
CREATE TABLE u07_question_delivery_evidence (
  consultation_id VARCHAR2(64 CHAR) NOT NULL,
  cdp_id VARCHAR2(64 CHAR) NOT NULL,
  question_id VARCHAR2(64 CHAR) NOT NULL,
  selection_effect_id VARCHAR2(128 CHAR) NOT NULL,
  delivery_id VARCHAR2(128 CHAR) NOT NULL,
  delivery_effect_id VARCHAR2(128 CHAR) NOT NULL,
  confirmation_ref VARCHAR2(128 CHAR) NOT NULL,
  CONSTRAINT pk_question_delivery_evidenc PRIMARY KEY (consultation_id, cdp_id, question_id)
);
CREATE TABLE u07_question_wait_evidence (
  consultation_id VARCHAR2(64 CHAR) NOT NULL,
  cdp_id VARCHAR2(64 CHAR) NOT NULL,
  question_id VARCHAR2(64 CHAR) NOT NULL,
  wait_effect_ref VARCHAR2(128 CHAR) NOT NULL,
  checkpoint_ref VARCHAR2(128 CHAR) NOT NULL,
  eligibility_ref VARCHAR2(128 CHAR) NOT NULL,
  CONSTRAINT pk_question_wait_evidence PRIMARY KEY (consultation_id, cdp_id, question_id)
);
