# Phase A6.5 Decision Log

Planning base: `agent/enterprise-agent-refactoring-plan` @ `b4a1714506ac7b05ce2d8c7b6ecae3bd7bb2e836`
Planning head reviewed: `0cc13f29c564731257569ee293359ae37b5fbce9`
Status: planning only — no implementation decisions executed
Independent review: risk calibration and OD fail-closed fields added

## 1. Confirmed decisions (planning scope)

| ID | Decision | Basis | Binding |
| --- | --- | --- | --- |
| D-001 | A6.5 normative name is **Legacy Design Asset Validation** (also referred to as Legacy Asset Validation in planning titles); inserted between A6 and A7 | `可执行实施路线.md` A6.5; mapping doc §15 | NORMATIVE |
| D-002 | Primary historical design evidence root is `docs/AI医生/项目文档` (49 Markdown files), but scan is **not** limited to that directory | Roadmap requires mapping to code/config/data/tests; A2/A4 evidence | GOVERNING |
| D-003 | A5 `contracts/v1` and A6 `capabilities/adult_respiratory_v1` are mapping targets / boundaries, not legacy retention subjects | A5/A6 evidence | NORMATIVE |
| D-004 | A6.5 must not mutate A6 lifecycle, clinical approval, runtime adoption, or production eligibility | A6 governance | NORMATIVE |
| D-005 | Tracked real patient records found: **0**; PHI-capable paths inventoried without content inspection | A4 + path metadata | GOVERNING |
| D-006 | Batches A–E used; Coverage Matrix `A6.5-F` deliverables retained via D/E mapping table (see OD-007) | Coverage Matrix + engineering deliverables list | GOVERNING |
| D-007 | Planning dispositions map to roadmap KEEP/ADAPT/EXTRACT/SPLIT/EVALUATE/ARCHIVE/REMOVE during implementation | Roadmap + matrices | GOVERNING |
| D-008 | Evidence levels use mapping-doc full set; roadmap `DOCUMENTED` maps to design-document evidence | Mapping doc vs roadmap | GOVERNING |
| D-009 | P0 requires an explicit stop/safety blocker (`risk_basis`); pure design docs are not automatic P0 | Independent plan review | GOVERNING |
| D-010 | A6.5-A forbids content-level clinical extraction and any patient-data content inspection | Independent plan review batch boundary | NORMATIVE |

## 2. Document conflicts

| Conflict | Sources | Resolution | Status |
| --- | --- | --- | --- |
| Evidence enum completeness | Roadmap vs mapping doc | Use mapping-doc full set | RESOLVED (planning) |
| Retention decision enum | Roadmap vs matrices | Planning enums + mapping table | RESOLVED (planning) |
| Batch naming `A6.5-F` | Coverage Matrix | Fold into D/E with deliverable retention table | ACKNOWLEDGED (OD-007 human confirm during A6.5-A authorization) |
| Title wording Asset vs Design Asset | PR title vs normative docs | Treat as aliases; normative phrase retained in plan | RESOLVED (planning) |
| README narrative lag | README vs phase evidence | Prefer phase evidence for baseline facts | RESOLVED (planning) |

## 3. Open decisions (structured)

### OD-001 Medical corpus license

| Field | Value |
| --- | --- |
| decision_id | OD-001 |
| question | Which medical source corpora are legally usable for product Knowledge Release? |
| current_status | NEEDS_HUMAN_DECISION |
| owner_role | legal owner role |
| reviewer_roles | legal; evidence owner role |
| blocking_batches | A6.5-B; A6.5-C; A6.5-E (corpus-related tasks) |
| default_fail_closed_decision | QUARANTINE; NOT_APPROVED; NOT_ELIGIBLE_FOR_KNOWLEDGE_RELEASE |
| required_evidence | license classification; publisher/version; provenance; withdrawal policy |
| decision_deadline_or_phase_gate | before any Knowledge Release mapping exits A6.5-E |
| impact_if_unresolved | corpora remain quarantined; approved medical sources stay 0 |

### OD-002 DR.KNOWS

| Field | Value |
| --- | --- |
| decision_id | OD-002 |
| question | Are DR.KNOWS assets evaluable for product graph enhancement or archive-only? |
| current_status | NEEDS_HUMAN_DECISION |
| owner_role | architecture owner role |
| reviewer_roles | legal; architecture; evidence |
| blocking_batches | A6.5-B; A6.5-E |
| default_fail_closed_decision | ARCHIVE_OR_QUARANTINE; NO_PRODUCT_USE |
| required_evidence | LICENSE terms applicability; training-data provenance; reproducibility statement |
| decision_deadline_or_phase_gate | before any EVALUATE experiment beyond read-only inventory |
| impact_if_unresolved | no download/run/eval of external DRKnows resources; no product enablement |

