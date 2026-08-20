# POSTFREEZE-03B-E OCR Docker Raw Engine Technical Verification Evidence

> Dated: 2026-08-20
>
> Classification: `BOUNDED NON-CLINICAL IMPLEMENTATION` + `DOCKER IMAGE RAW ENGINE TECHNICAL PROOF` + `DRAFT PR`
>
> Batch: `POSTFREEZE-03B-E` (+ remediation `POSTFREEZE-03B-E-R1`)
>
> Status: `IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`
>
> Authorization token:
> `POSTFREEZE_03B_E_OCR_DOCKER_RAW_ENGINE_TECHNICAL_VERIFICATION_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Remediation authorization token:
> `POSTFREEZE_03B_E_R1_DOCKERFILE_COMPATIBILITY_REMEDIATION_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Authorized Base: `5a496f2b73441e7e33d462e80dcf087928763d9a`
>
> Authorized Base tree: `4d15ac151de1814886046639bd7e41500b084ac8`
>
> Implementation branch: `agent/postfreeze-03b-e-ocr-docker-raw-engine`
>
> PR: `#64`
>
> Frozen engineering snapshot (Enterprise at authorization):
> `5a496f2b73441e7e33d462e80dcf087928763d9a` /
> `4d15ac151de1814886046639bd7e41500b084ac8`

```text
OCR Docker image RawOcrEngine execution
!=
OCR FastAPI service startup in Docker
!=
OCR HTTP endpoint proof
!=
Python Runtime inside Docker
!=
repository Runtime verification
!=
canonical Runtime raw OCR enablement
!=
OCR accuracy validation
!=
Chinese OCR quality validation
!=
real patient-image OCR
!=
Java cutover
!=
clinical Runtime
!=
Phase B
```

## 1. Authorization

- Token: `POSTFREEZE_03B_E_OCR_DOCKER_RAW_ENGINE_TECHNICAL_VERIFICATION_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
- Remediation token: `POSTFREEZE_03B_E_R1_DOCKERFILE_COMPATIBILITY_REMEDIATION_EXPLICIT_AUTHORIZATION_GRANTED`
- Repository: `cxjchelsea/AIdoctor`
- Base branch: `agent/enterprise-agent-refactoring-plan`
- Implementation branch: `agent/postfreeze-03b-e-ocr-docker-raw-engine`
- Draft PR: `#64`
- Authorized exact Base: `5a496f2b73441e7e33d462e80dcf087928763d9a`
- Authorized Base tree: `4d15ac151de1814886046639bd7e41500b084ac8`
- Live preflight Enterprise HEAD: `5a496f2b73441e7e33d462e80dcf087928763d9a`
- Live preflight Enterprise tree: `4d15ac151de1814886046639bd7e41500b084ac8`
- Drift: none
- POSTFREEZE-01 / 02 / 03A / 03B-A / 03B-B / 03B-C / 03B-D: `DURABLY_CLOSED`
- 03B-D merge: `5a496f2b73441e7e33d462e80dcf087928763d9a`
- 03B-D post-merge push CI: `32321903372` / push / success
  (historical only; not reused as 03B-E CI)

## 2. Objective

Exact evidence question (unchanged by R1):

Can the OCR image built directly from the current `ocr-service/Dockerfile`
execute `RawOcrEngine` with its default `chi_sim+eng` language against an
in-memory synthetic engineering image, using the Tesseract binary and
traineddata actually contained inside that Docker image, while leaving the
default Python Runtime capability surface unchanged?

Proof chain:

```text
current Enterprise
    ->
docker build ./ocr-service
    ->
built OCR image
    ->
container process
    ->
RawOcrEngine
    ->
default chi_sim+eng
    ->
pytesseract
    ->
Tesseract binary inside container
    ->
synthetic HELLO/OCR recognition
```

Primary stage-closing label candidate:

`OCR_DOCKER_RAW_ENGINE_VERIFIED`

Decomposition labels (if useful):

