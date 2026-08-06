# Phase A6.5-B Structural, Provenance and Clinical Evidence Review Plan

Document status: **Draft planning**
Planning branch: `agent/phase-a6-5-b-structural-provenance-validation-plan`
Enterprise base: `90cdc686a2991156c97445c7a6958203ce70e348`
A6.5-A status: `A6_5_A_MERGED_AND_VERIFIED`
A6.5-B status: `NOT_STARTED`
TASK-B04 status: `BLOCKED`

## 1. Objective

Phase A6.5-B validates legacy clinical, prompt, knowledge and governance assets at structural and evidence levels without approving clinical content, enabling Runtime, changing production eligibility, or accessing patient data.

The batch is split into:

- **B-Core**: Gate-B0 + TASK-B01 + TASK-B02 + TASK-B03.
- **B04**: separately unlocked, human-supervised clinical-policy candidate extraction.

B-Core is dependency-separable from TASK-B04, but remains `NOT_STARTED` until separately authorized by the Repository Owner. This planning document does **not** grant `AUTHORIZED_FOR_IMPLEMENTATION` to TASK-B01/B02/B03. TASK-B04 remains `BLOCKED` until clinical-owner, human-reviewer, and scoped content-access authorization conditions are all met.

## 2. Current baseline

```text
Repository: cxjchelsea/AIdoctor
Enterprise branch: agent/enterprise-agent-refactoring-plan
Enterprise Head: 90cdc686a2991156c97445c7a6958203ce70e348

A6.5 planning: MERGED_AND_VERIFIED
A6.5-A: MERGED_AND_VERIFIED
A6.5-B: NOT_STARTED
A6.5-C: NOT_STARTED
A7: NOT_STARTED

Lifecycle: DRAFT
Clinical review: REQUIRES_CLINICAL_REVIEW
Approved clinical rules: 0
Approved thresholds: 0
Approved hypotheses: 0
Approved medical sources: 0
Runtime: NOT_IMPLEMENTED
Production: BLOCKED
CI: NO_CI_CONFIGURED
```

A6.5-A established:

```text
Inventory assets: 203
Discovery rows: 203
P0/P1/P2/P3: 77/72/52/2
Quarantine rows: 90
Unique quarantined assets: 87
Validation evidence rows: 303
Risk themes: 8
Backlog tasks: 13
PHI-capable assets: 18
Clinical candidate paths: 9
Missing evaluation suites: 3
```

Open residual:

```text
F-A65A-R09: OPEN / P2 / NON_BLOCKING
DATA-KG001..003 retain inventory-level RUNTIME_COUPLING_BLOCKER.
```

B-Core may collect structural or governance evidence for DATA-KG001..003 but must not mark F-A65A-R09 resolved without a separate Architecture Decision.

### 2.1 Current vs future task status

Current actual status (planning PR does not change this):

```text
TASK-B01: PLANNED / NOT_STARTED
TASK-B02: PLANNED / NOT_STARTED
TASK-B03: PLANNED / NOT_STARTED
TASK-B04: BLOCKED
B-Core: NOT_STARTED
A6.5-B implementation: NOT_STARTED
```

Future Gate-B0 may record `AUTHORIZED_FOR_IMPLEMENTATION` for B01–B03 only after explicit Repository Owner authorization of B-Core. Until then, Access Manifest rows for B01–B03 remain `authorization_status=NOT_YET_AUTHORIZED`.

### 2.2 Historical planning snapshots

`a6-5-implementation-backlog.csv` and `a6-5-decision-log.md` retain historical planning text such as `status=PLANNED` for A-batch tasks and Decision Log headers stating `planning only` / earlier Enterprise bases. Those are historical snapshots:

- A6.5-A actual completion is defined by merged A6.5-A Evidence (`A6_5_A_MERGED_AND_VERIFIED`).
- Gate-B0 must add a Current Implementation Status Addendum that reconciles backlog/decision-log historical rows to verified evidence without deleting or rewriting history.
- OD-001..OD-006 remain unresolved/fail-closed; OD-007 remains `ACKNOWLEDGED`.

