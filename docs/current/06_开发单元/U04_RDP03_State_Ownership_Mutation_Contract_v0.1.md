# U04 RDP-03 Safety Gate State Ownership and Mutation Contract v0.1

> Scope: ownership, versioning, mutation and invalidation of U04 Safety Gate state.

## 1. Owner

U04 is the sole business owner of Safety Gate interpretation for the current U04 unit.
Other modules may provide inputs but may not maintain a competing Safety Gate truth.

## 2. State vocabulary

Canonical Safety Gate business values:
`ALLOW`, `RESTRICTED`, `BLOCKED`, `UNAVAILABLE`.

These values are business state, not model labels and not raw capability outputs.

## 3. Controlled mutation

Required sequence:
`admitted U04 input -> U04 owner decision -> typed proposal -> G2/StateCommitter validation -> committed Safety Gate state`.

Forbidden:
- direct model/frontend/agent mutation;
- direct capability-to-canonical-state write;
- uncommitted proposal represented as committed Safety Gate;
- mutation without Clinical State Version binding.

## 4. Version binding and invalidation

Every committed Safety Gate must bind the Clinical State Version and exact U03 source result it was derived from.

A new Clinical State Version, invalidated U03 risk result, changed required dependency state, or superseding governed policy must make the previous U04 gate stale/invalidated until re-evaluated.

## 5. Idempotency

Same business event + same source Clinical State Version may produce at most one canonical U04 mutation.
Replay must not create duplicate commits or duplicate routing effects.

## 6. Failure boundary

Proposal/commit failure is not a valid ALLOW/RESTRICTED/BLOCKED/UNAVAILABLE result.
Canonical mutation failure must fail closed and remain distinguishable from Safety Gate business output.

## 7. Verdict

U04-RDP-03 = FROZEN / PASS_FOR_READINESS
U04 implementation authorization = NOT_GRANTED