- `OCR_DOCKER_IMAGE_BUILD_VERIFIED`
- `OCR_DOCKER_TESSERACT_IDENTITY_VERIFIED`
- `OCR_DOCKER_DEFAULT_LANGUAGE_RAW_ENGINE_EXECUTION_VERIFIED`

Do **not** upgrade:

`OCR_DOCKER_RUNTIME_VERIFIED`

## 3. Non-objectives

This batch does not:

- redesign Docker base-image strategy (no bookworm pin / digest pin)
- start OCR FastAPI / HTTP verification inside Docker
- prove Python Runtime composition inside Docker
- register raw OCR on the default canonical Runtime
- prove repository Runtime
- change Artifact storage
- validate OCR accuracy or Chinese OCR quality
- use real patient images or PHI
- repair `LEGACY_OCR_ROUTE_MISMATCH`
- start POSTFREEZE-03B-F

OCR Python production sources, Runtime, Java, contracts, capabilities, and
clinical code remain unchanged.

## 4. Initial three-file attempt (blocked)

Initial authorized attempt changed exactly:

1. NEW `ocr-service/tests/docker_raw_engine_probe.py`
2. NEW this evidence document
3. MODIFY `.github/workflows/ci.yml`

Blocked Head:

`eddedefc9f8f583aa1d6f1d2c6408322a4372b81`

Blocked tree:

`7f2722cb4279f9a85ed46ece7fc60b45c2fa901a`

Historical failed exact-Head CI (evidence of blocker only; not authoritative for closure):

`32324032814` / `pull_request` / `failure`

Only failing job:

`ocr-docker-raw-engine`

Exact build blocker:

```text
FROM python:3.10-slim
resolved during CI to a Debian trixie-based image
=>
apt package libgl1-mesa-glx has no installation candidate
```

Result at that Head:

```text
POSTFREEZE_03B_E_IMPLEMENTATION = BLOCKED_SCOPE_EXPANSION_REQUIRED
OCR_DOCKER_RAW_ENGINE_VERIFIED = NO
```

Do **not** rerun `32324032814` as authoritative proof after remediation.

## 5. POSTFREEZE-03B-E-R1 remediation

Scope-expansion authorization:

`POSTFREEZE_03B_E_R1_DOCKERFILE_COMPATIBILITY_REMEDIATION_EXPLICIT_AUTHORIZATION_GRANTED`

Exact Dockerfile package remediation:

```text
-    libgl1-mesa-glx \
+    libgl1 \
```

Why:

- current `python:3.10-slim` resolves to Debian 13 (trixie)
- `libgl1-mesa-glx` is obsolete / unavailable on that base
- `libgl1` is the minimal package-name replacement restoring OpenCV/GL runtime linkage without changing base image strategy

Unchanged by R1:

```text
DOCKERFILE_BASE_IMAGE_CHANGE = NO
DOCKERFILE_CMD_CHANGE = NO
DOCKERFILE_PORT_CHANGE = NO
DOCKERFILE_TESSERACT_PACKAGE_CHANGE = NO
```

Classification:

```text
POSTFREEZE_03B_E_DOCKERFILE_DELTA
= 1_BOUNDED_COMPATIBILITY_REMEDIATION

DOCKERFILE_PACKAGE_REMEDIATION
= libgl1-mesa-glx -> libgl1

OCR_PYTHON_PRODUCTION_SOURCE_DELTA = 0
PYTHON_RUNTIME_SOURCE_DELTA = 0
JAVA_SOURCE_DELTA = 0
CONTRACT_DELTA = 0
CAPABILITY_DELTA = 0
CLINICAL_SOURCE_DELTA = 0
```

After the remediating image built successfully, the already-authorized probe revealed
a real execution defect under the documented command
`python tests/docker_raw_engine_probe.py`:

```text
ModuleNotFoundError: No module named 'app'
```

Cause: when Python runs a script path, `sys.path[0]` is `tests/`, so the image
`WORKDIR` `/app` is not automatically importable.