## 3. Governing tasks

### TASK-B01 — Structural parse

Parse clinical, knowledge and prompt files; record format/schema completeness; do not make clinical correctness claims.

### TASK-B02 — Provenance, license, privacy and governance pass

Populate or fail-close owner, license, provenance, privacy and clinical-status fields. Unknown or insufficient evidence remains blocked.

### TASK-B03 — Duplication and conflict detection

Detect duplicate prompts, engines and design/code conflicts. Record a SoT candidate only where evidence supports one; otherwise record `CONFLICTING` or `INSUFFICIENT_EVIDENCE`.

### TASK-B04 — Clinical-policy candidate extraction

Human-supervised extraction of candidate clinical policy text. This is not approval. It requires an accountable clinical owner role and remains blocked until separately authorized.

## 4. Gate-B0 — status and access reconciliation

Gate-B0 is the first step of a future B-Core implementation. Planning review must not execute Gate-B0.

Before reading or parsing target content:

1. Reconcile implementation status into a Current Implementation Status Addendum:
   - TASK-A01/A02/A03 → `MERGED_AND_VERIFIED` (from A6.5-A Evidence; backlog may still show historical `PLANNED`).
   - TASK-B01/B02/B03 → `AUTHORIZED_FOR_IMPLEMENTATION` only after explicit Repository Owner B-Core authorization; otherwise remain `NOT_STARTED` / `NOT_YET_AUTHORIZED`.
   - TASK-B04 → `BLOCKED`.
2. Preserve OD-001..OD-006 as unresolved/fail-closed; preserve OD-007 as `ACKNOWLEDGED`.
3. Preserve RISK-001..005 and RISK-008 as `OPEN`; preserve F-A65A-R09 as `OPEN` / P2 / NON_BLOCKING.
4. Create an access manifest for every B-Core target and every B04 candidate (B04 rows stay blocked).
5. Confirm PHI runtime paths remain `NO_CONTENT_ACCESS`.
6. Confirm external downloads, external models, paid APIs and live/staging access are forbidden.
7. Do not read clinical rule bodies, prompt bodies, medical corpus records, or patient data during Gate-B0.

Recommended artifact:

```text
docs/refactoring/evidence/phase-a/a6-5/a6-5-b-access-manifest.csv
```

Recommended fields:

```text
access_id
asset_id
path
task_id
access_class
authorization_status
allowed_operation
forbidden_operation
executor_role
required_reviewer_role
raw_content_persistence
output_redaction
external_access
patient_data_risk
clinical_content_risk
status
evidence_reference
notes
```

Distinguish:

```text
plan status          (this document / backlog planning labels)
authorization_status (NOT_YET_AUTHORIZED | AUTHORIZED | BLOCKED)
execution status     (NOT_STARTED | IN_PROGRESS | COMPLETED | STOPPED)
```

At planning time and until B-Core is separately authorized:

```text
B01-B03 authorization_status: NOT_YET_AUTHORIZED
B04 authorization_status: BLOCKED
access_class HUMAN_SUPERVISED_CLINICAL_READ: inactive / not executable
raw_content_persistence: no
```

Planning PR rows must not claim `AUTHORIZED`, `ACTIVE`, or `EXECUTED`.

Allowed access classes:

```text
STRUCTURAL_PARSE_ALLOWED
GOVERNANCE_METADATA_READ_ALLOWED
HASH_AND_STRUCTURE_COMPARE_ALLOWED
HUMAN_SUPERVISED_CLINICAL_READ
NO_CONTENT_ACCESS
EXTERNAL_ACCESS_FORBIDDEN
```

`HUMAN_SUPERVISED_CLINICAL_READ` is listed only for future TASK-B04 scoping. It remains inactive for planning review and for all B-Core tasks.

## 5. B-Core scope

### 5.1 TASK-B01 targets

