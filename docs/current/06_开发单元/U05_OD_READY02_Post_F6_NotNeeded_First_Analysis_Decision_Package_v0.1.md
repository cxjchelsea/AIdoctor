# U05 OD-U05-READY-02 Owner Policy Decision Package v0.1

> Decision ID: `OD-U05-READY-02`  
> Target finding: `BF-U05-F6R-TR-03`  
> Design baseline: `268d9c5e9075420aa76c5c1aa37c254a3a99e2b8`  
> Finding source: PR #160 exact head `48783988f127f7aa5ded775db28a75f023dc074b`, review `5263189477`  
> Decision status: **NOT_DECIDED / REVISED_FOR_TARGETED_REVIEW**  
> Scope: Owner policy decision preparation only; no frozen amendment or implementation authorization.

---

## 1. Why a new Owner decision is required

CL-04 F6 mutation-stale remediation can lawfully produce this current profile:

```text
evaluation_context = POST_USER_FACT_UPDATE

F1 = PRESENT / FRAMED_IN_SCOPE
F3 = PRESENT / CURRENT / NO_ACTIVE_ONLINE_BLOCKING_GAP
F5 applicability = NOT_YET_APPLICABLE

F6 = PRESENT / CURRENT
F6 Assessment = VALID
F6 business signal = NO_BLOCKING_OFFLINE_EVIDENCE_NEED

no OUT_OF_SCOPE
no NEEDS_CLARIFICATION
no CAN_ASK_MORE
no qualified blocking offline signal
no required input failure / stale / unavailable
Safety = ALLOW or action-permitted RESTRICTED
```

This profile is not a technical failure and is not post-DDx.

But the currently frozen positive first-entry rule is:

```text
D03-POL-005
policy_scope = FIRST_CLINICAL_ANALYSIS_ENTRY_ONLY

F1 = FRAMED_IN_SCOPE
+ F3 = NO_ACTIVE_ONLINE_BLOCKING_GAP
+ F5 = NOT_YET_APPLICABLE
+ F6 = NOT_YET_APPLICABLE
+ no higher blocker
-> READY_FOR_CLINICAL_ANALYSIS
```

The frozen contract explicitly says:

```text
F6 PRESENT as an applicable producer result
-> D03-POL-005 NOT_APPLICABLE
```

Therefore the lawful profile above currently has no unique D03 result and no existing non-D03 continuation consequence.

```text
POLICY_EXPECTATION_GAP
= legally triggerable
```

This cannot be solved by implementation code or test expectation invention.

---

## 2. Non-negotiable preserved semantics

Any Owner decision must preserve:

```text
U05/D03 = unique Clinical Readiness Resolver

READY_FOR_CLINICAL_ANALYSIS
= permission to enter governed U08/F5 first clinical analysis only

READY_FOR_CLINICAL_ANALYSIS
!= diagnosis
!= safety
!= delivery
!= completion
!= proof that no future evidence need exists

F6 NO_BLOCKING_OFFLINE_EVIDENCE_NEED
!= READY by itself

F6 VALID + JUSTIFIED blocking need
-> retains NEEDS_OFFLINE_EVIDENCE precedence

F5 PRESENT / post-DDx
-> must not use a first-entry READY shortcut

FAILED / STALE / UNAVAILABLE
-> never treated as NOT_NEEDED or READY
```

No option may change the six-value Clinical Readiness vocabulary.

---

## 3. Decision question

Owner must decide:

> When first clinical analysis has not yet begun, F5 is still NOT_YET_APPLICABLE, but F6 has already been lawfully activated and currently proves NO_BLOCKING_OFFLINE_EVIDENCE_NEED, may D03 still resolve READY_FOR_CLINICAL_ANALYSIS when all other first-entry positive guards are satisfied?

This is a policy-scope decision, not a new clinical truth assertion.

The package does not decide it automatically.

### 3.1 Executable first-entry eligibility axis

For this decision package, `FIRST_CLINICAL_ANALYSIS_ENTRY` is not inferred from step number or from the absence of an F5 artifact.

It requires:

```text
F5 applicability = NOT_YET_APPLICABLE
= F5 has never been lawfully activated in this Consultation path

+ current intent is entry to the first governed U08/F5 clinical-analysis cycle
+ current Safety/admission permits that entry
```

The candidate rule may be evaluated only in these frozen evaluation contexts:

```text
A1_POST_BARRIER_CURRENT
POST_USER_FACT_UPDATE
POST_OFFLINE_ASSESSMENT
```

with context-specific guards:

```text
A1_POST_BARRIER_CURRENT
-> A1 bootstrap/barrier/current-F3 requirements complete
-> F5 still NOT_YET_APPLICABLE

POST_USER_FACT_UPDATE
-> accepted mutation/correction provenance current
-> required mutation-stale dependencies resolved/revalidated
-> F5 still NOT_YET_APPLICABLE

POST_OFFLINE_ASSESSMENT
-> current F6 assessment is complete/current
-> this is a pre-F5 offline-assessment return
-> F5 still NOT_YET_APPLICABLE
```

