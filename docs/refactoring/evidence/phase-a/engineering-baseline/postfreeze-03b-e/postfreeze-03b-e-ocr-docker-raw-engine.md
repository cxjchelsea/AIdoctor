# POSTFREEZE-03B-E OCR Docker Raw Engine Technical Verification Evidence

> Dated: 2026-08-20
>
> Classification: `BOUNDED NON-CLINICAL IMPLEMENTATION` + `DOCKER IMAGE RAW ENGINE TECHNICAL PROOF` + `DRAFT PR`
>
> Batch: `POSTFREEZE-03B-E`
>
> Status: `IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`
>
> Authorization token:
> `POSTFREEZE_03B_E_OCR_DOCKER_RAW_ENGINE_TECHNICAL_VERIFICATION_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Authorized Base: `5a496f2b73441e7e33d462e80dcf087928763d9a`
>
> Authorized Base tree: `4d15ac151de1814886046639bd7e41500b084ac8`
>
> Implementation branch: `agent/postfreeze-03b-e-ocr-docker-raw-engine`
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
- Repository: `cxjchelsea/AIdoctor`
- Base branch: `agent/enterprise-agent-refactoring-plan`
- Implementation branch: `agent/postfreeze-03b-e-ocr-docker-raw-engine`
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

Exact evidence question:

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

- modify `ocr-service/Dockerfile`
- modify production OCR / Runtime / Java / contracts / clinical source
- start OCR FastAPI / HTTP verification inside Docker
- prove Python Runtime composition inside Docker
- register raw OCR on the default canonical Runtime
- prove repository Runtime
- change Artifact storage
- validate OCR accuracy or Chinese OCR quality
- use real patient images or PHI
- repair `LEGACY_OCR_ROUTE_MISMATCH`
- start POSTFREEZE-03B-F

## 4. Exact three-file scope

NEW:

1. `ocr-service/tests/docker_raw_engine_probe.py`
2. `docs/refactoring/evidence/phase-a/engineering-baseline/postfreeze-03b-e/postfreeze-03b-e-ocr-docker-raw-engine.md`

MODIFY:

3. `.github/workflows/ci.yml`

Expected: 3 files exactly.

Not changed:

- `ocr-service/Dockerfile`
- `ocr-service/requirements.txt`
- `ocr-service/app/**`
- `packages/python_runtime/**`
- `packages/model_runtime/**`
- `contracts/**`
- `capabilities/**`
- `diagnosis-service/**`
- any Java source
- `docker-compose.yml`
- any clinical file

```text
PRODUCTION_SOURCE_DELTA = 0
DOCKERFILE_DELTA = 0
DEFAULT_RUNTIME_SOURCE_DELTA = 0
JAVA_SOURCE_DELTA = 0
```

## 5. Dockerfile assumption (unchanged)

LIVE current `ocr-service/Dockerfile` (verified before implementation):

- `FROM python:3.10-slim`
- installs `tesseract-ocr`, `tesseract-ocr-chi-sim`, `tesseract-ocr-eng`
- installs Python deps from `ocr-service/requirements.txt`
- copies OCR source into the image
- starts `uvicorn app.main:app --host 0.0.0.0 --port 8087`

03B-E verifies this existing Dockerfile. It is **not** a Dockerfile remediation stage.

```text
DOCKERFILE_DELTA = 0
```

## 6. Built-image proof model

Authoritative proof requires:

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

## 7. Probe design

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

## 8. CI job strategy

New isolated job: `ocr-docker-raw-engine` on `ubuntu-24.04`

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

## 9. Pre-CI evidence state

Before authoritative exact-Head CI succeeds:

```text
OCR_DOCKER_IMAGE_BUILD_VERIFIED = NO
OCR_DOCKER_TESSERACT_IDENTITY_VERIFIED = NO
OCR_DOCKER_DEFAULT_LANGUAGE_RAW_ENGINE_EXECUTION_VERIFIED = NO
OCR_DOCKER_RAW_ENGINE_VERIFIED = NO
```

After successful exact-Head CI but before independent review:

```text
OCR_DOCKER_IMAGE_BUILD_VERIFIED = VERIFIED_SUCCESS_PENDING_INDEPENDENT_REVIEW
OCR_DOCKER_TESSERACT_IDENTITY_VERIFIED = VERIFIED_SUCCESS_PENDING_INDEPENDENT_REVIEW
OCR_DOCKER_DEFAULT_LANGUAGE_RAW_ENGINE_EXECUTION_VERIFIED = VERIFIED_SUCCESS_PENDING_INDEPENDENT_REVIEW
OCR_DOCKER_RAW_ENGINE_VERIFIED = PENDING_INDEPENDENT_REVIEW
```

## 10. Evidence ceiling (must remain)

Even after successful 03B-E:

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

## 11. Governance preservation

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

## 12. Stop state

After implementation + exact-Head CI success report:

```text
READY_FOR_POSTFREEZE_03B_E_INDEPENDENT_REVIEW = YES
READY_FOR_POSTFREEZE_03B_E_PREMERGE_VERIFICATION = NO
MERGE_AUTHORIZATION_GRANTED = NO
POSTFREEZE_03B_E_DURABLY_CLOSED = NO
```

STOP. Do not mark Ready, merge, self-authorize merge, declare independent
review PASS, perform PMV, declare durable closure, edit Dockerfile, start
HTTP-in-Docker, Runtime opt-in, canonical registration, real Artifact storage,
Java work, repository Runtime, real images, legacy route repair, PHI, or
POSTFREEZE-03B-F.

Wait for independent review.