```text
CLIN-001..CLIN-010
CLIN-020..CLIN-029
DATA-K001..DATA-K020
PROMPT-001..PROMPT-015
```

Expected total: **55 structural targets**.

### 5.2 TASK-B02 targets

```text
DATA-K001..DATA-K020
DATA-DR001..DATA-DR006
CLIN-001..CLIN-010
CLIN-020..CLIN-029
PHI-001..PHI-006
```

Expected total: **52 governance targets**.

### 5.3 TASK-B03 targets

```text
PROMPT-001
PROMPT-002
MODEL-004
MODEL-005
DOC-001
DOC-002
DOC-003
```

Expected total: **7 duplication/conflict targets**.

These are **7 targets**, not 7 comparison pairs. Suspected-duplicate relationships must be generated from known relations, shared consumers, shared responsibility, shared configuration keys, shared AST/template structure, or manual architecture candidates. Do not treat all C(7,2)=21 pairwise combinations as meaningful behavior comparisons by default.

Cross-task asset overlap among B01/B02/B03 is expected and must not be misread as Inventory duplication.

### 5.4 Dependency graph (planning)

Backlog dependencies (authoritative):

```text
TASK-B01 depends on TASK-A01
TASK-B02 depends on TASK-A02
TASK-B03 depends on TASK-B01
TASK-B04 depends on TASK-A03 and TASK-B01
TASK-C01 depends on TASK-B01
TASK-C02 depends on TASK-B01 and TASK-B04
TASK-C03 depends on TASK-C01
```

Execution shape after separate B-Core authorization:

```text
Gate-B0
├── TASK-B01
└── TASK-B02

TASK-B01
└── TASK-B03

TASK-A03 + TASK-B01 + Clinical Owner + scoped authorization
└── TASK-B04
```

Notes:

- B02 need not formally wait for B01 completion.
- B03 must wait for B01.
- B04 is not unlocked by merging this planning PR or by completing B-Core alone.
- B04 blocks TASK-C02; it must not be described as automatically blocking all A6.5-C tasks.
- This plan does not authorize any A6.5-C task.

## 6. Content-access boundary

### Allowed

- Parse Python with `ast.parse` without importing target modules.
- Parse YAML with safe loaders.
- Parse JSON using standard parsers.
- Parse CSV/TSV in streaming mode and emit only header/schema/row-count metadata.
- Parse XML using streaming/iterative parsing and emit element/attribute structure only.
- Parse Jinja templates and emit block/variable/filter **counts and hashes** only.
- Read repository-contained LICENSE, README and provenance declarations for identifier/path/hash metadata only (no full license-text copy into Evidence; no legal opinion).
- Hash files and compare normalized structure counts/hashes.

### Forbidden

- `eval`, `exec`, importing or running target business modules.
- Starting services or connecting to databases, Redis, Neo4j, object storage, logs or upload directories.
- Reading live/staging/production patient data.
- Emitting medical data rows, patient examples, clinical rule bodies, thresholds, prompt bodies, diagnosis logic, raw string literals, raw clinical identifiers, raw headings, or content excerpts into evidence artifacts.
- Running DR.KNOWS, downloading weights/data or making model/API calls.
- Treating parse success as clinical correctness.
- Treating a LICENSE file as product-use approval.
- Activating `HUMAN_SUPERVISED_CLINICAL_READ` outside a separately authorized B04 task.

Mandatory statements:

```text
Patient-data content inspected: no
Live/staging stores accessed: no
Clinical content approved: no
External model/API called: no
```

## 7. TASK-B01 implementation model

Recommended parsers and redacted outputs:

