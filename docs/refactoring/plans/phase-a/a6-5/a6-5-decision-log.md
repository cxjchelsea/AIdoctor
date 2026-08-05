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
| Batch naming `A6.5-F` | Coverage Matrix | Fold into D/E with deliverable retention table | RESOLVED (planning) pending OD-007 human confirm |
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
| current_status | NEEDS_HUMAN_DECISION |
| owner_role | architecture owner role |
| reviewer_roles | architecture |
| blocking_batches | A6.5-D; A6.5-E (naming confirmation only; deliverables already mapped) |
| default_fail_closed_decision | keep D/E retention table; do not drop deliverables |
| required_evidence | human ack of retention table below |
| decision_deadline_or_phase_gate | before plan Ready / before A6.5-A |
| impact_if_unresolved | implementation may use A–E only after ack; deliverables still mandatory |

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

- Clinical owner availability for TASK-B04.
- Legal review for OD-001/OD-002.
- Privacy authorization for OD-006 if live inspection ever requested.
- A7 still required for PromptSpec/ModelSpec enablement.

## 6. Recommendation after independent review remediation

Independent remediation targets: `READY_FOR_A6_5_PLAN_REMEDIATION_MERGE` for the review PR only.
PR #10 remains Draft; A6.5-A remains NOT_STARTED.
