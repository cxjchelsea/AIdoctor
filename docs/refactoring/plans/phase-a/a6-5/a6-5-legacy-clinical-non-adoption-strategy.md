# A6.5 Legacy Clinical Non-Adoption Strategy

> Title: A6.5 Legacy Clinical Asset Non-Adoption Strategy
>
> Decision ID: `A6.5-NONADOPT-001`
>
> Strategy: `LEGACY_CLINICAL_NON_ADOPTION`
>
> Owner decision: `LEGACY_CLINICAL_ASSETS_WILL_NOT_BE_MIGRATED`
>
> Status: `MERGED_AND_VERIFIED`
>
> Planning state: `PLANNING_GOVERNANCE_ONLY`
>
> Authorized Base (historical planning): `49b0e4467fb4cf96a4893f9158bf23584ab69ee5`
>
> Planning branch (historical): `agent/phase-a-nc-closure-roadmap-amendment`
>
> Enterprise merge (PR #38): `6ee86cb001aceb8a6cf264c9a6fbd629dddd8dda`
>
> Architecture Change: `NO`
>
> Architecture Refreeze: `NOT_REQUIRED`
>
> Clinical Runtime: `NOT_ENABLED`
>
> Production: `BLOCKED`
>
> Phase B: `NOT_AUTHORIZED`
>
> Relationship to PR #29: **does not rewrite** historical A7-NC amendment or P7 evidence.
> This document **supersedes** only the legacy-clinical-migration purpose of B04/C02
> and the Full A7 Rejoin items that required `B04 COMPLETE` / `C02 COMPLETE`.

## 1. Owner decision

Repository Owner Explicit Authorization records:

```text
LEGACY_CLINICAL_ASSETS_WILL_NOT_BE_MIGRATED
```

Legacy clinical rules, clinical Prompts, triage logic, risk-screening logic,
diagnostic-reasoning paths, and medical-knowledge assets must **not** be inherited
as new-system:

- Capability clinical authority
- Safety authority
- Triage authority
- Question authority
- Hypothesis authority
- Prompt Release
- clinical gold
- Production clinical behavior

Future clinical content must be rebuilt under a **separate** governance track.

## 2. Track separation

```text
LEGACY_CLINICAL_TRACK: NON_ADOPTED
FUTURE_NEW_CLINICAL_TRACK: NOT_YET_AUTHORIZED
```

```text
NON_ADOPTION != NO_FUTURE_CLINICAL_GOVERNANCE
NON_ADOPTION != IMMEDIATE_DELETE
NON_ADOPTION != A6 COMPLETE
NON_ADOPTION != A7 COMPLETE
NON_ADOPTION != CLINICAL_RUNTIME_ENABLED
NON_ADOPTION != A7-CL READY
```

A6.5 owns **legacy governance closure** only. It must not create new Safety,
Red Flags, Triage, Question Policy, Hypothesis Policy, Clinical Prompt,
Medical Source Release, Clinical Evaluation, or clinical gold.

## 3. Scope (inventory metadata only)

Authoritative inventory: [a6-5-legacy-asset-inventory.csv](./a6-5-legacy-asset-inventory.csv)

Clinical bodies were **not** read. Scope uses `asset_id`, path, type, consumer,
risk, and existing evidence only.

| Class | Asset IDs | Authority fate | File fate |
|---|---|---|---|
| Terminology | CLIN-001..006 | `REJECT_FOR_NEW_RUNTIME` | `KEEP_READ_ONLY` |
| Clinical rules / workflow policy | CLIN-007..010, CLIN-023, CLIN-024, CLIN-027..029 | `REJECT_FOR_NEW_RUNTIME` | `KEEP_READ_ONLY` |
| Already quarantined clinical logic | CLIN-020..022, CLIN-025, CLIN-026 | `REJECT_FOR_NEW_RUNTIME` | `QUARANTINE` (not lifted) |
| Runtime clinical Prompts | PROMPT-001..011 | `REJECT_FOR_NEW_RUNTIME`; `NO_PROMPT_RELEASE` | `KEEP_READ_ONLY` |
| Prompt inventory evidence | PROMPT-012 | not a Prompt Release | `KEEP_READ_ONLY` |
| Offline translation scripts | PROMPT-013..015 | not a Prompt Release | `KEEP_READ_ONLY` |
| Medical sources / derived knowledge | DATA-K001..031 | `NOT_APPROVED_MEDICAL_SOURCE` | `QUARANTINE` |
| KG / path-injection | DATA-KG001..006 | `REJECT_FOR_NEW_RUNTIME` | `KEEP_READ_ONLY` |
| Clinical design docs | DOC-019, DOC-020, DOC-021, DOC-025..028, DOC-043, DOC-045..049 | not clinical authority | `KEEP_READ_ONLY` |
| Workflow with clinical relevance | WF-003, WF-017 | not clinical authority | existing non-clinical `ADAPT` **unchanged** |

CLIN-011..019 do not exist in the inventory. BOUND-002 is the **new** A6 package
and is not a legacy clinical asset.

Forbidden dispositions for legacy clinical authority:

```text
ADOPT_AS_CLINICAL_AUTHORITY
ADAPT_INTO_NEW_CLINICAL_RUNTIME
MAP_CONTENT_INTO_CAPABILITY
```

## 4. Disposition model

Allowed file/governance dispositions for legacy clinical assets:

- `QUARANTINE` — keep existing isolation; append reject-for-new-runtime semantics
- `KEEP_READ_ONLY` — preserve for provenance / audit / replacement comparison
- `ARCHIVE` — historical only; not used as a silent delete
- `REJECT_FOR_NEW_RUNTIME` — decision_status: not new-runtime clinical authority
- `DEPRECATE_PENDING_REPLACEMENT` — reserved for later decommission, not this PR

Physical deletion is **not authorized**. Runtime consumers may still exist in:

`health-state-assessment-service`, `risk-assessment-service`, `dialog-service`,
`diagnosis-engine-service`, `clinical-parsing-service`, `common`.

Future deletion requires: no runtime consumer, non-adoption evidence complete,
and a separate decommission authorization.

## 5. B04 treatment

Historical task `TASK-B04` is **preserved** as a backlog row.

```text
Historical purpose: content-level legacy clinical-policy extraction
Planning semantics: SUPERSEDED_BY_LEGACY_CLINICAL_NON_ADOPTION
Machine status (existing enum): BLOCKED
Not: EXECUTED_COMPLETE
Not: B04 COMPLETE
Extraction artifact: NOT_REQUIRED_FOR_NEW_RUNTIME_MIGRATION
legacy-clinical-policy-extraction.md: MUST NOT be produced under this strategy
```

Reason: Owner forbids legacy clinical content migration. Content extraction
would only serve a mapping path that is now closed.

## 6. C02 treatment

Historical task `TASK-C02` is **preserved** as a backlog row.

```text
Historical purpose: map legacy clinical packs to A6 Capability schemas
Planning semantics: NOT_REQUIRED_FOR_REJECTED_ASSETS
Not: EXECUTED_COMPLETE
Not: C02 COMPLETE
```

Rejected / quarantined / keep-read-only clinical assets must not be schema-mapped
into Capability slots. Metadata “not mapped / not authority” belongs to E01/E02.

## 7. E01 treatment

`TASK-E01` is **PRESERVED**.

New purpose: formal disposition board for REJECT / ARCHIVE / KEEP_READ_ONLY /
QUARANTINE, plus owner, evidence pointer, runtime consumer, and replacement status.

Dependency change (planning):

```text
Was: TASK-B02; TASK-C02; TASK-D01
Now: TASK-B02; TASK-D01
```

C02 content mapping is **not** a prerequisite for rejected clinical assets.

For Legacy Clinical Assets, E01 must not propose ADOPT / ADAPT / MAP as
new clinical authority. Non-clinical asset disposition semantics are unchanged.

E01 is **not executed** by this planning change.

Post-PR38 current note (2026-08-14): TASK-E01 board implementation is proposed
on `agent/a6-5-e01-disposition-board` as
`IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`. Authoritative population is
[a6-5-e01-disposition-board.csv](./a6-5-e01-disposition-board.csv)
(not the historical 40-id seed). Machine backlog `status` remains `PLANNED`
(existing enum). E02 remains `NOT_AUTHORIZED`. A6.5 remains
`INCOMPLETE_PENDING_E01_E02_EXIT`.

## 8. E02 treatment

`TASK-E02` is **PRESERVED**.

Purpose: write back Migration Matrix, Coverage Matrix, and Decommission Register
with:

```text
NOT_MIGRATED
NOT_AUTHORITY
NOT_RUNTIME_ADOPTED
FUTURE_REBUILD_REQUIRED
Physical file: PRESERVED until separate decommission
```

E02 is **not executed** by this planning change.

## 9. Recommended A6.5 dependency graph

Historical clinical migration chain (preserved as history, no longer the
planning path):

```text
B04 → C02 → E01 → E02 → A6.5 Exit
```

Recommended replacement (planning; not pre-executed):

```text
Repository Owner Legacy Clinical Non-Adoption Decision
  → durable strategy + inventory disposition metadata
  → E01 disposition board
  → E02 matrix write-back
  → A6.5 Exit Review
```

```text
B04: SUPERSEDED / NOT_REQUIRED_BY_POLICY
C02: NOT_REQUIRED_FOR_REJECTED_ASSETS
```

## 10. A6.5 Exit amendment

A6.5 Exit PASS means only that legacy validation / non-adoption / disposition /
migration **governance** is closed. It does **not** mean new clinical baseline
ready, A6 clinically approved, A7-CL ready, or clinical rules approved.

| Historical Exit item | Treatment |
|---|---|
| P0/P1 unique inventory records | `KEEP` |
| Non-clinical KEEP/ADAPT have test plans | `KEEP` |
| Clinical KEEP/ADAPT-as-authority test plans | `REPLACE_WITH_NON_ADOPTION_EVIDENCE` |
| EVALUATE experiment for non-clinical / research assets | `KEEP` |
| ARCHIVE/REMOVE dependency and rollback checks | `KEEP` (strengthened: no immediate delete) |
| AOP/Trace/CDP/AgentState/AuditTrail/Tool mapping | `KEEP` (non-clinical; already C01/C03) |
| Coverage “no valuable unowned asset” | `REPLACE_WITH_NON_ADOPTION_EVIDENCE` |
| `legacy-clinical-policy-extraction.md` as required deliverable | `REPLACE_WITH_NON_ADOPTION_EVIDENCE` |
| Full A7 Rejoin “B04 COMPLETE” / “C02 COMPLETE” | `REMOVE_BY_AMENDMENT` (supersession; PR #29 text not rewritten) |

Required non-adoption Exit evidence:

1. exact legacy clinical asset inventory
2. per-asset disposition
3. runtime consumer check
4. Owner decision provenance
5. no Capability authority binding
6. no Prompt Release
7. clinical content copied = 0
8. clinical threshold copied = 0
9. clinical gold copied = 0
10. PHI copied = 0
11. approved medical sources inherited = 0
12. Migration Matrix write-back
13. Coverage Matrix write-back
14. Decommission status
15. physical deletion = 0 unless separately authorized

## 11. Future clinical rebuild boundary

```text
FUTURE_NEW_CLINICAL_TRACK: NOT_YET_AUTHORIZED
```

Future rebuild of Safety, Red Flags, Triage, Question Policy, Hypothesis Policy,
Clinical Prompt, Medical Source Release, Clinical Evaluation, and Clinical Gold
requires new source governance, Clinical Owner, Human Clinical Reviewer,
provenance, versioning, evaluation, and approval.

That work must **not** be stuffed back into A6.5.

## 12. A7-CL impact

Machine-readable control state remains the existing convention until merge:

```text
A7-CL: BLOCKED_BY_A6_5_CLINICAL_LANE
A7: NOT_COMPLETE
```

Narrative after this strategy is reviewed and A6.5 non-adoption Exit later
closes:

```text
remaining blocker becomes new clinical content governance
recommended future label: BLOCKED_PENDING_NEW_CLINICAL_CONTENT
```

This planning change does **not** set A7-CL AUTHORIZED / READY / COMPLETE.

## 13. Full A7 Rejoin supersession

Historical PR #29 Full A7 Rejoin items 1–2 required `TASK-B04 COMPLETE` and
`TASK-C02 COMPLETE`. Those items are **superseded** by this Owner policy.

Future Full A7 Rejoin (planning semantics; PR #29 historical text preserved):

```text
A6.5 Legacy Non-Adoption Exit evidence valid
+ A7-NC Exit PASSED
+ new A7-CL prerequisites separately satisfied
+ future new clinical governance evidence
→ Full A7 Rejoin may be considered
```

A7 remains `NOT_COMPLETE`. `A7_ROADMAP_AMENDMENT_REQUIRED` is recorded here
and in the PR #38 remediation. Historical A7-NC amendment file is not rewritten.

## 14. A11 / Frozen Baseline

| Gate | Treatment |
|---|---|
| FB-11 Safety/Question/Hypothesis clinical first-edition approval | `PRESERVED` — new-clinical baseline |
| FB-20 A6.5 Exit | `PRESERVED_WITH_NON_ADOPTION_EXIT` — closable by governance, not by migration |
| FB-21 A7 overall COMPLETE | `PRESERVED` |

```text
LEGACY_CLINICAL_MIGRATION_BLOCKER:
  REMOVED_BY_NON_ADOPTION_POLICY once this amendment is verified

NEW_CLINICAL_BASELINE_BLOCKER:
  REMAINS
```

## 15. Runtime deletion boundary

This strategy forbids delete, rename, move, import-break, or behavior change of
legacy runtime files. Production Java/Python/Frontend, `contracts/v1`,
`capabilities`, `packages/model_runtime`, `common/aidoctor_llm` runtime files,
CI workflows, and tests must remain unchanged by this planning PR.

## 16. Stop conditions

Stop and escalate if this work would:

- read or copy clinical rule / threshold / Prompt / policy bodies
- execute B04 extraction or C02 capability mapping
- execute E01/E02 implementation
- mark B04/C02 `EXECUTED_COMPLETE`
- delete or move runtime files
- activate A7-CL, write new clinical content, or start Phase B
- claim A6 COMPLETE, A7 COMPLETE, or Frozen Baseline
- rewrite PR #29 or P7 evidence
- mark PR #38 Ready or merge it from this change alone

## 17. Current-state split

Until Independent Review, Merge Review, Explicit Merge Authorization, merge,
and post-merge verification:

```text
Enterprise current truth:
  B04: BLOCKED
  C02: BLOCKED_BY_TASK_B04
  A6.5: INCOMPLETE_BLOCKED_DEPENDENCY
  A7-CL: BLOCKED_BY_A6_5_CLINICAL_LANE
  A7: NOT_COMPLETE
  Clinical Runtime: NOT_ENABLED
  Production: BLOCKED

Planning-branch proposed truth:
  Legacy Clinical: NON_ADOPTION
  B04: SUPERSEDED_BY_POLICY (not EXECUTED_COMPLETE)
  C02: NOT_REQUIRED_FOR_REJECTED_ASSETS (not EXECUTED_COMPLETE)
  E01/E02: PRESERVED / not executed
```

## 18. Next Gate

```text
A6.5 Legacy Clinical Non-Adoption
+ Phase A NC Closure Roadmap Amendment
Combined Independent Review
for the exact new PR #38 Head
```

Do not treat this document as `MERGED_AND_VERIFIED` from planning implementation alone.