```text
.py      -> ast.parse
           allow: parse status; AST/node/function/class/assignment/import counts; content hash
           forbid: raw string literals; rule expressions; thresholds; bodies; comments; docstrings;
                   raw clinical-meaning function/class names
           if identifiers needed: identifier_count / identifier_hashes / allowlisted non-clinical only
.yaml/.json -> safe parsers
           allow: parse status; top-level type; field count; nesting depth; value-type distribution; schema hash
           forbid: raw values; rule text; thresholds; prompt fragments; medical terms; samples
.csv/tsv -> streaming parser
           allow: encoding; delimiter; column count; record count; schema hash
           forbid: row samples; cell values; medical entries; patient identifiers
.xml     -> streaming/iterparse
           allow: encoding; element count; attribute count; schema hash
           forbid: raw XML text; medical entries; patient identifiers
.jinja2  -> Jinja parser
           allow: parse status; node-type counts; block/variable/filter counts; template hash
           forbid: Prompt Body; clinical semantic variable names; literal text; clinical instruction fragments
.md/txt  -> structure-only
           allow: encoding; line count; heading count; link count; structure hash
           forbid: raw headings; medical paragraphs; rule text; clinical recommendations
```

Principle:

```text
parse content locally
persist structure only
emit no clinical/medical content
```

Recommended artifact:

```text
docs/refactoring/evidence/phase-a/a6-5/legacy-design-code-evidence.csv
```

Backlog lists this as a partial deliverable. B01 creates the initial version. Later phases may only append or update by stable primary key (`evidence_id`); they must not invent incompatible same-name schemas or overwrite historical rows. Each update records `execution_id` or an equivalent version field.

Recommended fields:

```text
evidence_id
asset_id
path
task_id
format
parser
encoding
parse_status
top_level_type
structural_counts
schema_hash
content_hash
record_count
raw_identifier_output
string_literal_output
content_excerpt_emitted
external_dependency_required
failure_class
quarantine_action
reviewer_role
notes
```

Mandatory Evidence values for every B01 row:

```text
raw_identifier_output=no
string_literal_output=no
content_excerpt_emitted=no
```

Use `top_level_symbol_count` / `top_level_symbol_hashes` inside `structural_counts` when needed. Do not persist raw `top_level_symbols`.

Allowed `parse_status` values:

```text
PARSED_STRUCTURALLY
PARSE_FAILED_QUARANTINED
MISSING
NOT_APPLICABLE
BLOCKED_EXTERNAL_DEPENDENCY
STOPPED_SAFETY_BOUNDARY
```

Acceptance:

```text
55/55 targets have a structural conclusion
raw_identifier_output=no for every target
string_literal_output=no for every target
content_excerpt_emitted=no for every target
parse failures remain fail-closed
external model/API calls=0
clinical correctness claims=0
```

## 8. TASK-B02 implementation model

B02 must keep three layers distinct:

```text
Observed Evidence
Provisional Classification
Human Decision
```

B02 may complete the first two layers. It must not pretend human Legal/Clinical/Product approval is complete.

Recommended artifact:

```text
docs/refactoring/evidence/phase-a/a6-5/a6-5-b-governance-evidence.csv
```

Primary key: `governance_id` (globally unique within the artifact). Append/update by stable key only.

Recommended fields:

```text
governance_id
asset_id
path
task_id
assigned_owner
required_owner_role
license_status
license_evidence
provenance_status
provenance_evidence
privacy_status
clinical_status
permitted_use
forbidden_use
blocking_decision
unblock_evidence
reviewer_roles
status
notes
```

License/README reads may record only:

```text
source path
license identifier
publisher/version if present
evidence hash
missing fields
review required
```

Do not copy full License Text into Evidence and do not issue legal opinions.

### License states

```text
LICENSE_IDENTIFIED_NOT_REVIEWED
LICENSE_REVIEW_REQUIRED
LICENSE_RESTRICTED
LICENSE_INCOMPATIBLE
LICENSE_NOT_FOUND
NOT_APPLICABLE
```

Do **not** auto-emit `LEGAL_APPROVED`, `PRODUCT_APPROVED`, or `ELIGIBLE_FOR_KNOWLEDGE_RELEASE` unless independent human Legal Decision Evidence already exists.

### Provenance states

```text
PROVENANCE_COMPLETE
PROVENANCE_PARTIAL
PROVENANCE_UNKNOWN
DERIVED_SOURCE_CHAIN_INCOMPLETE
```

