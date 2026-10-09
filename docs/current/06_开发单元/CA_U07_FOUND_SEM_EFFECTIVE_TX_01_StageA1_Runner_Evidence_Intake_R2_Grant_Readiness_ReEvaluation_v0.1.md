# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A1 Runner Nomination Evidence Intake + Exact Runner-Bound R2 Grant Readiness Re-Evaluation v0.1

> Review date: 2026-10-09
> GitHub repository: `cxjchelsea/AIdoctor`
> Exact baseline: `main@86e8843197091c8c8172b7e4213537a31bdf0654` (latest commit seen on readback).
> Prior intake review: [PR #331](https://github.com/cxjchelsea/AIdoctor/pull/331) exact HEAD `6c9afc60262943819bb58aba684e564a5ca1ed34`, report blob `7b9749e758e9de2229357e8010187bb7adc99647`.
> Current command profile: [PR #328](https://github.com/cxjchelsea/AIdoctor/pull/328) exact HEAD `3f6725f387105798d2fa4ba6cefb1b43a69a0360` and [PR #330](https://github.com/cxjchelsea/AIdoctor/pull/330) exact HEAD `e1ebaa527f95b41ec1611b2bc300872d45762882`; previous design-only four findings `CLOSED_CONDITIONAL_DESIGN`.
> **Decision: NOT_READY / EXTERNAL_OWNER_AND_RUNNER_EVIDENCE_REQUIRED / R2_NOT_AUTHORIZED.** This is a bounded GitHub-source follow-up and formal evidence-intake re-evaluation, not a real host attestation.

## 1. Re-evaluation method and limits

Independently rechecked repository `main` and exact PR #331. Searched relevant GitHub pull requests for `runner nomination`, `runner asset`, `R2 collection grant`, `owner signed` and `attestation`, and issues for the first three terms. Results were principally existing PRs #326–#331 on proposed runner designs/readiness; related Tier-0 source attestation PRs #308/#309 and unrelated historical PRs do not prove Stage A1 OS isolation, a selected runner or Owner grant. **No independently verifiable new Stage A1 runner/asset/owner/authorization artifact was identified in these reviewed sources.** This is a bounded repository search, NOT an assertion that no asset exists in a separate infrastructure inventory, connected service or off-repository organizational records.

No host/container/VM is accessed; no credentials, live API, Runner commands, `sudo`, `findmnt`, network, Spring, JDBC, CI, synthetic setup/canary, PHI or production touched. No proposed execution authority is granted.

## 2. Intake comparison: RN-01..10 against actual evidence

| Intake | Needed fact / admissible provenance | New accepted evidence after PR #331? | Status |
|---|---|---|---|
| RN-01 | Exact opaque nonproduction Runner asset ID and stable independent inventory reference | NONE VERIFIED | `NOT_NOMINATED` |
| RN-02 | Named responsible Infrastructure Owner and verified ability to grant access | NONE VERIFIED | `OWNER_UNKNOWN` |
| RN-03 | Separate Security and Foundation/U01 authorized reviewers | NONE VERIFIED | `OWNER_UNKNOWN` |
| RN-04 | Existing offline signed Runner OS/kernel/image/runtime asset manifest bound to RN-01 | NONE VERIFIED | `NOT_RECEIVED` |
| RN-05 | Signed least-disclosing mount/secrets/LSM/network policy claims with no raw paths | NONE VERIFIED | `NOT_RECEIVED` |
| RN-06 | Signed independent audit/attempt coverage and collector-to-target identity boundary | NONE VERIFIED | `NOT_RECEIVED` |
| RN-07 | Runner-bound fixed binary digest/argv, identity, allowable output fields, stderr/log suppression | DESIGN CONTRACT ONLY, no exact machine | `NOT_FROZEN` |
| RN-08 | Approved offline transfer integrity, issuer signatures/expiry/revocation and retention/ACL | NONE VERIFIED | `NOT_APPROVED` |
| RN-09 | Exact asset/collector/scope/timebound signed Infrastructure/Security/Foundation-U01 R2 grant | NONE VERIFIED | `NOT_GRANTED` |
| RN-10 | Asset change/owner revocation re-attestation conditions and effective owner | DESIGN ONLY, no bound actual asset | `NOT_ESTABLISHED` |

**Truth boundary:** a GitHub Draft PR, AI-generated checklist, pseudo-YAML signature field, kernel version from an arbitrary host, or ordinary `ubuntu-latest` CI job is NOT sufficient proof of any RN-01..09. Do not write `ZERO`, `NO_SECRETS`, `PASS`, `SAFE`, `SIGNED` in a data field whose source remains UNKNOWN. A real evidence envelope shall only carry opaque content hashes/approved references in the public repository, not real sensitive host paths, identity documents, API tokens or secret values.

## 3. Exact Runner-bound R2 grant predicate

Define readiness `GRANT_READY` **only if** each of the following is independently verified against the *same* runner asset ID:

1. `runner_id`, real environment classification and verified authorized infrastructure owner (RN-01/02), plus named Security/Foundation-U01 authority (RN-03);
2. signed *existing* offline asset, mount/policy and audit manifest with issuer trust, expiry/revocation checks and asset-ID match (RN-04/05/06);
3. approved fixed argv/binary/collector UID, exact no-shell/no-network process boundary, no-raw-stdout/stderr/log, bounded output schemas and approved collection storage, ACL, retention and independent readback (RN-07/08);
4. a **separate** R2-only authorization decision must bind exact runner/policy/image digest, permitted commands, invoking principal, validity window and grant signers (RN-09), with revocation and drift rules (RN-10).

**Authorization readiness vs actual grant:** once evidence is received, an independent review can return `R2_GRANT_READY`, but **collection remains prohibited until a real authorized grant is issued and separately verified**. No R2 command runs merely because a plan or design PASSES. If missing prerequisites, output `NOT_READY` and stop; do not request privileged API access or invent a stand-in VM.

## 4. Gate re-evaluation

| Gate | Rule | Decision |
|---|---|---|
| `R1-G01` | Main/PR #328/#330/#331 source provenance exact SHA bound | `PASS_SOURCE_ONLY` |
| `R1-G02` | Real nominated nonproduction asset with independently readable inventory | `BLOCKED_NO_ASSET` |
| `R1-G03` | Authorized distinct Owners and signoff | `BLOCKED_NO_OWNER` |
| `R1-G04` | Actual offline signatures and asset-policy hashes | `BLOCKED_NO_EVIDENCE` |
| `R1-G05` | Signature chain/transfer/retention/ACL independently accepted | `BLOCKED_NO_EVIDENCE` |
| `R1-G06` | Exact runner-bound collector/target policy and command digests | `NOT_PROVEN` |
| `R1-G07` | R2 limited human Owner permission and exact validity | `NOT_GRANTED` |
| `R1-G08` | Forbidden host/CI/Spring/DB/canary/PHI/merge changes excluded | `PASS_CONTRACT_ONLY` |

Earlier `RF-U07-A1-R2-IR-01..04 = CLOSED_CONDITIONAL_DESIGN`; this is **not** a physical runner approval or command-level execution grant. `BF-U07-A1-R2-01/02` and `SA-BF-01/02` remain open. Stage A2 `SA-BF-03/04` are out of scope.

## 5. Required next real-world inputs and stopping rule

**Responsible actors:** a real Infrastructure Owner nominates the sandbox-only nonproduction asset; organizational Security and Foundation/U01 grant authorities verify and issue independent signatures/approval if and when justified. An authorized reviewer independently validates the asset register and signed manifests; ChatGPT/GitHub code-review artifacts cannot exercise their institutional authority.

**Minimum to reopen this gate:** provide through approved secure channels an *opaque asset-ID inventory reference, named responsible Owner proof and offline signed attestation provenance*. Subsequently pin exact R2 command/output/digest/collector scope and seek a separate grant authorization. If these artifacts are inaccessible, **stop the Stage A1 R2 workflow at `NOT_READY` rather than initiate repeated document-only review cycles**. It remains possible to work on separately authorized nonclinical tasks outside this R2 gate, but this review confers no permission to change Stage A1 files.

Next conditional action (only after actual evidence arrives): `Stage A1 Runner Nomination Evidence Intake Re-Evaluation (with concrete Owner-signed asset evidence)`, followed by a separately framed `Exact Runner-Bound R2 Collection Authorization Decision`. No authorization/permission is implied by this report.

## 6. Formal conclusion

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
STAGE_A1_RUNNER_NOMINATION_EVIDENCE_INTAKE_EXACT_RUNNER_BOUND_R2_GRANT_READINESS_RE_EVALUATION
= NOT_READY / EXTERNAL_OWNER_AND_RUNNER_EVIDENCE_REQUIRED

SOURCE_MAIN = 86e8843197091c8c8172b7e4213537a31bdf0654
PR331_HEAD = 6c9afc60262943819bb58aba684e564a5ca1ed34
PR328_COMMAND_PROFILE_HEAD = 3f6725f387105798d2fa4ba6cefb1b43a69a0360
PR330_INDEPENDENT_PROFILE_REVIEW_HEAD = e1ebaa527f95b41ec1611b2bc300872d45762882

RN-01..10 = NO_NEW_ADMISSIBLE_EVIDENCE_VERIFIED
R1-G01 = PASS_SOURCE_ONLY
R1-G02..07 = NOT_PASSED
R1-G08 = PASS_CONTRACT_ONLY
REAL_RUNNER_NOMINATION = NOT_VERIFIED
OWNER_APPROVAL = NOT_VERIFIED
OFFLINE_SIGNED_RUNNER_ATTESTATION = NOT_RECEIVED
EXACT_RUNNER_BOUND_R2_GRANT_READINESS = NOT_READY
R2_COLLECTION_AUTHORIZATION = NOT_GRANTED
R2_HOST_COMMANDS_EXECUTED = NO
BF-U07-A1-R2-01/02 = OPEN
SA-BF-01/02 = OPEN
SA-BF-03/04 = OPEN / STAGE_A2
A1_SIX_FILE_IMPLEMENTATION_AUTHORIZATION = NOT_GRANTED
A1_SETUP_AND_CANARY_EXECUTION = NOT_AUTHORIZED
STAGE_B_SPRING_DIAGNOSTIC = NOT_AUTHORIZED
FOUNDATION_AUDIT = NOT_PASSED
U07_IMPLEMENTATION = NOT_AUTHORIZED
MERGE_AUTHORIZATION = NOT_GRANTED
```

No executable files, source code, CI or clinical interfaces were changed; no merge. This analytical re-evaluation does not constitute human institutional attestation.
