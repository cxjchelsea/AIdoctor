# POSTFREEZE-03B-C Raw OCR Real Tesseract Engine Technical Verification Evidence

> Dated: 2026-08-19
>
> Classification: `BOUNDED NON-CLINICAL IMPLEMENTATION` + `ENGINE-ONLY REAL TESSERACT TECHNICAL PROOF` + `DRAFT PR`
>
> Batch: `POSTFREEZE-03B-C`
>
> Status: `IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`
>
> Authorization token:
> `POSTFREEZE_03B_C_RAW_OCR_REAL_TESSERACT_ENGINE_TECHNICAL_VERIFICATION_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Authorized Base: `5557b255ecd33cd54a18b1042c7b11717ea72030`
>
> Authorized Base tree: `9722ac1b0753a896155ea6fdd5fb665e6973f08a`
>
> Implementation branch: `agent/postfreeze-03b-c-real-tesseract-engine-verification`
>
> Frozen engineering snapshot:
> `840a6fc7f83cec4fda6d53d739d7fd6b8013f031` /
> `d68d96d49eda96fe93dfb0c6c013b961a8e4b966`

```text
real Tesseract execution
!=
OCR accuracy validation
!=
Chinese OCR quality validation
!=
real patient-image OCR
!=
canonical Runtime enablement
!=
repository Runtime verification
!=
Java cutover
!=
clinical Runtime
!=
Phase B
```

## 1. Authorization

- Token: `POSTFREEZE_03B_C_RAW_OCR_REAL_TESSERACT_ENGINE_TECHNICAL_VERIFICATION_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
- Repository: `cxjchelsea/AIdoctor`
- Base branch: `agent/enterprise-agent-refactoring-plan`
- Implementation branch: `agent/postfreeze-03b-c-real-tesseract-engine-verification`
- Authorized exact Base: `5557b255ecd33cd54a18b1042c7b11717ea72030`
- Authorized Base tree: `9722ac1b0753a896155ea6fdd5fb665e6973f08a`
- Live preflight Enterprise HEAD: `5557b255ecd33cd54a18b1042c7b11717ea72030`
- Live preflight Enterprise tree: `9722ac1b0753a896155ea6fdd5fb665e6973f08a`
- Drift: none
- POSTFREEZE-01 / 02 / 03A / 03B-A / 03B-B: `DURABLY_CLOSED`
- 03B-B merge: `5557b255ecd33cd54a18b1042c7b11717ea72030`
- 03B-B post-merge push CI: `32225660414` / push / success
  (historical only; not reused as 03B-C CI)

## 2. Objective

Prove the current production-shaped engine path can execute an actual Tesseract
binary:

```text
synthetic non-medical image bytes
    ->
PIL decode
    ->
OpenCV / NumPy preprocessing
    ->
pytesseract Python wrapper
    ->
ACTUAL tesseract binary
    ->
default chi_sim+eng traineddata
    ->
raw OCR text
```

Primary call:

`RawOcrEngine.recognize_from_bytes(image_bytes)`

with no `language` override, so the existing default `chi_sim+eng` is executed.

## 3. Non-objectives

This batch does not:

- enable raw OCR on the default canonical Runtime
- register `engineering.ocr.raw`
- change HTTP routes, `create_app()`, ToolRouter, ArtifactPort, or transport
- change Java / Feign / diagnosis-service
- change `ocr-service/app/services/raw_ocr.py` or any production OCR source
- change `ocr-service/Dockerfile` or `ocr-service/requirements.txt`
- add a committed PNG/JPG fixture
- validate OCR accuracy or Chinese OCR quality
- use real patient images or PHI
- repair `LEGACY_OCR_ROUTE_MISMATCH`
- start POSTFREEZE-03B-D or injected Runtime real-Tesseract integration

## 4. Exact three-file scope

NEW:

1. `ocr-service/tests/test_raw_ocr_real_engine.py`
2. `docs/refactoring/evidence/phase-a/engineering-baseline/postfreeze-03b-c/postfreeze-03b-c-real-tesseract-engine.md`

MODIFY:

3. `.github/workflows/ci.yml`

Unexpected files = 0

Not changed:

- `ocr-service/app/services/raw_ocr.py`
- `ocr-service/app/services/ocr_service.py`
- `ocr-service/app/api/routes.py`
- `ocr-service/app/main.py`
- `ocr-service/requirements.txt`
- `ocr-service/Dockerfile`
- `packages/python_runtime/**`
- `packages/model_runtime/**`
- `contracts/**`
- `capabilities/**`
- `diagnosis-service/**`
- `examination-service/**`
- any Java source
- any other Dockerfile
- any production capability definition
- any committed image fixture