`PROVENANCE_COMPLETE` means provenance fields are complete. It does not mean medical correctness or product usability.

### Privacy states

```text
PATH_LEVEL_ONLY
NO_CONTENT_INSPECTION
PRIVACY_REVIEW_REQUIRED
AUTHORIZED_METADATA_ONLY
```

PHI paths remain path-level only. Do not access Runtime Stores to “verify” privacy states.

### Clinical states

```text
CLINICAL_REVIEW_REQUIRED
STRUCTURALLY_REVIEWED_ONLY
NOT_CLINICALLY_VALIDATED
NOT_APPLICABLE
```

Do **not** use `CLINICALLY_VALIDATED` or `CLINICALLY_APPROVED`.

### Record status

```text
EVIDENCE_RECORDED_FAIL_CLOSED
NEEDS_LEGAL_REVIEW
NEEDS_PRIVACY_REVIEW
NEEDS_CLINICAL_REVIEW
NEEDS_OWNER
NOT_APPLICABLE
STOPPED
```

Rules:

- Do not invent personal owner names.
- `assigned_owner=UNASSIGNED` is allowed; `required_owner_role` must remain explicit.
- `owner_role=UNASSIGNED` / `assigned_owner=UNASSIGNED` does **not** mean “no owner required”.
- B02 may add evidence and containment notes but must not automatically close RISK-001..RISK-005, RISK-008, OD-001..OD-006, or F-A65A-R09.
- Approved medical source count remains 0 unless a separate human legal/clinical governance process records approval.

## 9. TASK-B03 implementation model

Recommended methods:

```text
SHA-256 exact hash
normalized text hash
AST structural hash
template structure hash
symbol-count comparison (counts/hashes only; no raw clinical identifiers)
consumer/reference overlap
configuration-key comparison
manual architecture difference note
```

Relationship generation for the 7 targets:

```text
known relations
same consumer
same responsibility
same configuration key
same AST/template structure
manual architecture candidate
```

Hard distinctions:

```text
text similarity != behavior equivalence
structure similarity != clinical equivalence
hash mismatch != distinct responsibility
```

Recommended artifact:

```text
docs/refactoring/evidence/phase-a/a6-5/a6-5-b-duplication-conflict-register.csv
```

Primary key: `conflict_id` (globally unique within the artifact).

Recommended fields:

```text
conflict_id
asset_id_a
asset_id_b
path_a
path_b
comparison_method
exact_hash_match
structural_similarity
consumer_overlap
behavior_equivalence
conflict_type
sot_candidate
sot_decision_status
decision_owner_role
required_owner_role
required_evidence
status
notes
```

Defaults and allowed values:

```text
behavior_equivalence default: NOT_EVALUATED
  (change only with separate non-clinical behavior evidence)

sot_candidate / sot_decision_status:
  CANDIDATE
  CONFLICTING
  INSUFFICIENT_EVIDENCE
  DISTINCT
```

Allowed conclusions:

```text
DUPLICATE_EXACT
DUPLICATE_STRUCTURAL
OVERLAPPING_RESPONSIBILITY
CONFLICTING
DISTINCT
INSUFFICIENT_EVIDENCE
```

Do not silently merge, delete, or modify duplicate implementations. Do not modify Prompts or model behavior. Do not select a production SoT.

## 10. TASK-B04 unlock gate

TASK-B04 remains `BLOCKED` until **all** conditions are met:

1. Repository Owner separately authorizes TASK-B04 (distinct from B-Core authorization).
2. An accountable Clinical Owner Role is explicitly assigned.
3. A Human Clinical Reviewer Role is explicitly assigned.
4. Written content-access authorization exists.
5. Access scope is limited to exactly these nine registered assets:

```text
CLIN-020
CLIN-021
CLIN-022
CLIN-025
CLIN-026
PROMPT-001
PROMPT-003
PROMPT-004
PROMPT-008
```

