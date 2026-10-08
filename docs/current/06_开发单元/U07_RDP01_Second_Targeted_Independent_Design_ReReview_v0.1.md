# U07-RDP-01 Second Targeted Independent Design Re-Review v0.1

> Date: 2026-10-08
> Target: PR #266 (U07-RDP-01)
> **Reviewed exact head:** `b7545adb0ef93f51caac415d4cced3612ece6927`
> Previous remediated head: `2ee68ca1f15467ea942915c8ae8dcb05637c8a22`
> Reference review: PR #268 (`REVISE_REQUIRED`, BF-01 CLOSED, BF-02/BF-03 OPEN)
> Integration reference: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> Verdict: **REVISE_REQUIRED / BF-02 STILL OPEN; BF-03 CONDITIONALLY RESOLVED AS EXPLICIT UPSTREAM BLOCKER**.
> No implementation, merge, production, PROFILE-A, PHI or patient traffic authorization.

## 1. Exact-head comparison and method

Verified GitHub commit comparison: one commit ahead, zero behind; **one markdown file** modified (`U07_RDP01_Consumer_Inbound_Event_Admission_Contract_v0.1.md`), **150 additions / 5 deletions**. No runtime code or test modification in this remediation.

Independently inspected §4.3.1–§4.3.3 and §6.2.2 and compared with:
- `CanonicalBusinessEventLedger.java`, `CanonicalBusinessEventRecord.java`, `CanonicalBusinessEventRepository.java`;
- `FoundationRuntimeBaseTest.java`, which treats Foundation payload_digest as an opaque string, including `sha256:abc`;
- MySQL `migration/V2__create_clinical_runtime_foundation.sql` and Oracle `migration-oracle/V3__create_clinical_runtime_foundation.sql`;
- MySQL/Oracle U06 `V6__add_u06_wait_runtime.sql`;
- `U06WaitCoordinator.java`, `U07ResumeEligibilityProjector.java`, `RuntimeThreadWaitTransitionService.java`, `RuntimeWaitCheckpointRecord.java`, `ConsultationWaitEffectRecord.java`, and `JdbcU06GovernedExecutionTraceStore.java`.

The main tree inspection was untruncated at 2,634 entries, but a tree and sampled source files are **not equivalent** to a repository-wide symbol/reference search.

## 2. BF-U07-RDP01-IR-01 — previously CLOSED

**Status: CLOSED / UNCHANGED.** Original event ID and request alias, 21 protected fingerprint fields and deterministic key derivation remain separated. No new change contradicts the first re-review.

## 3. BF-U07-RDP01-IR-02 — Foundation compatibility/physical atomicity

**Status: PARTIALLY_REMEDIATED / OPEN DESIGN BLOCKER.**

Progress:
- Concrete bounded inventory identifies the Foundation ledger/entity/repository, Foundation unit test and both DB migrations.
- Foundation payload_digest semantics and method signatures are preserved. U07-specific binding fingerprint and globally scoped key are separate.
- Proposed dual-dialect V7 append-only binding migration and an outer REQUIRED transaction with fresh-transaction conflict reconciliation improve the design.
- Failure table now forbids poisoned transaction success and half-committed admission.

Outstanding design closure:
1. The updated §4.3.1 **explicitly states** that the required full-repository call-site/migration/fixture reference scan has **not been performed**. The previous review required an exact consumer impact inventory, not only a selected bounded list. It is therefore not independently established that no other consumer or deployment script conflicts.
2. The binding persists two refs (`binding_payload_ref` and `protected_binding_ref`) to external immutable data, but §4.3.2 also says an in-database canonical byte/blob is the chosen V1 fallback *if atomic reference durability cannot be established*. This leaves **two incompatible durability modes** without a single frozen authoritative storage/commit contract. A same-database relational transaction does not, by itself, atomically commit a separate ref store.
3. The proposed Oracle DDL is still described only as shorthand; a fully specified dialect-equivalent physical contract for binding contents, FK, indexes, owner/immutability and rollback needs targeted closure. Runtime tests are not required for a design gate, but one implementable physical decision is required.

