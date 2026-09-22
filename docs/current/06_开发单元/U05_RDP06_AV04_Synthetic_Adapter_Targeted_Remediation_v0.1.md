# U05 RDP-06 AV-04 Synthetic Adapter Targeted Remediation v0.1

> Finding: `BF-U05-RDP06-AV-04`  
> Source authoritative run: `35709046097` / job `106685417372`  
> Failing exact target: `261ee5525c8260e93db19173ffbde89a8af6810d`  
> Existing implementation authorization: `AUTH-U05-RUNTIME-IMPL-001 = AUTHORIZED`  
> Status: **PROPOSED / INDEPENDENT_REVIEW_PENDING**

## 1. Finding

Authoritative G2B baseline regression fails before verifier overlay is applied:

```
StateCommitterArchitectureGuardTest
.productionSourcesDoNotDirectlyConstructSyntheticStateRepository
```

Violation:

```
diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u05/
U05SyntheticClinicalReadinessSnapshotAdapter.java
```

The concrete adapter directly depends on:

```
SyntheticVersionedStateRepository
SyntheticStateSnapshot
```

which are synthetic/non-production state facilities. The architecture guard correctly forbids that dependency from a production source tree.

## 2. Root cause

The U05 production abstraction is correctly separated:

```
U05CommitService
-> U05ClinicalReadinessSnapshotPort
```

The defect is only placement of the synthetic implementation:

```
src/main
  U05SyntheticClinicalReadinessSnapshotAdapter   <-- wrong source set
```

The synthetic implementation exists solely to prove exact structured readback in non-production engineering/tests.

## 3. Minimal remediation

Keep unchanged in `src/main`:

```
U05ClinicalReadinessSnapshotPort
U05ClinicalReadinessSnapshot
U05CommitService
```

Move unchanged concrete synthetic adapter implementation from:

```
diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/u05/
U05SyntheticClinicalReadinessSnapshotAdapter.java
```

to:

```
diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/u05/
U05SyntheticClinicalReadinessSnapshotAdapter.java
```

Package remains:

```
com.aidoctor.diagnosis.runtime.u05
```

so existing U05 non-production tests/verifier code may continue to instantiate the adapter without production-source coupling.

## 4. Prohibited alternatives

Do not:
- weaken or exclude `StateCommitterArchitectureGuardTest`;
- whitelist U05 in the architecture guard;
- move SyntheticVersionedStateRepository into a production-facing abstraction;
- add Spring/default wiring;
- add a production Clinical State adapter under this remediation;
- change readback semantics;
- change RDP-03 state mutation semantics.

## 5. Expected verification

After relocation:

```
mvn -f diagnosis-service/pom.xml test
```

must show:
- `StateCommitterArchitectureGuardTest` PASS;
- prior known U03 authorization-gated skip may remain if unchanged;
- U05 focused tests remain PASS;
- no new failures/errors/skips beyond the governed baseline.

Focused checks:
- U05 readback verification remains exact-version/exact-payload;
- no `SyntheticVersionedStateRepository` reference exists outside the StateCommitter package in `src/main/java/com/aidoctor/diagnosis/**`;
- no production source behavior changes.

## 6. Authorization impact

This remediation changes only U05 implementation/test source placement and remains within the existing authorized non-production U05 implementation scope.

```
New shared-runtime authorization = NOT_REQUIRED
New clinical-content authorization = NOT_REQUIRED
Frozen RDP amendment = NOT_REQUIRED
```

Because implementation SHA changes:

```
current RDP-06 target rebind = INVALIDATED_BY_TARGET_DRIFT
current static-review head binding = HISTORICAL
```

Required after implementation:
1. full baseline regression;
2. exact-head implementation re-review;
3. verifier exact-target rebind;
4. targeted static re-review/digest applicability check;
5. authoritative RDP-06 rerun.

## 7. Proposed verdict

```
BF-U05-RDP06-AV-04 remediation design
= PASS_FOR_IMPLEMENTATION_UNDER_EXISTING_AUTHORIZATION

Shared production runtime semantic change
= NONE
```