6. B01 has structurally validated all nine files.
7. No step requires patient-data access.
8. Capability Package mutation is explicitly forbidden.
9. Approved rules/thresholds/hypotheses/sources remain 0.
10. A separate B04 branch and Draft PR are created.
11. Exit and stop conditions for B04 are explicit; work is human-supervised.

Hard distinction:

```text
Repository Owner technical authorization
!=
Clinical Owner approval
```

`HUMAN_SUPERVISED_CLINICAL_READ` may be activated only by that separately authorized B04 task. Planning review and B-Core must keep it inactive.

B04 must not be appended to the B-Core implementation PR. Planning review must not create `legacy-clinical-policy-extraction.md`.

When unlocked, the B04 artifact is:

```text
docs/refactoring/evidence/phase-a/a6-5/legacy-clinical-policy-extraction.md
```

Every extracted item must state:

```text
Candidate only
Not clinically validated
Not approved
Not mapped into Capability
Not eligible for Runtime
Not eligible for Production
```

## 11. B-Core deliverables

```text
docs/refactoring/evidence/phase-a/a6-5/
├── a6-5-b-access-manifest.csv
├── legacy-design-code-evidence.csv
├── a6-5-b-governance-evidence.csv
├── a6-5-b-duplication-conflict-register.csv
├── a6-5-b-validation-evidence.csv
└── a6-5-b-structural-and-governance-report.md
```

Allowed planning/evidence updates:

```text
docs/refactoring/plans/phase-a/a6-5/
├── a6-5-implementation-backlog.csv
├── a6-5-validation-matrix.csv
├── a6-5-risk-register.csv
├── a6-5-decision-log.md
└── a6-5-legacy-asset-inventory.csv
```

Updates are limited to execution status, evidence pointers, owner/license/provenance/privacy states, risk containment and open-decision evidence. Do not delete historical rows; append or update by stable keys only.

### 11.1 Stable Evidence keys

```text
Access Manifest:        access_id
Structural Evidence:    evidence_id
Governance Evidence:    governance_id
Conflict Register:      conflict_id
Validation Evidence:    execution_id + validation_id
```

Integrity rules:

```text
IDs globally unique within artifact
asset_id references Inventory
task_id references Backlog
evidence_reference resolves
no overwrite of historical Evidence without version/execution_id
```

## 12. Explicitly out of scope

- Application source changes.
- Shared Contract changes.
- Capability Package content or status changes.
- Runtime registration or inference.
- Database migrations or access.
- Patient API/UI work.
- Clinical approval.
- Medical-source approval.
- Asset migration, deletion, rename or decommission.
- A6.5-C/D/E implementation.
- A7 implementation.

## 13. Branch and PR strategy

### Planning branch

```text
agent/phase-a6-5-b-structural-provenance-validation-plan
```

This document is planning only and must be reviewed before B-Core implementation.

### B-Core implementation branch

```text
agent/phase-a6-5-b-structural-governance
```

Exact base must be the then-current verified Enterprise Head.

Suggested Draft PR title:

```text
docs(a6.5-b): validate legacy structures and governance evidence
```

### Independent review branch

```text
agent/phase-a6-5-b-independent-review
```

### B04 branch

Only after unlock:

```text
agent/phase-a6-5-b04-clinical-extraction
```

B04 must not be appended to the B-Core implementation PR.

## 14. Validation

Every implementation/review Head must run:

```bash
python -m pip check
python capabilities/validator/validate_capability.py
python -m pytest capabilities/tests/test_capability.py -v

git diff --check
git diff --check <exact-enterprise-base>...HEAD
```

A6 regression baseline:

```text
Validator: 11 schemas / 25 assets / 5 eval cases / 0 issues
Pytest: 59 passed
```

B-Core integrity expectations:

