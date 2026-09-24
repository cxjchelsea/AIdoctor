# U06 Accepted Compact Evidence Snapshot v0.1

> Repository-retained compact snapshot created by Independent Evidence Acceptance.  
> Acceptance review: **U06 Independent Evidence-Only Re-Review v0.3 = PASS**  
> Scope: **PROFILE-B / SYNTHETIC_STRUCTURAL_NONPROD only**

## Accepted identity

~~~text
implementation_semantic_sha
= 66fa3c078dce5e304e2ada9d6c0df0ff9fbde661

implementation_evidence_head
= 8c2d4d515ed5e9c5aecbc9a93c02af81f03da613

verifier_sha
= d693abd10f6defef1d7d012ea806dcbd4dd0e442

authority_core_digest
= 038c07422218f6ab12d312a5b27560e069c32bf1c3d87067ace9a3a95b0a2d22

contract_manifest_blob
= fec6755eab3709472b571f9317c75c8b50b6efd4

oracle_sha256
= 9a7ae0b5286bd033e74e28167af523a53d57fccd7694d16e7c76f3da281f234f

fixture_sha256
= df87c9fb7e30fc472d96f840f25fafac987d8a256cbcda2c872e89e2e8a63cd0

auth_profile_sha256
= b1aaea818525f6e6c81089d18a8d2ac38d35478a666173de27903c70e4ab77b4
~~~

## Accepted execution evidence

~~~text
workflow_run_id
= 35964444790

authoritative_job_id
= 107520018444

primary_artifact_id
= 10793348986

primary_artifact_name
= u06-rdp06-authoritative-d693abd10f6defef1d7d012ea806dcbd4dd0e442

primary_artifact_sha256
= 0f8fe12ffbb88d2b261c1989a842d4219fa367716c9f01e89eb16b7f7c4a9103

receipt_artifact_id
= 10793358880

receipt_artifact_sha256
= 797080f4530dc06e734f6d9734be82f51b171ed340395988d2eaf262f773ef6b

retention
= 90 days

expires_at
= 2026-12-23
~~~

## Accepted verification result

~~~text
PASS
= 189

FAIL
= 0

NOT_EXECUTED
= 0

runtime_observations
= 115

integrity_checks
= 53 / 53 PASS

full_regression
= PASS

regression_tests
= 434

unexpected_skips
= 0

external_call_spy
= enabled

external_call_spy_observed_count
= 0

hard_boundary_violations
= []

missing_case_ids
= []

failed_case_ids
= []
~~~

## Independent authority review lineage

~~~text
Oracle / Fixture provenance review
= 0a6cbc7d7c6f587d41da64fad969e421fa574e78
= PASS

Auth Profile / Contract Manifest equivalence review
= fc34e793feef838fa4fbb2d3e61b542716979045
= PASS

Auth Profile review ID
= U06_AUTH_PROFILE_EQUIVALENCE_REVIEW_20260924_01
~~~

## Durable integrity

~~~text
primary outer SHA
= PASS

receipt outer SHA
= PASS

primary SHA256SUMS
= 21 / 21 OK

artifact manifest
= 20 / 20 exact

contract authority blobs
= 17 / 17 exact

key retained source snapshots
= 10 / 10 exact Git blobs
~~~

## Acceptance

~~~text
Independent Evidence Acceptance
= PASS

Combined Implementation / Evidence Review
= PERMITTED NEXT

Implementation Verification Closure
= NOT_YET_DECIDED

Merge Authorization
= NOT_GRANTED
~~~

This snapshot is compact repository-retained evidence metadata. It does not replace the 90-day raw CI artifacts and does not authorize production/live use or merge.