Required targeted remediation: complete the code-reference/fixture/migration inventory on the pinned main tree (or formally register and independently approve a separate mandatory pre-readiness review gate without claiming compatibility already closed); select **one** atomic authoritative binding/payload storage model for V1, including synthetic ref storage semantics; give equivalent MySQL/Oracle physical schema contracts and transaction boundary with the adopted model. Distinguish design evidence from later database integration tests. Do not change existing Foundation semantics or source code under this gate.

## 4. BF-U07-RDP01-IR-03 — U06 eligibility issuance source

**Status: CONDITIONAL_DESIGN_RESOLUTION / CLOSED AS A GAP REGISTRATION, UPSTREAM BLOCKER OPEN.**

The remediation no longer assumes a persistent issuance authority exists. It correctly lists concrete persisted U06 wait-effect, Thread, Checkpoint and trace records and distinguishes them from an actual eligibility issuance. In reviewed code `U06WaitCoordinator.establish` returns a projected eligibilityId, `U07ResumeEligibilityProjector` computes a hash, and the examined V6 migrations contain no `u06_eligibility_issuance` table. The new `CA-U06-U07-ELIG-ISSUANCE-01` names a **required upstream U06-owned controlled amendment**, with a typed proposed issuance record, lookup outcomes, provenance, fail-closed missing/forged-issuance behavior and crash-gap repair requirement.

Accordingly the **specific RDP-01 review concern of hidden presumed authority** is remediated and may be closed **conditionally as a design-gap classification**. This does **not** confirm the issuance system exists or declare the historical reattachment execution path available. The upstream amendment must receive its own detailed physical design, independent review and approval; both DB schemas, persistence/replay, U06 regression, query authority and the crash window must be verified before U07 implementation readiness. Until then the positive historical eligibility branch is not executable or authorized.

This closure is expressly conditioned on retention of the upstream blocker; omission of it from RDP-05/aggregate would reopen the finding.

## 5. Regression / nonblocking findings

| Boundary | Result |
|---|---|
| F8 business validity != P02 runtime compatibility | PRESERVED |
| Canonical event identity != F8 decision | PRESERVED |
| F3 Question and U02 Clinical Truth owners | PRESERVED |
| U06 Eligibility cannot be inferred from hash alone | PRESERVED |
| Missing checkpoint alone cannot turn accepted business event into rejection | PRESERVED |
| Production/PROFILE-A/real-patient/PHI | BLOCKED |
| Runtime test execution evidence | NOT_RUN / NOT_CLAIMED |
| PR merge or implementation authorization | NOT_GRANTED |

No evidence of new semantic ownership violation was found in the targeted delta.

## 6. Gate decision

```text
U07-RDP-01 Second Targeted Independent Design Re-Review = REVISE_REQUIRED
Reviewed head = b7545adb0ef93f51caac415d4cced3612ece6927
BF-U07-RDP01-IR-01 = CLOSED (prior review)
BF-U07-RDP01-IR-02 = OPEN / PARTIALLY_REMEDIATED
BF-U07-RDP01-IR-03 = CONDITIONAL_DESIGN_RESOLUTION / GAP REGISTERED
CA-U06-U07-ELIG-ISSUANCE-01 = REQUIRED / NOT_AUTHORIZED / NOT_IMPLEMENTED
B-U07-RG-01 = OPEN
U07-RDP-01 = DESIGN_CANDIDATE / NOT_FROZEN
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
Production / PROFILE-A / patient traffic = BLOCKED
```

**Next governed step:** `U07-RDP-01 Third Targeted Design Remediation`, strictly targeted to BF-02. Retain the CA-U06 issuance dependency for subsequent owner-controlled design/review, then run an exact-head third targeted independent re-review. No merges or runtime implementation are authorized.
