# U06 Evidence Authority + Durable Bundle Targeted Remediation v0.1

> Trigger: U06 Independent Evidence-Only Review PR #246  
> Evidence review verdict: REVISE_REQUIRED  
> Implementation semantic SHA remains frozen at: `66fa3c078dce5e304e2ada9d6c0df0ff9fbde661`  
> Scope: evidence authority / verifier / workflow packaging only

## 1. Findings addressed

~~~text
B-U06-EVR-01 = TARGETED_REMEDIATION
B-U06-EVR-02 = TARGETED_REMEDIATION
B-U06-EVR-03 = TARGETED_REMEDIATION
RF-U06-EVR-01 = TARGETED_REMEDIATION
RF-U06-EVR-02 = TARGETED_REMEDIATION
~~~

## 2. Oracle / Fixture immutable provenance repair

Historical gate files are restored to the exact pre-re-freeze review `5298650596` bindings.

New independent provenance-only review:

~~~text
review PR = #247
review record commit = 0a6cbc7d7c6f587d41da64fad969e421fa574e78

Oracle review ID
= U06_ORACLE_REFREEZE_PROVENANCE_REVIEW_20260924_01

Fixture review ID
= U06_FIXTURE_REFREEZE_PROVENANCE_REVIEW_20260924_01
~~~

The verifier consumes only the new refrozen gate files for the refrozen asset digests.

## 3. Durable evidence package

The verifier now copies exact source bytes into the evidence directory for:

- contract manifest;
- verification Oracle;
- Fixture manifest;
- auth profile;
- new refrozen Oracle gate;
- new refrozen Fixture gate;
- historical Oracle gate;
- historical Fixture gate;
- observed authorized shared-runtime change manifest;
- authoritative workflow snapshot;
- verifier source snapshot;
- runtime observations;
- raw external-call spy;
- environment input/evidence;
- regression summary;
- case evidence;
- verification summary;
- run/toolchain metadata.

It then creates:

~~~text
u06-artifact-manifest.json
SHA256SUMS
~~~

`SHA256SUMS` covers every retained uploaded evidence file except itself.

## 4. Run / toolchain / upload identity

A durable run metadata file records:

- workflow run ID / attempt;
- exact numeric authoritative job ID;
- workflow/ref/event;
- GitHub checkout SHA/ref/head-ref;
- implementation semantic SHA;
- runner OS/arch/name/image;
- platform;
- Java, Maven, Python and Docker versions;
- authoritative container image;
- artifact logical name;
- generated timestamp.

After the primary artifact upload, the workflow uses upload-artifact outputs to generate a separately retained upload receipt binding:

- artifact ID;
- artifact name;
- artifact URL;
- artifact digest;
- workflow run ID;
- implementation semantic SHA;
- GitHub checkout SHA.

The receipt is independently checksummed and retained as a second 90-day artifact.

## 5. Retention

~~~text
primary evidence artifact retention
= 90 days

upload receipt artifact retention
= 90 days
~~~

This replaces the prior 14-day policy.

## 6. Boundary

No runtime implementation source is changed by this remediation.

Therefore:

~~~text
IMPLEMENTATION_SEMANTIC_PASS_SHA
= 66fa3c078dce5e304e2ada9d6c0df0ff9fbde661

Implementation Semantic Re-Freeze
= remains valid
~~~

A fresh authoritative run is mandatory because verifier/workflow/evidence packaging changes.

No Merge Authorization or production authorization is granted.
