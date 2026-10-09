# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A1 Real Runner Nomination + Owner Evidence Intake / R2 Authorization Prerequisite Review v0.1

> Review date: 2026-10-09
> Baseline: `main@86e8843197091c8c8172b7e4213537a31bdf0654` (GitHub latest repository commit at review).
> Exact governing documents: [PR #328](https://github.com/cxjchelsea/AIdoctor/pull/328) @ `3f6725f387105798d2fa4ba6cefb1b43a69a0360`; [PR #329](https://github.com/cxjchelsea/AIdoctor/pull/329) @ `c7a2772fb5d2668a78354147d79158744558bf16`; [PR #330](https://github.com/cxjchelsea/AIdoctor/pull/330) @ `e1ebaa527f95b41ec1611b2bc300872d45762882`. PR #330's conditional review blob: `747203b2db7d8252ffcc2fca174db94007702eaa`.
> **Decision: R1_REAL_RUNNER_EVIDENCE_INTAKE = BLOCKED_BY_EXTERNAL_PROVENANCE; R2_AUTHORIZATION_PREREQUISITES = NOT_READY; R2_COLLECTION = NOT_AUTHORIZED.** Repository-only assessment; no Runner nominated by this review, no Owner approved by this review, no machine accessed.

## 1. Scope and evidence provenance

Independently rechecked current main commit and PR #328/#329/#330 exact author/reviewer heads and status in GitHub. Searched repository pull requests and issues for `runner attestation`. The retrieved results were the existing design/readiness PRs #326/#327/#328 and no matching issues; **no accessible positive real-runner evidence was verified in this bounded search**. This is **not** proof that no internally managed machine exists, or that no document with different terms exists elsewhere. Organization asset inventory, signed owner records, infrastructure accounts and private host access are outside this repository-only readback.

PR #330 accepted four R2 command-profile findings as **CLOSED_CONDITIONAL_DESIGN**. Design acceptance is not a signed owner grant, not a confirmed machine inventory, not an executable attestation, and not evidence that specific preventive rules block traffic. `.github/workflows/foundation-0-verification.yml` references normal `ubuntu-latest` CI (previously pinned blob `eb80668db53c9ac497de99a774434ec2b73d62a2`), which cannot be used as an approved Tier-1 isolated runner by inference.

No external owner identity, real asset ID, signed offline manifest, signature verifier key, granted R2 collection identity, collector binary, target OS/kernel, isolation policy hash, or approved command-set SHA was provided to this review.

## 2. Concrete evidence-intake inventory (required, none fabricated)

| Item | Required verifiable evidence | Current state / rule |
|---|---|---|
| `RN-01` Runner asset | Stable **opaque asset ID**, approved nonproduction/isolated environment, inventory-system reference, independent identity readback | `NOT_RECEIVED`; never substitute CI runner label |
| `RN-02` Infrastructure owner | Named authorized infrastructure owner identity/role and scope of administration, attested by owner registry | `UNASSIGNED / NOT_VERIFIED` |
| `RN-03` Security / Foundation U01 owners | Named distinct Security reviewer and Foundation/U01 representative with current decision authority | `UNASSIGNED / NOT_VERIFIED` |
| `RN-04` Offline signed runner asset manifest | `RunnerOfflineAttestationV1` asset ID, OS/kernel/image/runtime digests, environment, issuance/expiry/revocation, issuer/key ID/signature and SHA256 | `NOT_RECEIVED / NOT_VERIFIED` |
| `RN-05` Offline signed mount/secret/policy summary | Typed least-disclosing tri-state claims, independent policy-control and absence claims; avoid raw paths | `NOT_RECEIVED / NOT_VERIFIED`; `UNKNOWN` cannot become `SAFE` |
| `RN-06` Signed audit coverage and collector mapping | Exact permitted syscall/child/FD/socket monitoring scope and proof that **collector identity differs from target** until separately bound | `NOT_RECEIVED / NOT_VERIFIED` |
| `RN-07` R2 command collection plan | Exact command argv + binary digests for R2-01..05, R2-06/07/08/09/10 offline-only, validated output schemas, redaction, error handling, sealed stdout/stderr | `DESIGN_CONDITIONALLY_ACCEPTED / RUNNER_SPECIFIC_HASHES_UNKNOWN` |
| `RN-08` Evidence retention/transfer | Signed transfer provenance, access control, location, integrity, retention/deletion policy, reviewer readback | `NOT_APPROVED` |
| `RN-09` R2 collection grant | Signed limited grant with runner/principal/scope/timebound hashes from Infrastructure + Security + Foundation/U01 | `NOT_GRANTED` |
| `RN-10` Runner validity/recheck triggers | Asset image/policy/owner, identities or time changes revoke grant; independent review on drift | `NOT_ESTABLISHED` |

If any artifact contains secrets, raw internal network layouts, hostname lists, patient identifiers or real mount paths, do **not** commit it into public GitHub PRs. In-repository readiness document should cite an opaque hash/reference and the named authorized reviewer, not reveal the sensitive content.

## 3. Strict R1 nomination and owner-evidence intake procedure (design, not executed)

1. **Infrastructure-side nomination (external human action):** authorized owner supplies opaque candidate ID, nonproduction purpose and stable inventory pointer. Owner does not assert tests passed.
2. **Security and Foundation/U01 owner eligibility:** verify owner's real role and right to sign; one identity cannot be silently inferred from PR authorship. Record decision scope and any organizational segregation-of-duties requirements.
3. **Offline artifact handoff:** obtain *existing* signed asset/mount/policy/coverage manifests through an approved offline channel. Each manifest binds the exact asset ID, issuer identity, signed digest, issuance/expiration and revocation status. Never use an unapproved network/cloud API, metadata endpoint or Docker socket to fetch missing values.
4. **Independent verification:** authorized reviewer checks issuer/key registry, asset inventory match, current signing authority, manifest integrity, expiry/revocation, allowed disclosure fields and cross-manifest ID alignment. Any absent record is `UNKNOWN`, not `NONE`.
5. **R2 collection-specific readiness:** after nomination is established, freeze exact target OS/runner command argv/binary hashes, user privileges, max export lengths, stdout/stderr/log suppression, retention, no-network stance and command behavior under failure. Commands that cannot safely control raw logs must be excluded.
6. **Separate permission decision:** only an appropriately authorized decision may issue a scoped `U07StageA1R2CollectionGrantV1` after independent readiness review. This document is no authorization; no commands should be run.

**No circular gate:** selecting a real asset and receiving existing owner-signed inventory is an R1 process. Read-only R2 may then collect additional limited properties **only after** its own grant. A future R2 observation does not retroactively prove an owner had granted access. Stage A1 code implementation, trusted SETUP, CANARY, Spring and production have separate approval gates.

## 4. Real evidence manifest envelope and acceptance predicates

```yaml
schema: U07StageA1RunnerNominationEvidenceIntakeV1
source_main_sha: 86e8843197091c8c8172b7e4213537a31bdf0654
command_profile_design_head: 3f6725f387105798d2fa4ba6cefb1b43a69a0360
independent_profile_review_head: e1ebaa527f95b41ec1611b2bc300872d45762882
runner_asset_id: UNKNOWN
runner_inventory_ref: UNAVAILABLE
environment_class: NOT_VERIFIED
infrastructure_owner_ref: UNASSIGNED
security_authority_ref: UNASSIGNED
foundation_u01_owner_ref: UNASSIGNED
offline_asset_manifest_hash_and_validated_signature: UNKNOWN
offline_mount_policy_manifest_hash_and_validated_signature: UNKNOWN
audit_coverage_manifest_hash_and_validated_signature: UNKNOWN
approved_offline_transfer_provenance: UNAPPROVED
issuer_key_current_and_revocation_check: NOT_VERIFIED
collector_vs_target_security_context_mapping: NOT_VERIFIED
r2_command_and_output_allowlist_sha256: UNKNOWN
evidence_acl_retention_policy: UNAPPROVED
independent_reviewer_identity_and_readback_ref: UNASSIGNED
r2_grant_ref: NOT_GRANTED
r2_grant_validity_window: UNSET
source_of_truth: REPOSITORY_ONLY_NO_MACHINE_EVIDENCE
status: NOT_READY
```

**Predicates:** `RUNNER_NOMINATED` requires independently validated RN-01..03 with nonproduction proof, not a GitHub label. `OWNER_EVIDENCE_ACCEPTED` additionally requires RN-04..06, RN-08, RN-10 valid and matching that runner. `R2_GRANT_READINESS` requires RN-07 with exact runner-bound command profile, named collector and all authorized reviewers; RN-09 actual grant remains a separate decision. Neither `RUNNER_NOMINATED` nor `R2_GRANT_READINESS` entails `ISOLATION_POLICY_PASS`.

## 5. Gate-by-gate prerequisite review

| Gate | Required acceptance | Evidenced now | Decision |
|---|---|---|---|
| `R1-G01` | Exact main/PR #328/#330 provenance | GitHub exact heads | PASS_SOURCE_ONLY |
| `R1-G02` | Actual approved nonproduction asset ID + inventory source | No accepted record observed | **BLOCKED** |
| `R1-G03` | Responsible Infrastructure/Security/Foundation-U01 owners + independent authority checks | No accepted signatures | **BLOCKED** |
| `R1-G04` | Offline runner/mount/policy/audit artifact content digests and signatures | Not supplied/verified | **BLOCKED** |
| `R1-G05` | Controlled artifact transfer, key validity and ACL/retention | Not supplied/verified | **BLOCKED** |
| `R1-G06` | Exact collector-vs-target context and R2 command/output hashes | Conceptual contract only | NOT_PROVEN |
| `R1-G07` | R2-only independent permission scoped to named asset | No grant | NOT_GRANTED |
| `R1-G08` | No unapproved host/CI/scripts/SETUP/CANARY/medical/production/merge | Contract prohibition preserved | PASS_CONTRACT_ONLY |

The earlier `RF-U07-A1-R2-IR-01..04` remain `CLOSED_CONDITIONAL_DESIGN` by PR #330. Their executable, platform-bound and human evidence requirements stay pending.

## 6. Findings, block classification and next actionable gate

| ID | Classification | Missing external fact | Current state |
|---|---|---|---|
| `BF-U07-A1-R2-01` | EXTERNAL PROVENANCE | Real named runner, segregated asset ID, verified Infrastructure owner | OPEN |
| `BF-U07-A1-R2-02` | EXTERNAL AUTHORITY | Human Security/Foundation-U01 and Infrastructure signoff, scoped R2 grant | OPEN |
| `RF-U07-A1-RN-01` | REQUIRED EVIDENCE INTAKE | Existing offline signed runner asset/mount/policy/audit inventory with independent readback | REQUIRED |
| `RF-U07-A1-RN-02` | REQUIRED COLLECTOR BINDING | Concrete target-bound R2 command binaries/argv/UID/logging/retention/expiry and no-network policy | REQUIRED |
| `RF-U07-A1-RN-03` | REQUIRED GOVERNANCE | Human signers, key revocation validation, asset authority and separation of duties | REQUIRED |
| `SA-BF-01/02` | RUNTIME ISOLATION/CANARY | Still no actual preventive-control and measured canary evidence | OPEN |
| `SA-BF-03/04` | STAGE A2 | Oracle/classpath/controlled amendment outside this A1 scope | OPEN |

**Next useful event:** actual owner supplies RN-01..03 and the minimal signed offline inventory/provenance. Then perform `Stage A1 Runner Nomination Evidence Intake + Exact Runner-Bound R2 Grant Readiness Re-Evaluation` on those specific evidence refs. If unavailable, return `R2_NOT_READY` without repeated document-only cycles. A later independent audit must precede any explicit scoped R2 execution authorization.

## 7. Formal decision (no invented actual runner)

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
STAGE_A1_REAL_RUNNER_NOMINATION_OWNER_EVIDENCE_INTAKE_R2_PREREQUISITE_REVIEW
= NOT_READY / EXTERNAL_EVIDENCE_REQUIRED

SOURCE_MAIN = 86e8843197091c8c8172b7e4213537a31bdf0654
PR328_COMMAND_PROFILE_HEAD = 3f6725f387105798d2fa4ba6cefb1b43a69a0360
PR330_INDEPENDENT_REVIEW_HEAD = e1ebaa527f95b41ec1611b2bc300872d45762882

RUNNER_ASSET_ID = UNKNOWN / NOT_NOMINATED
INFRASTRUCTURE_OWNER = UNASSIGNED
SECURITY_AND_FOUNDATION_U01_OWNER = UNASSIGNED
OWNER_SIGNED_OFFLINE_EVIDENCE = NOT_RECEIVED / NOT_VERIFIED
SIGNED_R2_GRANT = NOT_GRANTED
R2_AUTHORIZATION_PREREQUISITES = NOT_READY
R2_COLLECTION_EXECUTION = NOT_AUTHORIZED
R2_HOST_COMMANDS_EXECUTED = NO
BF-U07-A1-R2-01/02 = OPEN
SA-BF-01/02 = OPEN
SA-BF-03/04 = OPEN / STAGE_A2
A1_SIX_FILE_CODE_IMPLEMENTATION = NOT_AUTHORIZED
SETUP_AND_CANARY_EXECUTION = NOT_AUTHORIZED
SPRING_CONTEXT = NOT_AUTHORIZED
FOUNDATION_AUDIT = NOT_PASSED
U07_IMPLEMENTATION = NOT_AUTHORIZED
MERGE_AUTHORIZATION = NOT_GRANTED
```

This review contains **source-repository evidence only** and an external evidence-intake contract; it does not claim full organizational evidence discovery, real runner verification or authorization by a human Security/Infrastructure owner.
