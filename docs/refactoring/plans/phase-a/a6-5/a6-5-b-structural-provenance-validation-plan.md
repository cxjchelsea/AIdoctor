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

B-Core may proceed independently. TASK-B04 remains blocked until a clinical owner role and scoped content-access authorization exist.

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

Before reading or parsing target content:

1. Reconcile implementation status:
   - TASK-A01/A02/A03 → `MERGED_AND_VERIFIED`.
   - TASK-B01/B02/B03 → `AUTHORIZED_FOR_IMPLEMENTATION` only after explicit authorization.
   - TASK-B04 → `BLOCKED`.
2. Preserve OD-001..OD-006 as unresolved/fail-closed.
3. Create an access manifest for every B-Core target.
4. Confirm PHI runtime paths remain `NO_CONTENT_ACCESS`.
5. Confirm external downloads, external models, paid APIs and live/staging access are forbidden.

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
allowed_operation
forbidden_operation
executor_role
required_reviewer_role
output_redaction
external_access
patient_data_risk
clinical_content_risk
status
evidence_reference
notes
```

Allowed access classes:

```text
STRUCTURAL_PARSE_ALLOWED
GOVERNANCE_METADATA_READ_ALLOWED
HASH_AND_STRUCTURE_COMPARE_ALLOWED
HUMAN_SUPERVISED_CLINICAL_READ
NO_CONTENT_ACCESS
EXTERNAL_ACCESS_FORBIDDEN
```

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

## 6. Content-access boundary

### Allowed

- Parse Python with `ast.parse` without importing target modules.
- Parse YAML with safe loaders.
- Parse JSON using standard parsers.
- Parse CSV/TSV in streaming mode and emit only header/schema/row-count metadata.
- Parse XML using streaming/iterative parsing and emit element/attribute structure only.
- Parse Jinja templates and emit block/variable/filter structure only.
- Read repository-contained LICENSE, README and provenance declarations.
- Hash files and compare normalized structure.

### Forbidden

- `eval`, `exec`, importing or running target business modules.
- Starting services or connecting to databases, Redis, Neo4j, object storage, logs or upload directories.
- Reading live/staging/production patient data.
- Emitting medical data rows, patient examples, clinical rule bodies, thresholds, prompt bodies or diagnosis logic into evidence artifacts.
- Running DR.KNOWS, downloading weights/data or making model/API calls.
- Treating parse success as clinical correctness.
- Treating a LICENSE file as product-use approval.

Mandatory statements:

```text
Patient-data content inspected: no
Live/staging stores accessed: no
Clinical content approved: no
External model/API called: no
```

## 7. TASK-B01 implementation model

Recommended parsers:

```text
.py      -> ast.parse
.yaml    -> yaml.safe_load
.json    -> json.load
.csv/tsv -> csv parser; header/schema/record count only
.xml     -> iterparse; element/attribute structure only
.jinja2  -> Jinja parser; blocks/variables/filters only
.md/txt  -> encoding/headings/references only
```

Recommended artifact:

```text
docs/refactoring/evidence/phase-a/a6-5/legacy-design-code-evidence.csv
```

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
schema_summary
top_level_symbols
reference_count
record_count
content_excerpt_emitted
external_dependency_required
failure_class
quarantine_action
evidence_hash
reviewer_role
notes
```

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
content_excerpt_emitted=no for every target
parse failures remain fail-closed
external model/API calls=0
clinical correctness claims=0
```

## 8. TASK-B02 implementation model

Recommended artifact:

```text
docs/refactoring/evidence/phase-a/a6-5/a6-5-b-governance-evidence.csv
```

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

### License states

```text
LICENSE_IDENTIFIED_NOT_REVIEWED
LICENSE_REVIEW_REQUIRED
LICENSE_RESTRICTED
LICENSE_INCOMPATIBLE
LICENSE_NOT_FOUND
NOT_APPLICABLE
```

### Provenance states

```text
PROVENANCE_COMPLETE
PROVENANCE_PARTIAL
PROVENANCE_UNKNOWN
DERIVED_SOURCE_CHAIN_INCOMPLETE
```

### Privacy states

```text
PATH_LEVEL_ONLY
NO_CONTENT_INSPECTION
PRIVACY_REVIEW_REQUIRED
AUTHORIZED_METADATA_ONLY
```

### Clinical states

```text
CLINICAL_REVIEW_REQUIRED
STRUCTURALLY_REVIEWED_ONLY
NOT_CLINICALLY_VALIDATED
NOT_APPLICABLE
```

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
- B02 may add evidence but must not automatically close RISK-001..RISK-005 or OD-001..OD-006.
- Approved medical source count remains 0 unless a separate human legal/clinical governance process records approval.

## 9. TASK-B03 implementation model

Recommended methods:

```text
SHA-256 exact hash
normalized text hash
AST structural hash
function/class/template-variable set comparison
consumer/reference overlap
configuration-key comparison
manual architecture difference note
```

Recommended artifact:

```text
docs/refactoring/evidence/phase-a/a6-5/a6-5-b-duplication-conflict-register.csv
```

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
decision_status
required_owner_role
required_evidence
status
notes
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

Do not silently merge, delete or modify duplicate implementations.

## 10. TASK-B04 unlock gate

TASK-B04 remains `BLOCKED` until all conditions are met:

1. An accountable clinical owner role is explicitly assigned.
2. Written content-access authorization exists.
3. Access scope is limited to exactly nine registered assets:

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

4. B01 has structurally validated all nine files.
5. A human clinical-review template and reviewer role are defined.
6. Patient-data access is explicitly excluded.
7. Capability Package mutation is explicitly excluded.
8. Approved rules/thresholds/hypotheses/sources remain 0.
9. A separate B04 branch and Draft PR are created.

Repository-owner technical authorization alone does not satisfy the clinical-owner requirement.

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

Updates are limited to execution status, evidence pointers, owner/license/provenance/privacy states, risk containment and open-decision evidence.

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
1. Merge and verify this planning PR.
2. Separately authorize B-Core only.
3. Execute Gate-B0.
4. Execute TASK-B01.
5. Execute TASK-B02.
6. Execute TASK-B03.
7. Perform independent B-Core review.
8. Merge B-Core to Enterprise and verify.
9. Assess clinical owner and B04 authorization separately.
10. Keep TASK-B04 BLOCKED until every unlock condition is satisfied.
```

## 19. Planning recommendation

```text
READY_FOR_A6_5_B_PLAN_REVIEW
```

This recommendation authorizes review of this plan only. It does not authorize B-Core implementation or TASK-B04.