### OD-003 Path-Injection LLM

| Field | Value |
| --- | --- |
| decision_id | OD-003 |
| question | Remain EVALUATE, REJECT, or KEEP_READ_ONLY for adult respiratory v1? |
| current_status | NEEDS_HUMAN_DECISION |
| owner_role | architecture owner role |
| reviewer_roles | clinical; architecture; model governance |
| blocking_batches | A6.5-C; A6.5-E |
| default_fail_closed_decision | REJECT_OR_KEEP_READ_ONLY; NO_RUNTIME_REGISTRATION |
| required_evidence | explicit accept/reject record; if evaluate, controlled experiment plan without production path |
| decision_deadline_or_phase_gate | before A6.5-E disposition finalization for PROMPT-004/DATA-KG005 |
| impact_if_unresolved | deferred decision defaults to deny (not allow) |

### OD-004 Owner assignment

| Field | Value |
| --- | --- |
| decision_id | OD-004 |
| question | Assign owner roles for P0 clinical/prompt/state/PHI assets |
| current_status | NEEDS_OWNER |
| owner_role | governance owner role |
| reviewer_roles | clinical owner; architecture owner; governance owner |
| blocking_batches | A6.5-B; A6.5-E |
| default_fail_closed_decision | keep declared_owner=UNASSIGNED; block disposition approval |
| required_evidence | role assignment matrix (names optional; roles mandatory) |
| decision_deadline_or_phase_gate | before A6.5-E board approval |
| impact_if_unresolved | no KEEP/ADAPT promotion; assets remain NEEDS_OWNER |

### OD-005 Three evaluation suites

| Field | Value |
| --- | --- |
| decision_id | OD-005 |
| question | Are static/interactive/trajectory suites DESIGNED_BUT_MISSING, RENAMED_OR_RELOCATED, NEVER_IMPLEMENTED, or EXTERNAL_NOT_AVAILABLE? |
| current_status | NEEDS_EVIDENCE |
| owner_role | evaluation owner role |
| reviewer_roles | evaluation; architecture |
| blocking_batches | A6.5-D; A6.5-E |
| default_fail_closed_decision | treat as DESIGNED_BUT_MISSING; no fabricated datasets; synthetic-only planning |
| required_evidence | locate path OR explicit NEVER_IMPLEMENTED/EXTERNAL_NOT_AVAILABLE declaration |
| decision_deadline_or_phase_gate | A6.5-D exit |
| impact_if_unresolved | cannot claim eval-backed KEEP/ADAPT |

### OD-006 Live PHI inspection authorization

| Field | Value |
| --- | --- |
| decision_id | OD-006 |
| question | May live/staging stores be inspected beyond path-level inventory? |
| current_status | NEEDS_SECURITY_REVIEW |
| owner_role | security/privacy owner role |
| reviewer_roles | security; privacy |
| blocking_batches | any task proposing live/staging content access |
| default_fail_closed_decision | NO_CONTENT_INSPECTION; PATH_LEVEL_INVENTORY_ONLY |
| required_evidence | written privacy authorization; scoped access plan; redaction controls |
| decision_deadline_or_phase_gate | before any live-store inspection task is unblocked |
| impact_if_unresolved | remain path-only; no sample copy into docs |

### OD-007 A6.5-F folding confirmation

| Field | Value |
| --- | --- |
| decision_id | OD-007 |
| question | Confirm Coverage Matrix A6.5-F deliverables are fully retained by D/E |
| current_status | ACKNOWLEDGED |
| owner_role | architecture owner role |
| reviewer_roles | architecture |
| blocking_batches | A6.5-D; A6.5-E (naming confirmation only; deliverables already mapped) |
| default_fail_closed_decision | keep D/E retention table; do not drop deliverables |
| required_evidence | human ack of retention table below |
| decision_deadline_or_phase_gate | before plan Ready / before A6.5-A |
| impact_if_unresolved | implementation may use A–E only after ack; deliverables still mandatory |
| acknowledgement_record | Repository-owner authorization for A6.5-A confirmed that: evaluation-suite and legacy-eval inventory deliverables remain in A6.5-D; decommission and matrix-write-back deliverables remain in A6.5-E. Retention table below is unchanged and mandatory. |

#### A6.5-F deliverable retention table

| Coverage / normative deliverable | Retained in batch | Backlog / artifact |
| --- | --- | --- |
| Static Case Eval suite validation planning | A6.5-D | TASK-D01 / DATA-EV001 / `legacy-eval-asset-inventory.csv` |
| Interactive Interview Eval suite validation planning | A6.5-D | TASK-D01 / DATA-EV002 |
| Trajectory Replay Eval suite validation planning | A6.5-D | TASK-D01 / DATA-EV003 |
| `legacy-eval-asset-inventory.csv` | A6.5-D | TASK-D01 expected_files |
| Legacy Asset Decommission Register | A6.5-E | TASK-E01 / `legacy-asset-decommission-register.csv` |
| Decommission Gate evidence (owner/deps/rollback) | A6.5-E | TASK-E01 acceptance criteria |
| Matrix write-back for Coverage/Migration | A6.5-E | TASK-E02 |

