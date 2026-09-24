# U06 RDP-06 Authoritative Verification Runner Completion v0.1

> Scope: **PROFILE-B / SYNTHETIC_STRUCTURAL_NONPROD**  
> Authorization: `AUTH-U06-PROFILEB-IMPL-001 = AUTHORIZED`  
> Implementation base: `f78bd9192d0603cfa3cc878644088f908dcb2fb8`  
> Runner completion semantic head: `131ae3599628316b5496a91ab0b67f9e39e623d8`  
> Independent runner review: **PASS / 5299059814**  
> Status: **RUNNER_COMPLETE / AUTHORITATIVE_PASS_CAPABILITY_READY**  
> This document does not declare U06 implementation verification PASS and does not authorize merge or production.

---

# 1. Completion verdict

~~~text
U06 RDP-06 Authoritative Verification Runner
= COMPLETE / PASS

AUTHORITATIVE_RDP06_PASS_CAPABILITY
= READY
~~~

This means the runner can now issue a fail-closed authoritative verdict over the full reviewed U06 verification authority.

It does **not** mean:

~~~text
U06 RDP-06 Authoritative Verification
= PASS
~~~

The current implementation under test still has verification failures.

---

# 2. Exact reviewed verification identity set

~~~text
U06-EV-001..106
= 106

U06-CW-01..09
= 9

U06-HG-001..003
= 3

U06-VG-001..010
= 10

U06-AGG-V-001..012
= 12

IRR01-V01..V16
= 16

IRR02-V01..V16
= 16

IRR03-V01..V17
= 17

TOTAL
= 189
~~~

No replacement identity namespace is permitted.

---

# 3. Evidence authority split

The completed runner keeps input/expectation/observation authority separate:

~~~text
reviewed Fixture
→ constructs synthetic scenario
→ SUT execution
→ actual observation

reviewed Oracle
→ expected claim

Verifier
→ independently compares observation vs Oracle
~~~

The runtime observation harness does not read the Oracle.

---

# 4. Runtime observation coverage

Runtime fixture classes:

~~~text
EXECUTION_CASE
CRASH_WINDOW
~~~

Exact runtime identities:

~~~text
106 U06-EV
+
9 U06-CW
=
115
~~~

Required:

~~~text
runtime_observation_count
= 115

runtime_observation_identity_count
= 115
~~~

Each runtime case becomes:
- PASS; or
- FAIL.

A thrown SUT exception becomes explicit observation/failure evidence rather than disappearing as unexecuted coverage.

---

# 5. Non-runtime verification coverage

The remaining 74 identities are handled as:

~~~text
49 IRR physical assertions
12 AGG-V aggregate compatibility assertions
3 HG verifier self-tests
10 VG verification gates
~~~

The verifier contains explicit handlers for every exact IRR/AGG/HG/VG identity.

---

# 6. Terminal-state invariant

For a valid authoritative run:

~~~text
all 189 exact case identities
→ PASS or FAIL
~~~

The runner no longer leaves ordinary negative execution results as:

~~~text
NOT_EXECUTED
~~~

`INCOMPLETE` is reserved for a genuine runner/evidence coverage defect.

---

# 7. External-call and environment isolation

The workflow runs the SUT observation/regression process under:

~~~text
Docker --network none
~~~

and records runtime socket-connect interception evidence.

Required:

~~~text
external_call_spy_enabled = true
external_call_spy_observed_count = 0

real_phi_count = 0
real_recipient_endpoint_count = 0
production_secret_count = 0
production_store_write_count = 0
real_external_delivery_count = 0
real_model_call_count = 0
real_tool_call_count = 0
real_knowledge_call_count = 0
unreviewed_shared_runtime_change_count = 0
~~~

---

# 8. Full regression / VG-010

The completed runner executes the full diagnosis-service Maven regression under the isolated environment.

The only pre-existing expected skip is narrowly enumerated:

~~~text
com.aidoctor.diagnosis.runtime.u03.U03Cd08FrozenClinicalValidationTest
~~~

Any other skipped test counts as unexpected.

VG-010:

~~~text
PASS only if:
full_regression_pass = true
regression_failures = 0
regression_errors = 0
unexpected_skips = 0
~~~

Otherwise:

~~~text
U06-VG-010 = FAIL
~~~

---

# 9. Fail-closed verdict semantics

~~~text
authority / digest invalid
→ INVALID_EVIDENCE

hard boundary violation
or any exact case FAIL
→ FAIL

genuine coverage/evidence gap
→ INCOMPLETE

all 189 exact cases PASS
+ isolation PASS
+ external-call spy PASS
+ full regression PASS
→ PASS_PENDING_INDEPENDENT_EVIDENCE_REVIEW
~~~

The runner never auto-grants:
- Implementation Verification Closure;
- Merge Authorization;
- Production Authorization.

---

# 10. Durable evidence bundle

The workflow produces:
- U06 case evidence;
- verification summary;
- environment evidence;
- fixture-driven runtime observation bundle;
- external-call spy evidence;
- SHA256SUMS;
- uploaded GitHub Actions artifact.

---

# 11. Execution proof before completion review

Run:

~~~text
35948052800
~~~

proved the authoritative path could execute end-to-end after the observation-harness compile blocker was fixed:

~~~text
engineering job
= PASS

runtime observation count
= 115

external_call_spy_enabled
= true

external_call_spy_observed_count
= 0

authoritative verifier
= executed
~~~

The result was:

~~~text
FAIL
~~~

rather than INCOMPLETE because the runner found actual SUT/regression failures.

At that run:
- one repository regression failed;
- U06 exact-case mismatches were surfaced explicitly;
- no external network boundary violation was observed.

---

# 12. Independent runner review

~~~text
review_id
= 5299059814

reviewed head
= 131ae3599628316b5496a91ab0b67f9e39e623d8

verdict
= PASS
~~~

Therefore:

~~~text
U06 RDP-06 Authoritative Verification Runner Completion
= PASS
~~~

---

# 13. Implementation-review applicability

Important:

~~~text
previous implementation semantic PASS head
= 1d427fd09957767c938e34883dcbcda85b7fd992
~~~

The implementation branch received additional U06 runtime/semantic changes while completing the authoritative runner.

Therefore:

~~~text
previous Implementation Review PASS
!= current-head Implementation Review PASS
~~~

Before final authoritative verification PASS can be accepted:

~~~text
current implementation
→ exact-head independent Implementation Re-Review
→ PASS
→ re-freeze implementation semantic authority
→ fresh authoritative verification
~~~

The contract manifest must not be silently repointed to an unreviewed implementation head.

---

# 14. Current verification state

~~~text
AUTHORITATIVE_RDP06_PASS_CAPABILITY
= READY

U06 RDP-06 Authoritative Verification
= FAIL / REMEDIATION_REQUIRED

U06 Implementation Verification Closure
= NOT_PASSED

Merge Authorization
= NOT_GRANTED
~~~

---

# 15. Next governance step

~~~text
U06 Authoritative Verification Failure Triage
+
Targeted Implementation Remediation
~~~

Then:

~~~text
Exact-Head Implementation Re-Review
→ implementation semantic re-freeze
→ fresh Authoritative RDP-06 Verification
→ Independent Evidence-Only Review
→ Combined Implementation / Evidence Review
→ Implementation Verification Closure
~~~

No merge, production, PROFILE-A, live routing, real dependency, external delivery, or real-patient authorization is granted.