Authorized probe-only fix (same remediation commit; CI job unchanged):

- insert the probe parent directory (`/app`) onto `sys.path` before importing
  `app.services.raw_ocr`

No production OCR source change. No CI structural change.

## 6. Total PR scope after remediation

Base → final Head may contain exactly these 4 files:

1. MODIFY `ocr-service/Dockerfile`
2. NEW `ocr-service/tests/docker_raw_engine_probe.py`
3. NEW/updated `docs/refactoring/evidence/phase-a/engineering-baseline/postfreeze-03b-e/postfreeze-03b-e-ocr-docker-raw-engine.md`
4. MODIFY `.github/workflows/ci.yml`

Unexpected files = 0

Probe and CI job semantics remain the originally authorized 03B-E design unless
a post-build probe defect is independently demonstrated (none authorized here).

## 7. Built-image proof model

Authoritative proof requires a **fresh** exact-Head CI after remediation:

1. `docker build -t aidoctor-ocr:postfreeze-03b-e ./ocr-service`
2. identity recording of the built image
3. container checks for Python / Tesseract / langs
4. stage-closing:

```text
docker run --rm aidoctor-ocr:postfreeze-03b-e \
  python tests/docker_raw_engine_probe.py
```

Hard constraints:

- no host Tesseract installation in the new job
- no post-build `pip install` / `apt install` inside the running container
- no volume-mounted replacement of the stage-closing probe
- no pytest inside the image for this proof
- overriding Dockerfile CMD with `python` / `tesseract` is acceptable for engine proof
  and does **not** prove HTTP service startup

```text
OCR_SERVICE_HTTP_IN_DOCKER_VERIFIED = NO
OCR_DOCKER_RUNTIME_VERIFIED = NO
```

## 8. Probe design

Standalone file: `ocr-service/tests/docker_raw_engine_probe.py`

- filename does **not** begin with `test_` (avoid accidental pytest collection)
- no pytest dependency
- exit 0 only on success; non-zero on any failed requirement
- no skip behavior
- require `shutil.which("tesseract")`
- require `tesseract --list-langs` contains `eng` and `chi_sim`
- import production `DEFAULT_OCR_LANGUAGE` / `RawOcrEngine`
- require `DEFAULT_OCR_LANGUAGE == "chi_sim+eng"`
- no language override on `recognize_from_bytes`
- in-memory synthetic `HELLO OCR 123` PNG via Pillow `load_default(size=96)`
- assert tokens `HELLO` and `OCR` after normalize
- no accuracy / CER / WER / Chinese quality thresholds
- print audit markers ending with `OCR_DOCKER_RAW_ENGINE_PROBE=PASS`

```text
PHI_REQUIRED = NO
PHI_FIXTURE_USED = NO
RAW_OCR_CLINICAL_BOUNDARY_PRESERVED = YES
```

## 9. CI job strategy

Isolated job: `ocr-docker-raw-engine` on `ubuntu-24.04`

Preserved existing jobs and semantics:

- `ocr-separation` (mocked separation)
- `ocr-real-engine` (03B-C host apt real engine)
- `ocr-runtime-real-engine` (03B-D test-injected Runtime + host apt)

03B-E meaning:

```text
RawOcrEngine + real Tesseract inside repository-built OCR Docker image
```

Authoring-time CI pointer (intentional; F004 preserved):

```text
CI_VERIFIED = PENDING_EXACT_HEAD_CI
```

Do not create a documentation-only commit merely to replace this pointer with a
CI run ID. External CI evidence closes the pointer.

Historical failed run `32324032814` remains blocker evidence only.

## 10. Pre-CI evidence state after remediation commit

Before authoritative **new** exact-Head CI succeeds:

```text
OCR_DOCKER_IMAGE_BUILD_VERIFIED = NO
OCR_DOCKER_TESSERACT_IDENTITY_VERIFIED = NO
OCR_DOCKER_DEFAULT_LANGUAGE_RAW_ENGINE_EXECUTION_VERIFIED = NO
OCR_DOCKER_RAW_ENGINE_VERIFIED = NO
```