Important:

```text
POST_OFFLINE_ASSESSMENT
!= POST_DDX_REEVALUATION

POST_OFFLINE_ASSESSMENT
does not by itself prove F5 has already activated.
```

The rule is NOT eligible in:

```text
POST_DDX_REEVALUATION

any context where F5 is:
PRESENT
STALE
INVALIDATED
FAILED
UNAVAILABLE

or any provenance proves prior F5 activation
```

Therefore:

```text
F5 NOT_YET_APPLICABLE
!= "no current F5 artifact"
= "F5 has never lawfully activated on this Consultation path"
```

This first-entry eligibility axis applies to both Option A and Option B.

---

## 4. Option A — add a separate narrow first-entry rule

Proposed rule identity:

```text
D03-POL-011
policy_scope = FIRST_CLINICAL_ANALYSIS_ENTRY_AFTER_CURRENT_F6_NOT_NEEDED
```

Exact proposed guards:

```text
first-entry eligibility = Section 3.1 SATISFIED
evaluation_context in:
  A1_POST_BARRIER_CURRENT
  POST_USER_FACT_UPDATE
  POST_OFFLINE_ASSESSMENT

F1 = PRESENT / FRAMED_IN_SCOPE

F3 = PRESENT / CURRENT
F3 business signal = NO_ACTIVE_ONLINE_BLOCKING_GAP

F5 applicability = NOT_YET_APPLICABLE

F6 = PRESENT / CURRENT
F6 Assessment = VALID
F6 business signal = NO_BLOCKING_OFFLINE_EVIDENCE_NEED

no qualified blocking offline signal
no lawful NEEDS_CLARIFICATION
no OUT_OF_SCOPE
no CAN_ASK_MORE
no required input failure / stale / unavailable
all required provenance/currentness/admission checks pass

-> DECIDED / READY_FOR_CLINICAL_ANALYSIS
```

### 4.1 Explicit exclusions

D03-POL-011 would be NOT_APPLICABLE when:

```text
POST_DDX_REEVALUATION
F5 has any prior activation provenance
F5 is PRESENT / STALE / INVALIDATED / FAILED / UNAVAILABLE
POST_OFFLINE_ASSESSMENT with prior/current F5 activation
F6 is STALE / FAILED / UNAVAILABLE
F6 has JUSTIFIED blocking offline evidence need
F3 is not current
F3 = CAN_ASK_MORE
OUT_OF_SCOPE or NEEDS_CLARIFICATION is present
Safety does not permit current ordinary first-analysis entry
```

### 4.2 Relationship to D03-POL-005

```text
D03-POL-005
= first entry where F6 has not legally activated

D03-POL-011
= first entry where F6 has legally activated
  and has a current VALID / NO_BLOCKING_OFFLINE_EVIDENCE_NEED result
```

They are mutually exclusive on F6 applicability:

```text
D03-POL-005: F6 = NOT_YET_APPLICABLE
D03-POL-011: F6 = PRESENT / VALID / NOT_NEEDED
```

Therefore Option A preserves the exact frozen D03-POL-005 meaning.

### 4.3 Policy precedence

D03-POL-011 must occupy the existing positive READY position only after all higher business blockers have been evaluated.

It must not precede:

```text
OUT_OF_SCOPE
blocking NEEDS_OFFLINE_EVIDENCE
NEEDS_CLARIFICATION
CAN_ASK_MORE
```

It must not override:

```text
D03-POL-006 NO_RELIABLE_DIRECTION
```

because F5 PRESENT / NO_RELIABLE_DIRECTION makes D03-POL-011 non-applicable.

### 4.4 Governance impact

If approved, Option A requires a controlled amendment at least to:

```text
U05-RDP-02
U05-RDP-05
Phase 5 policy semantics as needed
Phase 6 U05 verification semantics as needed
Phase 8 deterministic-decision contract/evidence cases as needed
```

The exact artifact inventory must be reconciled with the CL-04 seven-artifact amendment before authorization so overlapping files are amended once under one controlled inventory.

---

## 5. Option B — broaden D03-POL-005

Alternative:

```text
D03-POL-005
policy_scope = FIRST_CLINICAL_ANALYSIS_ENTRY_ONLY
```

would amend its F6 guard from:

```text
F6 applicability = NOT_YET_APPLICABLE
```

to an equivalent form such as:

```text
F6 applicability = NOT_YET_APPLICABLE
OR
F6 = PRESENT / CURRENT / VALID / NO_BLOCKING_OFFLINE_EVIDENCE_NEED
```

while retaining every other first-entry guard and the exact Section 3.1 first-entry eligibility/context contract.

