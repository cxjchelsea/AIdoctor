# U04 RDP-06 Verification and Durable Evidence Plan v0.1

> Scope: future non-production verification requirements only.

## 1. Required verification families

V1 inbound admission: exact state/version/release/provenance identity.
V2 decision uniqueness: one accepted input -> one gate result.
V3 safety invariants: prohibited ALLOW cases fail.
V4 controlled state mutation: proposal/commit ownership and stale invalidation.
V5 routing: blocked/unavailable cannot enter ordinary path; no U04/U14 double-route.
V6 replay/idempotency: no duplicate commit/routing.
V7 unauthorized owner/routing attempts fail closed.
V8 regression against U03 producer contract and existing U03 no-U04-execution invariants.

## 2. Minimum negative cases

Must include at least:
- stale U03 handoff;
- wrong/mixed/mutable release refs;
- malformed/missing provenance;
- U03 FAILED;
- HIGH_RISK;
- required Safety Capability unavailable if RDP-05 later declares one;
- conflicting safety inputs;
- direct mutation attempt;
- duplicate replay;
- frontend/model/agent override attempt;
- BLOCKED/UNAVAILABLE ordinary-route attempt;
- unauthorized U14 double-route attempt.

## 3. Durable evidence

Future verification must freeze:
- exact implementation SHA;
- exact policy/dependency package refs;
- test case/scenario IDs;
- source Clinical State Version;
- admitted input identity;
- observed U04 gate result or typed failure;
- proposal/commit identity if reached;
- routing effect if reached;
- trace/audit identity;
- expected vs observed boundary/result;
- PASS/FAIL;
- workflow run/artifact/checksums.

## 4. Clinical truth boundary

Verification may compare only against frozen governed U04 policy/dependency semantics.
If an expected Safety Gate result is not frozen, classify `SAFETY_POLICY_EXPECTATION_GAP` and stop; do not invent the answer in test code.

## 5. Acceptance gate

Implementation cannot be declared verified unless all blocking safety scenarios pass, full regression passes, durable artifact is retained, and independent implementation/evidence review passes.

## 6. Verdict

U04-RDP-06 = FROZEN / PASS_FOR_READINESS
U04 implementation authorization = NOT_GRANTED