After successful fresh exact-Head CI but before independent review:

```text
OCR_DOCKER_IMAGE_BUILD_VERIFIED = VERIFIED_SUCCESS_PENDING_INDEPENDENT_REVIEW
OCR_DOCKER_TESSERACT_IDENTITY_VERIFIED = VERIFIED_SUCCESS_PENDING_INDEPENDENT_REVIEW
OCR_DOCKER_DEFAULT_LANGUAGE_RAW_ENGINE_EXECUTION_VERIFIED = VERIFIED_SUCCESS_PENDING_INDEPENDENT_REVIEW
OCR_DOCKER_RAW_ENGINE_VERIFIED = PENDING_INDEPENDENT_REVIEW
```

## 11. Evidence ceiling (must remain)

Even after successful 03B-E + R1:

```text
OCR_DOCKER_RUNTIME_VERIFIED = NO
OCR_SERVICE_HTTP_IN_DOCKER_VERIFIED = NO
CANONICAL_RUNTIME_RAW_OCR_ENABLED = NO
REPOSITORY_RUNTIME_VERIFIED = NO
REAL_ARTIFACT_STORAGE_BACKEND_VERIFIED = NO
FULL_SHARED_CONTRACT_SEMANTIC_VALIDATOR_RUNTIME_VERIFIED = NO
JAVA_REAL_TOOL_PROTOCOL_VERIFIED = NO
JAVA_PYTHON_CUTOVER_VERIFIED = NO
REAL_IMAGE_OCR_VERIFIED = NO
CHINESE_OCR_QUALITY_VERIFIED = NO
OCR_ACCURACY_VERIFIED = NO
CLINICAL_VALIDATED = NO
PRODUCTION_VERIFIED = NO
```

Default Runtime non-regression:

```text
DEFAULT_RUNTIME_CAPABILITY_SURFACE_UNCHANGED = YES
CANONICAL_RUNTIME_RAW_OCR_ENABLED = NO
```

Legacy route:

```text
LEGACY_OCR_ROUTE_MISMATCH = PRESERVED
```

## 12. Governance preservation

```text
A7-NC = COMPLETE / CLOSED / STABLE
A7 = NOT_COMPLETE
A7-CL = DEFERRED_OUT_OF_CURRENT_BASELINE / NOT COMPLETE
A7-CL-02 = NOT_AUTHORIZED
FB-11 = UNSATISFIED / PRESERVED
FB-21 = PRESERVED / UNSATISFIED THROUGH A7 NOT_COMPLETE
A11 = NOT_PASSED
Original Frozen = NOT_READY_BLOCKED_CLINICAL + UNREACHABLE_UNDER_CURRENT_RESOURCE_MODEL
Clinical Runtime = NOT_ENABLED
Production = BLOCKED
Phase B = NOT_AUTHORIZED
F001 = PRESERVED
F004 = PRESERVED
adult_respiratory_v1 = DRAFT / PARTIALLY_VALIDATED / REQUIRES_CLINICAL_REVIEW / NOT_IMPLEMENTED
```

Never write: `A7 = CLOSED`.

## 13. Stop state

After remediation + fresh exact-Head CI success report:

```text
READY_FOR_POSTFREEZE_03B_E_INDEPENDENT_REVIEW = YES
READY_FOR_POSTFREEZE_03B_E_PREMERGE_VERIFICATION = NO
MERGE_AUTHORIZATION_GRANTED = NO
POSTFREEZE_03B_E_DURABLY_CLOSED = NO
```

STOP. Do not mark Ready, merge, self-authorize merge, declare independent
review PASS, perform PMV, declare durable closure, pin/change base image,
start HTTP-in-Docker, Runtime opt-in, canonical registration, real Artifact
storage, Java work, repository Runtime, real images, legacy route repair,
PHI, or POSTFREEZE-03B-F.

Wait for independent review.
