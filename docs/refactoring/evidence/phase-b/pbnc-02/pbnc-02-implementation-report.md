# PBNC-02 Synthetic Versioned State Integration Implementation Report

> Classification: `CONTROLLED NON-CLINICAL IMPLEMENTATION` / `SYNTHETIC ONLY` / `IN-MEMORY` / `NO PRODUCTION WIRING`
>
> Batch: `PBNC-02` / `SYNTHETIC_VERSIONED_STATE_INTEGRATION`
>
> Status: `IMPLEMENTED_PENDING_INDEPENDENT_RE_REVIEW`
>
> Authorization token:
> `PBNC_02_SYNTHETIC_VERSIONED_STATE_INTEGRATION_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Authorized Enterprise base:
> `126b2dec3445022ea155ec7933095bb1bd2ce186` /
> `00771d0f4731f938149320813206fb027e87779d`

```text
PBNC-02 IMPLEMENTED
!=
B1 COMPLETE
!=
B2 COMPLETE
!=
PHASE B COMPLETE
!=
PRODUCTION READY
```

## 1. Scope

PBNC-02 implements a dormant, non-Spring, in-memory synthetic versioned
repository behind the existing State Committer repository boundary.

It proves synthetic state application for:

- `ADD`
- `REPLACE`
- `REMOVE`

It also proves:

- internal synthetic snapshot semantics
- optimistic version conflict
- exactly-once version advancement
- no partial authoritative mutation
- rollback by discarding an uncommitted working copy
- deterministic same-base concurrent winner/conflict behavior
- integration with PBNC-01 idempotency coordination
- caller snapshot isolation

It does not implement or verify:

- `TEST` operation application
- full `expected_current_value` semantics
- top-level null value application for `ADD` / `REPLACE`
- full JSON Schema runtime validation
- durable persistence
- distributed concurrency
- production wiring
- clinical state

## 2. Internal Port Adjustment

`StateRepositoryPort.AtomicCommitCommand` now carries the existing
`StateTypes.StatePatch` object in addition to:

- `cdpId`
- `expectedCurrentVersion`
- `patchId`
- `idempotencyKey`

The synthetic repository verifies that command metadata matches the attached
patch before mutation. A mismatch returns a deterministic nonretryable failure
and performs no mutation.

`AtomicCommitOutcome` now carries internal retryability. This maps repository
infrastructure failure to `FAILED / retryable=true` and deterministic synthetic
operation application failure to `FAILED / retryable=false`, without changing
Shared Contracts v1.

```text
contracts/v1/** semantic diff = 0
```

## 3. Internal Synthetic Application Semantics

```text
PBNC_02_INTERNAL_SYNTHETIC_APPLICATION_SEMANTICS
!=
SHARED_CONTRACTS_CLINICAL_APPLICATION_SEMANTICS

SYNTHETIC_STATE
!=
CLINICAL_STATE
```

PBNC-02 uses JSON-pointer-compatible path parsing, including deterministic
decoding of `~0` and `~1`.

Bounded semantics:

- parent traversal is object/map-only;
- parent path must already exist;
- arbitrary array-index path mutation is unsupported;
- flat arrays may be leaf values;
- no domain-specific shape is inferred;
- `CDPFieldWriter` is not reused.

### ADD

Parent object must exist. Target leaf must not exist. Authorized non-null
synthetic value is added to the working copy.

### REPLACE

Parent object and target leaf must exist. Authorized non-null synthetic value
replaces the leaf on the working copy.

### REMOVE

Parent object and target leaf must exist. Target is removed on the working copy.

### TEST

`TEST` remains rejected by `StateCommitter` before repository mutation.

## 4. Unsupported Semantics

`expected_current_value`:

- non-null value is rejected before repository mutation;
- explicit null remains indistinguishable from absence in the current Java POJO;
- value-level CAS semantics are not implemented.

Top-level null `value` for `ADD` / `REPLACE`:

- rejected before repository mutation;
- not counted as verified null-value application.

Evidence ceiling:

```text
EXPECTED_CURRENT_VALUE_APPLICATION_VERIFIED = NO
FULL_EXPECTED_CURRENT_VALUE_SEMANTICS_VERIFIED = NO
NULL_VALUE_APPLICATION_VERIFIED = NO
TEST_OPERATION_APPLICATION_VERIFIED = NO
FULL_JSON_SCHEMA_RUNTIME_VALIDATION = NO
```

CR-01 remains accepted synthetic-only debt.

## 5. Synthetic Repository

Class:

`diagnosis-service/src/main/java/com/aidoctor/diagnosis/state/committer/SyntheticVersionedStateRepository.java`

Properties:

- final dormant Java class
- no Spring stereotype
- no bean registration
- no HTTP
- no Feign
- no JPA
- no database
- no Redis
- no Neo4j
- no filesystem persistence
- no provider/network calls
- no `CDPManager`
- no `CDPFieldWriter`
- no clinical entity classes

State representation:

- `Map<String,Object>`
- `List<Object>`
- `String`
- `Number`
- `Boolean`
- internal `null` only for deep-copy support and leaf array support

Initial state is constructor supplied and synthetic only.

## 6. Snapshot

Internal type:

`SyntheticStateSnapshot`

It carries:

- version
- deep-copied JSON-like synthetic state

Returned caller state is a deep copy. Mutating a returned snapshot does not
mutate repository state.

`SyntheticStateSnapshot` is not:

- Shared Contract
- Clinical State
- CDP
- Encounter
- checkpoint
- production DTO

## 7. Atomicity

Authoritative mutation model:

1. enter synchronized commit section;
2. compare current version to expected version;
3. deep-copy authoritative state;
4. apply every operation to the working copy;
5. on any operation failure, discard working copy;
6. construct committed outcome with representable next version;
7. atomically swap state + version once;
8. return `COMMITTED`.

No operation-by-operation mutation of authoritative state occurs.

At `Integer.MAX_VALUE`, version overflow is detected before state mutation.

## 8. Idempotency

PBNC-01 `StateCommitter` and `IdempotencyPort` remain the only idempotency
authority.

The synthetic repository does not introduce a second deduplication store. It
receives idempotency metadata only for command identity checks.

Verified behavior:

- same key + same fingerprint replays without second state mutation;
- idempotency completion failure after commit preserves `COMMITTED`;
- retry after incomplete completion cannot duplicate mutation.

## 9. Tests

Focused implementation validation:

```text
mvn -f diagnosis-service/pom.xml -Dtest='StateCommitter*,SyntheticVersionedStateRepositoryTest' test

StateCommitterArchitectureGuardTest: 5
StateCommitterContractBoundaryTest: 5
StateCommitterMechanicalCoreTest: 39
SyntheticVersionedStateRepositoryTest: 30

Total: 79
Failures: 0
Errors: 0
Skipped: 0
```

Repository validation:

```text
python contracts/v1/validator/validate_contracts.py
A5 CONTRACT VALIDATION PASSED: 13 schemas, 13 valid fixtures, 33 invalid fixtures

python contracts/v1/bindings/tooling/check_drift.py
drift check passed for 1.0.0 schemas 13

python -m pytest -p no:cacheprovider contracts/v1/tests -q
110 passed

mvn -f contracts/v1/bindings/java/pom.xml test
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0

mvn -f diagnosis-service/pom.xml test
Tests run: 134, Failures: 0, Errors: 0, Skipped: 0
```

Positive PBNC-02 proofs:

```text
INITIAL_VERSION_DETERMINISTIC = YES
INITIAL_SNAPSHOT_DETERMINISTIC = YES
SYNTHETIC_ADD_APPLICATION_VERIFIED = YES
SYNTHETIC_REPLACE_APPLICATION_VERIFIED = YES
SYNTHETIC_REMOVE_APPLICATION_VERIFIED = YES
STATE_VERSION_SINGLE_INCREMENT_VERIFIED = YES
STALE_BASE_CONFLICT_VERIFIED = YES
CONCURRENT_SAME_BASE_WINNER_CONFLICT_VERIFIED = YES
IDEMPOTENCY_NO_SECOND_STATE_MUTATION_VERIFIED = YES
MULTI_OPERATION_STATE_ATOMICITY_VERIFIED = YES
FAILED_OPERATION_ORIGINAL_STATE_UNCHANGED_VERIFIED = YES
REPOSITORY_FAILURE_ORIGINAL_STATE_UNCHANGED_VERIFIED = YES
INTEGER_MAX_NO_MUTATION_VERIFIED = YES
SNAPSHOT_ISOLATION_VERIFIED = YES
NO_DIRECT_WRITER_ROUTE_VERIFIED = YES
RESTART_PERSISTENCE_VERIFIED = NO
```

## 10. Architecture Negatives

Architecture guard and source inspection preserve:

```text
Clinical Runtime = 0
Provider = 0
PHI = 0
Real patient = 0
Clinical fixture content = 0
Safety = 0
Production DB = 0
JPA = 0
Spring wiring = 0
HTTP = 0
Feign = 0
Redis = 0
Neo4j state write = 0
Filesystem persistence = 0
Legacy CDP integration = 0
Dual write = 0
Shared Contracts semantic diff = 0
Production StateCommitter caller = 0
Synthetic repository production caller = 0
```

## 11. Evidence Ceiling

```text
DOCUMENTED = YES
CODE_CONFIRMED = YES
TEST_VERIFIED = YES

PBNC_02_DURABLY_CLOSED = NO
B1_COMPLETE = NO
B2_COMPLETE = NO
PHASE_B_COMPLETE = NO
CLINICAL_VALIDATED = NO
PRODUCTION_VERIFIED = NO
STATE_COMMITTER_PRODUCTION_WIRED = NO
DURABLE_PERSISTENCE_VERIFIED = NO
DISTRIBUTED_CONCURRENCY_VERIFIED = NO
```

Fresh PR CI identity is recorded after Draft PR creation.

## 12. Bounded Correction PB2-01 / PB2-02

Authorization token:

`PBNC_02_PR74_BOUNDED_CORRECTION_EXPLICIT_AUTHORIZATION_GRANTED`

Previous independently reviewed Head / Tree:

```text
e2993b49ee88b97e21556d9cacea604d900c7b17
61dd6cda8df76d847da738168f8670b53beae8ff
```

PB2-01 finding:

```text
public main-code failure-injection API
Previous verdict = CONFIRMED_PROBLEM
Previous severity = MAJOR
```

PB2-01 correction:

- removed main-code `failNextCommit` state
- removed main-code `throwNextCommit` state
- removed public `failNextCommit()` method
- removed public `throwNextCommit()` method
- removed test-only toggle branches from `attemptAtomicCommit(...)`
- moved backend failure/exception simulation into a private test-local
  `StateRepositoryPort` wrapper inside `SyntheticVersionedStateRepositoryTest`

PB2-02 finding:

```text
negative seeded snapshot version accepted
Previous verdict = CONFIRMED_PROBLEM
Previous severity = MAJOR
```

PB2-02 correction:

- `SyntheticStateSnapshot` is the version invariant owner
- `version < 0` is rejected at construction
- `0` remains accepted
- `Integer.MAX_VALUE` remains accepted as a snapshot state
- commit overflow from `Integer.MAX_VALUE` remains failed before mutation

Preserved minor debts:

```text
PB2-03 trusted backend does not independently validate complete StatePatch
PB2-04 direct repository REMOVE with non-null value
PB2-05 direct repository arbitrary/unauthorized seeded roots

NOT_FIXED_BY_THIS_BATCH
```

Focused correction validation:

```text
mvn -f diagnosis-service/pom.xml -Dtest='StateCommitter*,SyntheticVersionedStateRepositoryTest' test

StateCommitterArchitectureGuardTest: 6
StateCommitterContractBoundaryTest: 5
StateCommitterMechanicalCoreTest: 39
SyntheticVersionedStateRepositoryTest: 34

Total: 84
Failures: 0
Errors: 0
Skipped: 0
```

Full local correction validation:

```text
mvn -f contracts/v1/bindings/java/pom.xml install -DskipTests
mvn -f diagnosis-service/pom.xml test

Tests run: 139
Failures: 0
Errors: 0
Skipped: 0
```

Shared Contracts regression:

```text
python contracts/v1/validator/validate_contracts.py
A5 CONTRACT VALIDATION PASSED: 13 schemas, 13 valid fixtures, 33 invalid fixtures

python contracts/v1/bindings/tooling/check_drift.py
drift check passed for 1.0.0 schemas 13

python -m pytest -p no:cacheprovider contracts/v1/tests -q
110 passed

mvn -f contracts/v1/bindings/java/pom.xml test
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

Fresh post-correction PR CI identity is recorded after the correction commit and
Draft PR CI complete.

## 13. Control State

```text
Engineering Baseline = FROZEN / V1
A7-NC = COMPLETE / CLOSED / STABLE
A7 = NOT_COMPLETE
A11 = NOT_PASSED
Clinical Runtime = NOT_ENABLED
Production = BLOCKED
PBNC-01 = COMPLETE / DURABLY_CLOSED
PBNC-02 = BOUNDED_CORRECTION_IMPLEMENTED_PENDING_INDEPENDENT_RE_REVIEW
B2 = NOT_COMPLETE
Full Phase B = NOT_AUTHORIZED
```