```text
Structural targets: 55/55
Governance targets: 52/52
Duplication/conflict targets: 7/7
Duplicate evidence IDs: 0
Broken asset references: 0
Broken task references: 0
Unknown access classes: 0
PHI content-access violations: 0
Clinical excerpts emitted by B01-B03: 0
External model/API calls: 0
Approved clinical rules: 0
Approved thresholds: 0
Approved hypotheses: 0
Approved medical sources: 0
```

Local verification must not be called CI PASS when no CI is configured.

## 15. Stop conditions

Stop immediately on:

```text
POTENTIAL_REAL_PATIENT_DATA_FOUND
UNAUTHORIZED_CONTENT_ACCESS_REQUIRED
UNLICENSED_ACTIVE_PRODUCTION_USE_FOUND
EXTERNAL_DEPENDENCY_REQUIRED
B01_CONTENT_LEAKAGE_RISK
LICENSE_EVIDENCE_CONFLICT
TASK_B04_NOT_AUTHORIZED
ENTERPRISE_TARGET_MOVED
WORKTREE_NOT_SAFE
A6_5_B_REGRESSION_FAILED
```

Also stop if:

- A parser would emit medical or clinical content samples.
- A task requires external models, downloads or paid APIs.
- Runtime/Capability/Contract changes are required to continue.
- A conflicting A6.5-B branch or PR appears.

## 16. Exit gates

### B-Core success

```text
Gate-B0 completed
TASK-B01: 55/55 structurally concluded
TASK-B02: 52/52 governance concluded
TASK-B03: 7/7 duplicate/conflict concluded
All failures remain fail-closed
PHI content access: 0
Clinical content emitted by B01-B03: 0
External model/API calls: 0
A6 regression: passed
Independent review: passed
Enterprise post-merge verification: passed
```

Final B-Core state:

```text
A6_5_B_CORE_MERGED_AND_VERIFIED
A6.5-B Overall: PARTIALLY_COMPLETE
TASK-B04: BLOCKED
A6.5-C: NOT_STARTED
```

### B04 readiness

```text
READY_FOR_TASK_B04_IMPLEMENTATION
```

Only after all unlock conditions are satisfied.

### Full B completion

```text
A6_5_B_MERGED_AND_VERIFIED
```

Only after B-Core and B04 have both been independently reviewed, merged and verified.

## 17. Explicit non-claims

This phase does not claim:

- structural parse success equals clinical correctness;
- a LICENSE file equals legal/product approval;
- provenance evidence equals medical-source approval;
- candidate extraction equals clinical approval;
- prompt similarity equals behavioral equivalence;
- path-level quarantine equals runtime-store safety;
- DR.KNOWS is runnable or product-eligible;
- A6 Capability has approved clinical content;
- Runtime is implemented;
- Production is ready;
- local verification is CI PASS.

## 18. Recommended sequence

```text
1. Complete independent plan review / remediation merge into the planning branch.
2. Human-approve and merge the planning PR to Enterprise; verify.
3. Separately authorize B-Core only (does not authorize B04).
4. Execute Gate-B0 (Current Implementation Status Addendum + Access Manifest).
5. Execute TASK-B01 and TASK-B02 (B02 may proceed without waiting on B01).
6. Execute TASK-B03 after B01.
7. Perform independent B-Core review.
8. Merge B-Core to Enterprise and verify → A6_5_B_CORE_MERGED_AND_VERIFIED;
   A6.5-B Overall remains PARTIALLY_COMPLETE while TASK-B04 is BLOCKED.
9. Assess Repository Owner + Clinical Owner + Human Reviewer + scoped B04 authorization separately.
10. Keep TASK-B04 BLOCKED until every unlock condition is satisfied.
11. Only after B-Core and B04 are both independently reviewed, merged and verified:
    A6_5_B_MERGED_AND_VERIFIED.
```

## 19. Planning recommendation

```text
READY_FOR_A6_5_B_PLAN_REVIEW
```

This recommendation authorizes review of this plan only. It does not authorize B-Core implementation or TASK-B04. After independent review remediation lands on the planning branch, the human reviewer may treat the planning PR as ready for approval review; implementation remains `NOT_STARTED`.