Option B must not interpret "F6 PRESENT is now allowed" as permission in post-DDx or prior-F5-activation contexts.

### 5.1 Advantage

One rule represents the general first-analysis positive condition.

### 5.2 Governance cost

This changes an already Owner-approved and explicitly re-frozen executable expectation.

The current contract deliberately states:

```text
F6 PRESENT as an applicable producer result
-> D03-POL-005 NOT_APPLICABLE
```

Therefore Option B has a larger semantic diff and greater regression surface across the existing READY-policy evidence.

If selected, all prior D03-POL-005 verification assumptions involving the F6 applicability guard must be re-reviewed.

---

## 6. Option C — direct continuation routing to U08

Proposal:

```text
F5 = NOT_YET_APPLICABLE
+ F6 current NOT_NEEDED
+ F3 current no-gap
-> ClinicalContinuationRoutingDecision
-> directly U08
```

Disposition:

```text
REJECT AS DESIGN OPTION
```

Reason:

```text
it bypasses U05/D03 as unique Clinical Readiness Resolver
and makes the continuation router a second positive readiness owner.
```

---

## 7. Option D — reinterpret F6 PRESENT/NOT_NEEDED as NOT_YET_APPLICABLE

Disposition:

```text
REJECT AS DESIGN OPTION
```

Reason:

```text
NOT_YET_APPLICABLE
= domain has not legally activated

PRESENT / VALID / NOT_NEEDED
= domain has legally activated and produced a current business result
```

Collapsing them would violate frozen RDP-05 applicability semantics and erase provenance.

---

## 8. Option E — declare the profile impossible

Disposition:

```text
REJECT AS DESIGN OPTION
```

Reason:

F6 lifecycle and correction semantics already allow:

```text
F6 activated
-> user fact/correction mutation
-> F6 STALE
-> governed F6 reassessment
```

F6 may be activated from a lawful NEEDS_OFFLINE_EVIDENCE path before any successful F5/DDx activation.

Therefore:

```text
F5 NOT_YET_APPLICABLE
+ F6 PRESENT
```

cannot be dismissed as impossible without contradicting current frozen state/loop semantics.

---

## 9. Decision comparison

| Dimension | Option A — new D03-POL-011 | Option B — broaden D03-POL-005 |
|---|---|---|
| Clinical Readiness vocabulary | unchanged | unchanged |
| D03 remains unique Resolver | yes | yes |
| Existing D03-POL-005 exact semantics | preserved | amended |
| New rule identity | yes | no |
| First-entry meaning | explicit subcase | generalized in existing rule |
| F6 applicability distinction | explicit | folded into one rule |
| Post-DDx exclusion required | yes | yes |
| Existing D03-POL-005 regression surface | smaller | larger |
| New verification cases required | yes | yes |
| Owner decision required | yes | yes |

This comparison is architectural/governance analysis only. It is not Owner approval.

---

## 10. Proposed decision target for Owner consideration

For a narrow controlled change, the package proposes Option A as the candidate to review for Owner decision:

```text
OD-U05-READY-02 candidate
= Option A

D03-POL-011
= FIRST_CLINICAL_ANALYSIS_ENTRY_AFTER_CURRENT_F6_NOT_NEEDED
```

Rationale:

```text
preserve already re-frozen D03-POL-005 exactly
+ represent the newly discovered non-overlapping legal profile explicitly
+ keep U05/D03 ownership unchanged
+ minimize regression ambiguity
```

This is a proposal for decision, not an approved rule.

---

## 11. Required evidence if Owner approves Option A

A later controlled amendment/review must prove at least:

```text
CASE-R02-01
F5 NOT_YET_APPLICABLE
+ F6 NOT_YET_APPLICABLE
+ first-entry positive guards
-> D03-POL-005
-> READY

CASE-R02-02
F5 NOT_YET_APPLICABLE
+ F6 PRESENT / VALID / NOT_NEEDED
+ first-entry positive guards
-> D03-POL-011
-> READY

CASE-R02-03
F6 PRESENT / VALID / JUSTIFIED blocking need
-> D03-POL-011 not applicable
-> NEEDS_OFFLINE_EVIDENCE precedence

CASE-R02-04
F6 STALE / FAILED / UNAVAILABLE
-> no READY rule

CASE-R02-05
F5 PRESENT / ANALYSIS_RESULT_AVAILABLE
-> D03-POL-011 not applicable
-> governed post-DDx routing

CASE-R02-06
F5 PRESENT / REASSESSMENT_REQUIRED
-> D03-POL-011 not applicable
-> governed post-DDx routing

CASE-R02-07
F5 PRESENT / NO_RELIABLE_DIRECTION
-> D03-POL-011 not applicable
-> D03-POL-006 remains authoritative

CASE-R02-08
F3 CAN_ASK_MORE
+ F6 NOT_NEEDED
-> CAN_ASK_MORE
-> no READY shadowing

CASE-R02-09
NEEDS_CLARIFICATION or OUT_OF_SCOPE
+ F6 NOT_NEEDED
-> higher rule wins

CASE-R02-10
RESTRICTED without explicit first-analysis permission
-> no READY-to-U08 execution eligibility

CASE-R02-11
A1_POST_BARRIER_CURRENT
+ F5 never activated
+ F6 PRESENT / VALID / NOT_NEEDED
+ all positive guards
-> candidate positive rule eligible

CASE-R02-12
POST_USER_FACT_UPDATE
+ F5 never activated
+ F6 PRESENT / VALID / NOT_NEEDED
+ mutation/currentness guards complete
-> candidate positive rule eligible

CASE-R02-13
POST_OFFLINE_ASSESSMENT
+ pre-F5 path
+ F5 never activated
+ F6 PRESENT / VALID / NOT_NEEDED
+ all positive guards
-> candidate positive rule eligible

CASE-R02-14
POST_DDX_REEVALUATION
-> candidate positive rule NOT_APPLICABLE

CASE-R02-15
prior F5 activation exists
+ current F5 artifact absent/stale/invalidated
-> must not relabel F5 as NOT_YET_APPLICABLE
-> candidate positive rule NOT_APPLICABLE
```

---

## 12. Relationship to CL-04

`OD-U05-READY-02` does not replace the F6 mutation-stale remediation.

The complete path would remain:

```text
F6 mutation-stale
-> TO_F6_CURRENT_VERSION_REASSESSMENT
-> U10 / C05 / F6 Owner
-> canonical F6 commit
-> mandatory post-F6 Safety barrier
-> F6_CURRENT_VERSION_REVALIDATION
-> current F6 readiness input
-> ClinicalContinuationRoutingDecision
-> TO_U05_CLINICAL_READINESS when an actual D03 path exists
-> D03
```

If current F6 becomes:

```text
VALID / JUSTIFIED
```

the existing blocking offline path remains authoritative.

If current F6 becomes:

```text
VALID / NO_BLOCKING_OFFLINE_EVIDENCE_NEED
```

and F5 remains NOT_YET_APPLICABLE, an Owner-approved READY rule is required before D03 can lawfully return READY.

Thus:

```text
F6 recomputation design
!= READY policy decision
```

Both are required to close CL-04.

---

## 13. Owner decision options

After independent review of this package, Owner may choose exactly one:

```text
APPROVE_OPTION_A
APPROVE_OPTION_B
REVISE
REJECT
```

Until an explicit Owner decision:

```text
OD-U05-READY-02 = NOT_DECIDED
D03-POL-011 = PROPOSED_ONLY
D03-POL-005 = unchanged / frozen
BF-U05-F6R-TR-03 = OPEN / BLOCKING
BF-U05-RG02-CL-04 = OPEN / BLOCKING
BF-U05-RG-02 = NOT_CLOSED
U05 Implementation Readiness = NOT_READY
```

---

## 14. Authorization boundary

This package does not authorize:

```text
D03-POL-011
D03-POL-005 amendment
frozen artifact changes
CL-04 frozen amendment
U05/U10 runtime implementation
C05 activation
merge
production/live routing
release activation
real-patient traffic
```

Current permitted next step:

```text
Independent Review
-> if required, targeted design remediation
-> if PASS, OD-U05-READY-02 becomes READY_FOR_OWNER_DECISION
```


---

## 15. Independent Review Remediation

Review source:

```text
PR #161
reviewed head = 40a9a7ebfc2804d094b87c38818328530536c996
review_id = 5263202893
verdict = REVISE_REQUIRED
```

### BF-U05-READY02-IR-01

```text
FIRST_ENTRY_EVALUATION_CONTEXT_SET_UNDERDEFINED
-> REMEDIATED
```

The package now freezes an executable first-entry eligibility axis:

```text
F5 = NOT_YET_APPLICABLE
= never lawfully activated in this Consultation path
```

and explicitly allows only:

```text
A1_POST_BARRIER_CURRENT
POST_USER_FACT_UPDATE
POST_OFFLINE_ASSESSMENT
```

subject to their context-specific admission/currentness guards.

`POST_DDX_REEVALUATION` and all prior-F5-activation profiles are explicitly excluded.

### RQ-U05-READY02-IR-02

```text
CROSS_CONTEXT_VERIFICATION_MATRIX_REQUIRED
-> REMEDIATED
```

The evidence matrix now covers each allowed first-entry context and proves exclusion of post-DDx and prior-F5-activation profiles.

Current:

```text
OD-U05-READY-02
= NOT_DECIDED

Decision Package
= REVISED / TARGETED_INDEPENDENT_REVIEW_PENDING
```