## 4. Scope trade-offs

- A6.5-A reduced to discovery/quarantine/path-level clinical-candidate location.
- Content-level clinical-policy extraction moved to TASK-B04 (`BLOCKED` on clinical owner).
- P0 recalibrated with `risk_basis`; design docs no longer automatic P0.

## 5. Unresolved dependencies

- Clinical owner availability for TASK-B04. SUPERSESSION POINTER (2026-08-14, `A6.5-NONADOPT-001` now `MERGED_AND_VERIFIED`): B04 extraction/migration purpose is `SUPERSEDED_BY_LEGACY_CLINICAL_NON_ADOPTION` / `NOT_REQUIRED_FOR_NEW_RUNTIME_MIGRATION`. Clinical Owner is not required to judge old clinical correctness for rejected assets. Historical dependency text is retained. Do not execute B04 extraction.
- Legal review for OD-001/OD-002.
- Privacy authorization for OD-006 if live inspection ever requested.
- A7 still required for PromptSpec/ModelSpec enablement.

## 6. Recommendation after independent review remediation

Independent remediation targets: `READY_FOR_A6_5_PLAN_REMEDIATION_MERGE` for the review PR only.
PR #10 remains Draft; A6.5-A remains NOT_STARTED.

## 7. A7-NC implementation-order decision addendum

> Decision ID: `A7-NC-ORDER-001`
>
> Decision status: `MERGED_AND_VERIFIED`
>
> PR: `#29`
>
> Reviewed Head: `87e9780220e0fa219f1d0e0e91c2f06dea79b21d`
>
> Enterprise merge: `2c9dbf866c2c6b064f68fcab1f557635e64a73d2`
>
> Architecture classification: `IMPLEMENTATION_ORDER_AMENDMENT`

### Decision

Permit a controlled non-clinical A7-NC planning and, only after separate authorization, implementation lane before A6.5 Exit. The historical `A6 → A6.5 → A7` route remains the normal baseline.

### Reason

TASK-B04 is blocked by an external clinical-governance dependency: only 6/11 unlock conditions are satisfied; Clinical Owner and Human Clinical Reviewer roles are not assigned; written content-access authorization is absent. Platform mechanics can be isolated without reading clinical content or changing clinical behavior.

### Boundaries

```text
Architecture: unchanged
Execution order: amended
Architecture Refreeze: NOT_REQUIRED
A6.5: INCOMPLETE_BLOCKED_DEPENDENCY
A7-NC Roadmap Amendment: MERGED_AND_VERIFIED
A7-NC implementation authorization: NOT_GRANTED
A7-NC implementation: NOT_STARTED
A7-CL: BLOCKED_BY_A6_5_CLINICAL_LANE
A7 overall: NOT_COMPLETE
Clinical activation: BLOCKED
Clinical Runtime: NOT_ENABLED
Production: BLOCKED
```

State ownership, the unique State Committer write path, Mandatory Safety, Triage, Capability governance, the single Model Gateway, provider abstraction and the clinical write boundary remain frozen. Shared Contract and Capability semantic mutations are forbidden. Real-provider access, clinical Prompt content, patient data and clinical gold are outside A7-NC.

### Rejoin and authorization

Historical PR #29 text required `B04 → C02 → E01 → E02 → A6.5 Exit` before A7-CL. That migration-purpose chain is **superseded** by `A6.5-NONADOPT-001` for legacy clinical content. PR #29 body and P7 evidence are **not rewritten**. Future Full A7 Rejoin planning semantics are recorded in [the non-adoption strategy](./a6-5-legacy-clinical-non-adoption-strategy.md) and the PR #38 remediation.

## 8. Legacy Clinical Non-Adoption Decision

