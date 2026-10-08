# U07 Initial Readiness Targeted Independent Re-Review v0.1

> Date: 2026-10-08
> Original independent review: PR #264, `IR-U07-01` + `IR-U07-02` = REVISE_REQUIRED
> Remediation under review: PR #263
> Exact reviewed head: `a3b4215a49855910ce15cba73bb0e60db780db39`
> Original review baseline: `1722d7bcbece9909a64e5b1fac544ca433c41ca8`
> Upstream Unit Spec review basis: PR #262 @ `9fb8726421ffdb4ed8784560150afcb716f0b4f9`
> Main integration base: `6d4fd787600e3a57f01f3e17893e6d98893ac546`
> Scope: **targeted, documentation-only independent re-review**.
> Verdict: **PASS / NO_BLOCKER** for the two remediation findings; implementation remains NOT_READY/NOT_GRANTED.

## 1. Exact-head evidence and method

GitHub commit comparison of `1722d7bc...a3b4215a` shows `ahead_by=1`, `behind_by=0`, and exactly one changed file:

`docs/current/06_开发单元/U07_Initial_Implementation_Readiness_Gap_Review_v0.1.md`

File stats: **63 additions, 12 deletions**. No Runtime source/test change in the targeted delta. The remediated file was independently fetched at the exact reviewed SHA, rather than accepting the author's remediation labels as review results.

Scope of targeted review: whether each original independent finding is fully resolved; whether the change adds new authorization leakage, redefines the accepted U07 semantics, or hides unmet prerequisites. This is **not** a second exhaustive repository-wide audit of P01/P02/P05/P06 nor a U07 implementation review.

## 2. Finding IR-U07-01 — RDP-06 design vs post-implementation evidence

**Original blocker:** design closure incorrectly required tests passing on an implementation SHA before implementation authorization, yielding an impossible prerequisite loop.

**Observed remediation:**
- RDP-06 now defines a `Design target (not evidence of an executed test)`.
- `Design/readiness closure (RDP-06)` explicitly requires approved oracle/fixture schemas, scenario and negative-assertion matrices, runner/manifest **design** and an implementation plan, plus independent design PASS.
- `Later post-authorization evidence` separately requires implementing/running the verifier, generating durable evidence, and independent evidence-only review.
- The RDP classification matrix repeats this separation, and the governance decision still says implementation authorization `NOT_GRANTED`.

**Re-review decision:**

```text
IR-U07-01 = PASS / CLOSED
Design verification obligations = STILL REQUIRED IN RDP-06
Executed implementation evidence = DEFERRED_TO_POST_IMPLEMENTATION, NOT WAIVED
```

## 3. Finding IR-U07-02 — six-blocker taxonomy and runtime evidence

**Original blocker:** all six domains were labelled P0 despite covering distinct missing design choices, unassessed platform capability, and later implementation/verification duties.

**Observed remediation:**
- Replaces blanket P0 with six `OPEN readiness workstreams / implementation prerequisites`.
- Adds explicit taxonomies for `Kind`, `Evidence strength`, `Runtime status` and independent `Design closure`.
- Adds a six-row matrix `B-U07-RG-01..06` with separate contract/design work, observed/reusable or unassessed Runtime assets, the exact intended reviewed RDP closure artifact, and later implementation obligations.
- Reuses positively observed Foundation/U06 assets; does not assert `CONFIRMED_MISSING` without exhaustive negative evidence.
- Each RDP section now distinguishes its reviewed design target from later implementation test evidence.

**Re-review decision:**

```text
IR-U07-02 = PASS / CLOSED
Runtime capability inventory completeness = NOT_ASSESSED; MUST BE RESOLVED BY RELEVANT RDP / AGGREGATE REVIEW
U07 RDP-01..06 = OPEN / REQUIRED
```

No observed reclassification implies that `NOT_ASSESSED` means a verified absence.

## 4. Guardrail regression check

| Boundary | Re-review result |
|---|---|
| Business Resume validity != P02 Runtime compatibility | PRESERVED |
| ACCEPTED answer != Clinical Fact | PRESERVED |
| U02 retains Clinical Fact interpretation | PRESERVED |
| F3 owns Question / Gap lifecycle | PRESERVED |
| U07 doesn't directly write Clinical Truth; K09/P01 governance remains mandatory | PRESERVED |
| Missing/stale checkpoint alone doesn't fabricate Business REJECTED | PRESERVED |
| No direct U05/U08 continuation | PRESERVED |
| Design approval != Implementation Authorization | PRESERVED |
| PROFILE-A/production/live/real-patient | STILL BLOCKED |

The change is solely the readiness review document's classification and gate logic. This confirms preservation **within the targeted delta**, not a claim that a live implementation passed testing.

## 5. Minor observations — NON_BLOCKING

1. Some RDP sections retain the old `Design target` description next to the new closure paragraph; this is redundant but does not change authority.
2. `RDP-05` must later provide concrete code-and-test evidence for each P01/P02/P05/P06 dependency; the `NOT_ASSESSED` value here is deliberately provisional.
3. An exact-head design re-review does not supersede the independent review record in PR #264 or authorize merging any stacked PR.

## 6. Formal decision

```text
U07 Initial Readiness Targeted Independent Re-Review = PASS / NO_BLOCKER
Reviewed head = a3b4215a49855910ce15cba73bb0e60db780db39
IR-U07-01 = CLOSED
IR-U07-02 = CLOSED
Additional blocking finding = NONE
U07 Initial Readiness Gap Review = ACCEPTABLE AS DESIGN WORKLIST / NOT_READY
U07-RDP-01..06 = OPEN / REQUIRED
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
U07 Runtime implementation = NOT_STARTED AS GOVERNED UNIT
Production / PROFILE-A / live resume / PHI / real-patient = BLOCKED
```

Next permitted **design** task: `U07-RDP-01 Consumer Inbound / Event Admission Contract`, followed by RDP-02..06 and aggregate compatibility/readiness gates. No code implementation, merge, production or live-patient authorization is granted.