The historical `ocr-separation` job is not converted. The only line change
inside that job is an additional `--ignore=tests/test_raw_ocr_real_engine.py`
so mocked-separation evidence is not mixed with the real-engine file.

## 5. Real-engine architecture path

The test instantiates the existing `RawOcrEngine` and calls
`recognize_from_bytes()` without mocks.

That path remains:

1. PIL `Image.open` decode of in-memory PNG bytes
2. historical `preprocess_image` (grayscale, Otsu, `fastNlMeansDenoising`,
   contrast `alpha=1.5`)
3. `pytesseract.image_to_string` with default `chi_sim+eng`
4. actual `tesseract` binary on PATH

This batch does not set `tesseract_cmd` and does not change preprocessing.

## 6. Synthetic / non-PHI fixture policy

- Image is generated in memory with existing Pillow `ImageFont.load_default(size=)`.
- No font package is added. No font is downloaded. No network is used.
- No PNG/JPG is committed.
- Synthetic text is engineering-only: `HELLO OCR 123`.
- `PHI_FIXTURE_USED = NO`
- `CLINICAL_SURFACE_DELTA = 0`

## 7. Default chi_sim+eng primary proof

`RawOcrEngine.DEFAULT_OCR_LANGUAGE` remains `"chi_sim+eng"`.

The primary test:

- asserts that constant is still `"chi_sim+eng"`
- calls `engine.recognize_from_bytes(image_bytes)` with no `language=` argument
- does not use `language="eng"` as the stage-closing proof

Successful default-path execution, if later proven by exact-Head CI, shows only:

- real Tesseract binary executable
- `eng` traineddata loadable
- `chi_sim` traineddata loadable
- current default `chi_sim+eng` configuration executable
- real decode / preprocess / Tesseract path executable

It does **not** prove `CHINESE_OCR_QUALITY_VERIFIED`.

## 8. Local skip vs authoritative CI fail

```text
AIDOCTOR_REQUIRE_REAL_TESSERACT == "1"
```

| Environment | Missing binary or required traineddata |
|---|---|
| Local / non-required | `pytest.skip` with explicit reason |
| Authoritative CI | `pytest.fail` — no skip, job must not go green |

Required CI languages inspected via `tesseract --list-langs`:

- `eng`
- `chi_sim`

A local skip must be recorded as `LOCAL_REAL_TESSERACT_TEST = SKIPPED_NO_LOCAL_BINARY`.
A local skip must **not** be written as `TESSERACT_ENGINE_VERIFIED = YES`.

Authoritative proof is only the isolated `ocr-real-engine` job, and only when
that job shows actual execution (`N passed`, `0 skipped` for this file).

## 9. CI environment

New isolated job: `ocr-real-engine`

- `runs-on: ubuntu-24.04` (not `ubuntu-latest`)
- same pinned `actions/checkout` and `actions/setup-python` as existing OCR CI
- Python 3.10
- apt: `tesseract-ocr`, `tesseract-ocr-eng`, `tesseract-ocr-chi-sim`
- pip: only `ocr-service/requirements.txt` + `pytest`
- does **not** install `packages/model_runtime/requirements-ci.txt`
- does **not** install `packages/python_runtime/requirements-http.txt`
- `working-directory: ocr-service`
- `AIDOCTOR_REQUIRE_REAL_TESSERACT=1`
- command: `python -m pytest tests/test_raw_ocr_real_engine.py -q`

Historical `ocr-separation` remains:

- no apt Tesseract
- mocked `image_to_string` historical suite
- injected Runtime composition with mocked engine
- ignores the new real-engine test file

Exact apt Tesseract package versions are not pinned in this batch.

## 10. Tesseract version evidence location

Authoritative identity is recorded by the `ocr-real-engine` job step
`Record Tesseract engine identity`:

```text
python --version
which tesseract
tesseract --version
tesseract --list-langs
```

Those values exist only in that CI log after the exact-Head run. This document
does not invent them at authoring time.

`CI_VERIFIED = PENDING_EXACT_HEAD_CI`

F004 remains preserved: do not later create a doc-only recursive Head solely to
replace this pending CI text.

## 11. Assertion policy

OCR output is normalized only enough to remove whitespace variance:

```text
normalized = " ".join(raw_text.upper().split())
```

Required token containment:

- `HELLO`
- `OCR`

`123` is rendered in the synthetic image but is not a required assertion.
No OCR accuracy metric is introduced.

