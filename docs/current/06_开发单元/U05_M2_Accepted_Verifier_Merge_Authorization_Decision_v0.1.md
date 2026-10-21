# U05 M2 Accepted-Verifier Merge Authorization Decision v0.1

> Authorization ID: `AUTH-U05-M2-ACCEPTED-VERIFIER-MERGE-001`  
> Merge candidate: PR #223  
> M1 Post-Merge Verification: **PASS**  
> Independent M2 Merge Authorization Review: `5286283480 = PASS`  
> Exact source: `c334ea8ec6b75020c3bba12d02f48140a28b8318`  
> Exact target: `main@39e658eb6d86c2c0dfc5d1199837553b7faa2934`  
> Test merge: `fe660ff0e886c1206a1811ccdf087dffa73f6ea2`  
> Required method: **STANDARD MERGE COMMIT ONLY**  
> Status: **AUTHORIZED / CONSUMED_FOR_M2_STANDARD_MERGE**

## 1. Decision question

Whether to authorize repository integration of the exact accepted U05 RDP-06 verifier overlay into the exact post-M1 main target.

This is verifier/tooling repository integration only.

It does not authorize production or live clinical behavior.

## 2. Exact source / target

```
source
= c334ea8ec6b75020c3bba12d02f48140a28b8318

target
= main@39e658eb6d86c2c0dfc5d1199837553b7faa2934
```

No source or target drift is permitted.

## 3. Exact test-merge proof

GitHub test merge:

```
fe660ff0e886c1206a1811ccdf087dffa73f6ea2
```

Parents:

```
parent1 = 39e658eb6d86c2c0dfc5d1199837553b7faa2934
parent2 = c334ea8ec6b75020c3bba12d02f48140a28b8318
```

Test-merge result relative to target changes exactly 15 verifier-only paths.

No production/runtime path changes.

All 15 resulting file blobs are byte-identical to the accepted verifier source.

## 4. Accepted verifier identities

```
Static Review
= 5286008459 / PASS

Authoritative RDP-06 Run
= 35808170430 / PASS

Independent Evidence-Only Review
= 5286106435 / PASS

Combined Implementation/Evidence Review
= 5286165180 / PASS
```

Frozen static digests:

```
expectation
= 6aaebaaae788331788ec1b1c1bd14ca255212b3472f0c43ace0df82b52687f3a

precedence
= 2ba0411cdbe0b3dc194e81472e34f66ceb1c7b40343a2b0b22ee5bf20e8fc38b

fixtures
= 2e553e2c17e7109124f97467be7c1e81ea59e59603d345dcbd56699db04b5bed

authority core
= 570ca5303f601df2caf672bfb7c9e61fe2c5a5add86e288cf782c8e6030712e5
```

## 5. Authorized-if-approved action

If owner selects AUTHORIZE:

```
AUTH-U05-M2-ACCEPTED-VERIFIER-MERGE-001
= AUTHORIZED
```

Only permitted action:

```
merge PR #223

source
= c334ea8ec6b75020c3bba12d02f48140a28b8318

target
= main@39e658eb6d86c2c0dfc5d1199837553b7faa2934

method
= STANDARD MERGE COMMIT ONLY
```

Expected merge commit must have the exact target/source as parent1/parent2.

## 6. Mandatory post-merge verification

After M2:

1. verify actual main HEAD;
2. verify exact parent1/parent2;
3. compare pre-M2 main -> actual merge result;
4. require exactly 15 changed paths;
5. require no production/runtime path change;
6. require all 15 verifier blobs equal accepted verifier source;
7. require static digest-bearing files remain byte-identical;
8. confirm production/live boundaries unchanged.

Only after M2 Post-Merge Verification passes may M3 begin.

## 7. No new authoritative rerun required if byte-identical

A new RDP-06 authoritative run is not required solely because the accepted verifier bytes are repository-integrated, provided:

```
implementation tree unchanged
AND
all 15 verifier blobs byte-identical
AND
static digests unchanged
AND
no verifier semantic drift
```

Any mismatch invalidates this equivalence and requires STOP/review.

## 8. Still prohibited

```
M3 closure merge = NOT_AUTHORIZED

Production Authorization = BLOCKED
Production Clinical Runtime = NOT_ENABLED
Live upstream cutover = NOT_AUTHORIZED
Live downstream execution = NOT_AUTHORIZED
External delivery = NOT_AUTHORIZED
Release activation = NOT_AUTHORIZED
Real-patient traffic = NOT_AUTHORIZED
```

## 9. Owner options

```
AUTHORIZE
REVISE
REJECT
```

Current:

```
AUTH-U05-M2-ACCEPTED-VERIFIER-MERGE-001
= AUTHORIZED
```


## 10. Repository Owner Decision

The repository owner explicitly selected:

```
AUTHORIZE
```

against exact reviewed authorization-package head:

```
51059060cb64c9ff9b6615d3f28dd5590e09a5d9
```

Therefore:

```
AUTH-U05-M2-ACCEPTED-VERIFIER-MERGE-001
= AUTHORIZED
```

Authorized exact merge:

```
PR = #223
source = c334ea8ec6b75020c3bba12d02f48140a28b8318
target = main@39e658eb6d86c2c0dfc5d1199837553b7faa2934
method = STANDARD MERGE COMMIT ONLY
```

Pre-consumption drift check immediately before authorization consumption:

```
PR #223 head = c334ea8ec6b75020c3bba12d02f48140a28b8318
main = 39e658eb6d86c2c0dfc5d1199837553b7faa2934
mergeable = true
source drift = none
target drift = none
```

Mandatory next action after merge:

```
M2 Post-Merge Verification
```

M3 remains not authorized until M2 post-merge verification passes.
