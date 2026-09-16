# U03 Gate C Independent Implementation Review Remediation v0.1

> Object: PR #89 targeted remediation after `U03_Gate_C_Independent_Implementation_Review_v0.1.md`  
> Scope: BF-IR-01 / BF-IR-02 only, plus N-IR-01 executable-boundary strengthening  
> Status: `REMEDIATED_PENDING_TARGETED_RE_REVIEW / NOT_GATE_C / NOT_FOR_PRODUCTION`  
> Governed Evaluation Execution: `NOT_STARTED`  
> Gate C: `NOT_PASSED`

---

## 1. Boundary

This remediation does not modify frozen clinical thresholds, evidence taxonomy, release refs, D09 outcome vocabulary, Runtime wiring, State Committer wiring, U04 wiring, or production activation.

It does not mark the workflow input `REVIEW_COMPLETE` and does not create governed Gate C execution evidence.

---

## 2. BF-IR-01 remediation

The prior implementation allowed fixture-provided `forced_rule_results` to replace the C evaluator output for GC-026 / SS-012. That bypass is removed.

Current rule:

```text
executable non-P0 C path
→ evaluate_rules()
→ exactly 15 frozen rule results
→ D09
```

No executable fixture contains `forced_rule_results` or `forced_family_conflict`.

The independent review already identified that the frozen C semantics require a single shared scope outcome for each conditional family. Therefore a valid C execution cannot naturally produce the prior mixed family state:

```text
SCOPE_MISMATCH + MATCHED
```

for the same shared-scope family.

Accordingly:

```text
GC-026 = UNPRODUCIBLE_UNDER_SHARED_SCOPE / EXCLUDED_FROM_EXECUTABLE_GC_COUNT
SS-012 = UNPRODUCIBLE_UNDER_SHARED_SCOPE / EXCLUDED_FROM_EXECUTABLE_SS_COUNT
```

The identities remain explicitly recorded; they are not silently deleted.

P5 is retained only as:

```text
D09_DEFENSIVE_CONTRACT_BOUNDARY_ONLY
```

The boundary check first executes the honest 15-rule C result set, proves it contains no shared-scope conflict, then constructs a deliberately malformed inter-component rule-result payload solely to verify that D09 fails closed to `D09-P-090 / UNRESOLVABLE_CONFLICT`. That malformed payload is explicitly marked `counts_as_governed_c_execution = false`.

This is not represented as a clinical Golden Case or Safety Suite execution.

---

## 3. BF-IR-02 remediation

GC-012 / GC-014 / GC-015 no longer share one identical `p4_fixture()`.

They now exercise three independent frozen P4 coverage states:

```text
GC-012
baseline 5 = APPLICABLE_EVALUATED / NO_MATCH
NHS dyspnoea family = NOT_APPLICABLE
NG253 sepsis family = NOT_APPLICABLE
→ D09-P-040

GC-014
baseline 5 = APPLICABLE_EVALUATED / NO_MATCH
NHS dyspnoea family = APPLICABLE_EVALUATED / all NO_MATCH
NG253 sepsis family = NOT_APPLICABLE
→ D09-P-040

GC-015
baseline 5 = APPLICABLE_EVALUATED / NO_MATCH
NHS dyspnoea family = NOT_APPLICABLE
NG253 sepsis family = APPLICABLE_EVALUATED / all NO_MATCH
→ D09-P-040
```

The test suite asserts that the three fixtures are pairwise different and that each produces 15 rule results and D09-P-040.

---

## 4. N-IR-01 strengthening

SS-014 / SS-016 no longer rely only on `EvaluationOutcome.commit_attempted == false` or `u04_decision is None`.

The Safety Suite now executes an AST-based structural boundary check over the isolated evaluator and fails if it exposes callable or imported commit/state-writer/U04/runtime side-effect surfaces.

This is additive implementation evidence only; independent source review remains authoritative for architectural isolation.

---

## 5. Expected implementation-verification state

After remediation self-test, the expected executable accounting is:

```text
approved Golden identities = 31
executable Golden cases = 30
excluded = GC-026

approved Safety identities = 20
executable critical Safety cases = 19
excluded = SS-012

shared-scope invariant check = PASS expected
D09 P5 defensive boundary check = PASS expected
```

These expected results are not yet governed evaluation evidence and do not close BF-CD06-EXEC-01.

---

## 6. Required next gate

```text
Targeted Independent Re-Review
→ verify BF-IR-01 closure
→ verify BF-IR-02 closure
→ verify no new semantic/runtime scope expansion
```

Only after targeted Independent Re-Review PASS may a separate governance action mark the implementation review complete and proceed to Governed Evaluation Execution.

```text
Independent Implementation Review = REVISE_REQUIRED historically preserved
Remediation = APPLIED_PENDING_TARGETED_RE_REVIEW
Governed Evaluation Execution = NOT_STARTED
Gate C = NOT_PASSED
Runtime / U04 / Production = BLOCKED
```