## 12. Evidence ceiling

Before exact-Head authoritative `ocr-real-engine` execution:

```text
TESSERACT_ENGINE_VERIFIED = NO
SYNTHETIC_IMAGE_REAL_TESSERACT_EXECUTION_VERIFIED = NO
RAW_OCR_DEFAULT_LANGUAGE_REAL_ENGINE_EXECUTION_VERIFIED = NO
CI_VERIFIED = PENDING_EXACT_HEAD_CI
```

If later exact-Head CI proves actual execution (`N passed`, `0 skipped`),
independent review may elevate the three engine labels above to `YES`.

These MUST remain `NO` even after a green engine job:

```text
REAL_IMAGE_OCR_VERIFIED = NO
CHINESE_OCR_QUALITY_VERIFIED = NO
OCR_ACCURACY_VERIFIED = NO
CANONICAL_RUNTIME_RAW_OCR_ENABLED = NO
REAL_ARTIFACT_STORAGE_BACKEND_VERIFIED = NO
FULL_SHARED_CONTRACT_SEMANTIC_VALIDATOR_RUNTIME_VERIFIED = NO
JAVA_REAL_TOOL_PROTOCOL_VERIFIED = NO
JAVA_PYTHON_CUTOVER_VERIFIED = NO
REPOSITORY_RUNTIME_VERIFIED = NO
OCR_DOCKER_RUNTIME_VERIFIED = NO
CLINICAL_VALIDATED = NO
PRODUCTION_VERIFIED = NO
```

CI using apt packages does **not** prove `OCR_DOCKER_RUNTIME_VERIFIED` or
`REPOSITORY_RUNTIME_VERIFIED`. Existing `ocr-service/Dockerfile` Tesseract
install is historical context only and is not modified.

## 13. Governance preservation

Must remain exact:

- A7-NC = COMPLETE / CLOSED / STABLE
- A7 = NOT_COMPLETE
- A7-CL = DEFERRED_OUT_OF_CURRENT_BASELINE / NOT COMPLETE
- A7-CL-02 = NOT_AUTHORIZED
- FB-11 = UNSATISFIED / PRESERVED
- FB-21 = PRESERVED / UNSATISFIED THROUGH A7 NOT_COMPLETE
- A11 = NOT_PASSED
- Original Frozen = NOT_READY_BLOCKED_CLINICAL + UNREACHABLE_UNDER_CURRENT_RESOURCE_MODEL
- Clinical Runtime = NOT_ENABLED
- Production = BLOCKED
- Phase B = NOT_AUTHORIZED
- F001 = PRESERVED
- F004 = PRESERVED
- LEGACY_OCR_ROUTE_MISMATCH = PRESERVED
- DEFAULT_RUNTIME_CAPABILITY_SURFACE_UNCHANGED = YES
- CANONICAL_RUNTIME_RAW_OCR_ENABLED = NO

Never write: `A7 = CLOSED`

## 14. Explicit non-claims

This batch does not claim and must not be written as:

- `TESSERACT_ENGINE_VERIFIED = YES` from local skip
- `CHINESE_OCR_QUALITY_VERIFIED = YES` because `chi_sim` loaded
- `REAL_IMAGE_OCR_VERIFIED = YES` because a synthetic image passed
- `CANONICAL_RUNTIME_RAW_OCR_ENABLED = YES`
- `REPOSITORY_RUNTIME_VERIFIED = YES` because CI is green
- `OCR_DOCKER_RUNTIME_VERIFIED = YES` because apt packages were installed
- Java OCR cutover
- real artifact storage
- legacy OCR route repair
- Clinical Runtime
- Phase B
- Ready / merge
- `DURABLY_CLOSED`

```text
chi_sim traineddata successfully loaded
!=
Chinese OCR quality validation

synthetic image real OCR
!=
real patient-image OCR

CI green
!=
repository Runtime verification

engine-only test
!=
Java cutover
```

## 15. Local authoring-time test note

Authoring machine: Windows, Python 3.13.5, no local `tesseract` binary.

Expected local real-engine result under non-required mode:

```text
LOCAL_REAL_TESSERACT_TEST = SKIPPED_NO_LOCAL_BINARY
```

That result is not engine verification.

## 16. Author status

`IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`

```text
CI_VERIFIED = PENDING_EXACT_HEAD_CI
POSTFREEZE_03B_C_DURABLY_CLOSED = NO
MERGE_AUTHORIZATION_GRANTED = NO
```

Do not write `DURABLY_CLOSED` for POSTFREEZE-03B-C in this document.