> Decision ID: `A6.5-NONADOPT-001`
>
> Date: `2026-08-14`
>
> Owner: Repository Owner
>
> Decision: `LEGACY_CLINICAL_NON_ADOPTION`
>
> Owner policy: `LEGACY_CLINICAL_ASSETS_WILL_NOT_BE_MIGRATED`
>
> Decision status: `MERGED_AND_VERIFIED`
>
> Enterprise merge (PR #38): `6ee86cb001aceb8a6cf264c9a6fbd629dddd8dda`
>
> Architecture classification: `PLANNING_GOVERNANCE_ONLY`
>
> Architecture Change: `NO`
>
> Architecture Refreeze: `NOT_REQUIRED`

### Decision

Legacy clinical rules, Prompts, triage/risk/diagnostic-reasoning paths, and medical-knowledge assets will **not** be inherited as new-system clinical authority. Future Safety / Red Flags / Triage / Question / Hypothesis / Clinical Prompt / Medical Source / Clinical Evaluation must be rebuilt under a separate, not-yet-authorized track.

### Rationale

Legacy assets lack sufficient governance provenance to become Capability, Safety, Triage, Question, Hypothesis, Prompt Release, clinical gold, or Production clinical behavior. Non-adoption is a governance strategy, not an architecture change and not an immediate delete.

### Scope

CLIN-001..010, CLIN-020..029, PROMPT-001..015, DATA-KG001..006, DATA-K001..031, clinical DOC assets listed in the strategy, WF-003/WF-017 (clinical-authority reject only; non-clinical ADAPT unchanged). Clinical bodies were not read.

### Consequences

```text
LEGACY_CLINICAL_TRACK: NON_ADOPTED
FUTURE_NEW_CLINICAL_TRACK: NOT_YET_AUTHORIZED
NON_ADOPTION != NO_FUTURE_CLINICAL_GOVERNANCE
NON_ADOPTION != IMMEDIATE_DELETE
NON_ADOPTION != A6 COMPLETE
NON_ADOPTION != A7 COMPLETE
NON_ADOPTION != CLINICAL_RUNTIME_ENABLED
```

### Superseded task purposes

- TASK-B04 migration/extraction purpose: `SUPERSEDED_BY_LEGACY_CLINICAL_NON_ADOPTION` / `NOT_REQUIRED_FOR_NEW_RUNTIME_MIGRATION`. Historical row preserved. Status remains `BLOCKED`. Not `EXECUTED_COMPLETE`.
- TASK-C02 rejected-asset Capability mapping: `NOT_REQUIRED_FOR_REJECTED_ASSETS`. Historical row preserved. Status `BLOCKED` (policy). Not `EXECUTED_COMPLETE`.

### Preserved tasks

- TASK-E01 disposition board: preserved; C02 no longer a prerequisite for rejected clinical assets; ADOPT/ADAPT/MAP forbidden as new clinical authority.
- TASK-E02 matrix write-back: preserved; target labels `NOT_MIGRATED` / `NOT_AUTHORITY` / `NOT_RUNTIME_ADOPTED` / `FUTURE_REBUILD_REQUIRED`.

### Future Clinical Build

Separate. `NOT_YET_AUTHORIZED`. Must not be stuffed into A6.5.

### Physical deletion

Not authorized. Runtime consumers may still exist. Separate decommission gate required later.

### Durable authority

[a6-5-legacy-clinical-non-adoption-strategy.md](./a6-5-legacy-clinical-non-adoption-strategy.md)

## 9. E01 disposition-board implementation addendum (2026-08-14)

> Addendum only. Does not rewrite §8 historical decision text.
>
> Branch: `agent/a6-5-e01-disposition-board`
>
> Exact Base: `6ee86cb001aceb8a6cf264c9a6fbd629dddd8dda`
>
> Implementation state: `IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`
>
> Machine backlog `TASK-E01.status`: `PLANNED` (existing enum; no illegal status invented)
>
> E02: `NOT_AUTHORIZED` / `NOT_EXECUTED`
>
> A6.5: `INCOMPLETE_PENDING_E01_E02_EXIT`

Authoritative E01 population is
[a6-5-e01-disposition-board.csv](./a6-5-e01-disposition-board.csv).
Historical `TASK-E01.asset_ids` (40 IDs) remain a `HISTORICAL_SEED_SET` only.
Current control pointer:
[phase-a-current-state-reconciliation-2026-08-14-post-pr38.md](../phase-a-current-state-reconciliation-2026-08-14-post-pr38.md).

## 10. E02 matrix write-back addendum (2026-08-14)

> Addendum only. Does not rewrite §8 / §9.
>
> PR #39: `MERGED` (`3bb48dc538ac83325d7cdd6f460c9da9b0e93bb9`)
>
> TASK-E01: `MERGED_AND_VERIFIED`
>
> TASK-E02 branch: `agent/a6-5-e02-matrix-writeback`
>
> Implementation state: `IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`
>
> A6.5 Exit: `NOT_EXECUTED`
>
> A6.5: `INCOMPLETE_PENDING_E02_EXIT`

Authoritative write-back:
[a6-5-e02-matrix-writeback.csv](./a6-5-e02-matrix-writeback.csv).
Current control pointer:
[phase-a-current-state-reconciliation-2026-08-14-post-pr39.md](../phase-a-current-state-reconciliation-2026-08-14-post-pr39.md).
