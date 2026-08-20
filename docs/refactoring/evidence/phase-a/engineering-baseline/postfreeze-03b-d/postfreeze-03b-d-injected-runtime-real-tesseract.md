# POSTFREEZE-03B-D Injected Runtime Raw OCR Composition with Real Tesseract Evidence

> Dated: 2026-08-20
>
> Classification: `BOUNDED NON-CLINICAL IMPLEMENTATION` + `TEST-INJECTED RUNTIME COMPOSITION` + `REAL TESSERACT` + `DRAFT PR`
>
> Batch: `POSTFREEZE-03B-D`
>
> Status: `IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`
>
> Authorization token:
> `POSTFREEZE_03B_D_INJECTED_RUNTIME_RAW_OCR_REAL_TESSERACT_COMPOSITION_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Authorized Base: `4674c761bfb4dd5a3730f821eeec75591cde6afa`
>
> Authorized Base tree: `bf04dbe0d93fcf43ff3369f396501dc548f44d82`
>
> Implementation branch: `agent/postfreeze-03b-d-injected-runtime-real-tesseract`
>
> Frozen engineering snapshot:
> `840a6fc7f83cec4fda6d53d739d7fd6b8013f031` /
> `d68d96d49eda96fe93dfb0c6c013b961a8e4b966`

```text
injected Runtime HTTP + real Tesseract
!=
default Runtime raw OCR registration
!=
canonical Runtime enablement
!=
repository Runtime verification
!=
OCR Docker runtime verification
!=
Java cutover
!=
real artifact storage
!=
OCR accuracy / Chinese OCR quality
!=
clinical Runtime
!=
Phase B
```

## 1. Authorization

- Token: `POSTFREEZE_03B_D_INJECTED_RUNTIME_RAW_OCR_REAL_TESSERACT_COMPOSITION_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
- Repository: `cxjchelsea/AIdoctor`
- Base branch: `agent/enterprise-agent-refactoring-plan`
- Implementation branch: `agent/postfreeze-03b-d-injected-runtime-real-tesseract`
- Authorized exact Base: `4674c761bfb4dd5a3730f821eeec75591cde6afa`
- Authorized Base tree: `bf04dbe0d93fcf43ff3369f396501dc548f44d82`
- Live preflight Enterprise HEAD: `4674c761bfb4dd5a3730f821eeec75591cde6afa`
- Live preflight Enterprise tree: `bf04dbe0d93fcf43ff3369f396501dc548f44d82`
- Drift: none
- POSTFREEZE-01 / 02 / 03A / 03B-A / 03B-B / 03B-C: `DURABLY_CLOSED`
- 03B-C merge: `4674c761bfb4dd5a3730f821eeec75591cde6afa`
- 03B-C post-merge push CI: `32319125290` / push / success
  (historical only; not reused as 03B-D CI)

## 2. Objective

Close the product of previously separate proofs:

```text
03B-B: Injected Runtime HTTP + RawOcrEngine decode/preprocess + MOCKED pytesseract
03B-C: REAL Tesseract + synthetic image + NO Runtime HTTP
03B-D: Injected Runtime HTTP + ToolContext + StaticAllowlistArtifactPort
       + RawOcrToolAdapter + RawOcrEngine + REAL pytesseract
       + REAL Tesseract binary + default chi_sim+eng
```

Exact evidence question:

Can a test-injected Python Runtime HTTP composition execute raw OCR end-to-end
through ToolContext + StaticAllowlistArtifactPort + RawOcrToolAdapter +
RawOcrEngine + the actual Tesseract binary using the current default
chi_sim+eng path, while leaving the default Runtime capability surface
unchanged?

## 3. Exact three-file scope

NEW:

1. `ocr-service/tests/test_raw_ocr_runtime_real_engine.py`
2. `docs/refactoring/evidence/phase-a/engineering-baseline/postfreeze-03b-d/postfreeze-03b-d-injected-runtime-real-tesseract.md`

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
- `packages/python_runtime/**` (production sources)
- `packages/model_runtime/**`
- `contracts/**`
- `capabilities/**`
- Java sources
- any production Dockerfile

## 4. Test-only nature

Composition exists only inside the new test file.

- Registers `engineering.ocr.raw` only via test-local `ToolRouter`
- Injects `DeterministicRuntimeExecutor` + `StaticAllowlistArtifactPort` into
  `create_app(...)`
- Does **not** modify default `create_app()`
- Does **not** add `engineering.ocr.raw` to production Runtime sources
- Existing architecture guards remain unchanged and must stay green

## 5. Full execution chain

```text
POST /api/v1/runtime/tools/invoke
  -> ToolContext (synthetic)
  -> ArtifactPort.resolve (StaticAllowlistArtifactPort)
  -> ResolvedArtifact.content
  -> RawOcrToolAdapter
  -> RawOcrEngine.recognize_from_bytes (default chi_sim+eng)
  -> pytesseract.image_to_string (NOT mocked)
  -> actual tesseract binary
  -> ToolResult (SUCCEEDED / RAW_OCR_OK / raw_text)
```

No language override. No `open(storage_ref)`. No legacy OCR HTTP routes.

## 6. Synthetic / non-PHI fixture policy

- In-memory Pillow rendering (`HELLO OCR 123`), ~1600x400 white canvas
- `ImageFont.load_default(size=96)` only; no font download; no committed PNG/JPG
- Artifact metadata: `sensitivity=INTERNAL`, `retention_class=ENGINEERING_SYNTHETIC`
- `storage_ref` uses `artifact://engineering-synthetic/...` only
- `PHI_FIXTURE_USED = NO`
- `PHI_REQUIRED = NO`

## 7. Real Tesseract anti-skip policy

```text
AIDOCTOR_REQUIRE_REAL_TESSERACT == "1"
```

| Environment | Missing binary or eng/chi_sim |
|---|---|
| Local / non-required | `pytest.skip` |
| Authoritative CI | `pytest.fail` |

Authoritative job must show actual execution: `N passed`, `0 skipped`.

## 8. Default Runtime non-registration

The same test file asserts:

```text
create_app().state.authorized_capability_ids
  contains engineering.synthetic.runtime_smoke
  does NOT contain engineering.ocr.raw
```

Required after this stage:

```text
DEFAULT_RUNTIME_CAPABILITY_SURFACE_UNCHANGED = YES
CANONICAL_RUNTIME_RAW_OCR_ENABLED = NO
```

## 9. CI strategy

New isolated job: `ocr-runtime-real-engine`

- `runs-on: ubuntu-24.04`
- Python 3.10
- apt: `tesseract-ocr`, `tesseract-ocr-eng`, `tesseract-ocr-chi-sim`
- pip: `ocr-service/requirements.txt` + `packages/model_runtime/requirements.txt`
  + `packages/python_runtime/requirements-http.txt` + `pytest`
- does **not** install `packages/model_runtime/requirements-ci.txt`
- env: `AIDOCTOR_REQUIRE_REAL_TESSERACT=1`
- `PYTHONPATH=../contracts/v1/bindings/python:..`
- command: `python -m pytest tests/test_raw_ocr_runtime_real_engine.py -q`

Historical jobs preserved:

- `ocr-separation`: mocked separation + mocked composition; ignores the new file;
  no apt Tesseract
- `ocr-real-engine`: 03B-C engine-only real Tesseract proof (unchanged purpose)

## 10. Evidence ceiling

Before exact-Head authoritative CI:

```text
INJECTED_RUNTIME_RAW_OCR_REAL_TESSERACT_COMPOSITION_VERIFIED = NO
CI_VERIFIED = PENDING_EXACT_HEAD_CI
```

Prior durable labels remain:

```text
TESSERACT_ENGINE_VERIFIED = YES
SYNTHETIC_IMAGE_REAL_TESSERACT_EXECUTION_VERIFIED = YES
RAW_OCR_DEFAULT_LANGUAGE_REAL_ENGINE_EXECUTION_VERIFIED = YES
RAW_OCR_ADAPTER_TEST_VERIFIED = YES
INJECTED_RUNTIME_RAW_OCR_COMPOSITION_VERIFIED = YES
  (historical mocked-Tesseract composition from 03B-B)
```

Must remain NO even after green CI:

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

CI apt Tesseract ≠ Docker image proof. Injected composition ≠ default registration.

## 11. Governance preservation

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
- adult_respiratory_v1 = DRAFT / PARTIALLY_VALIDATED / REQUIRES_CLINICAL_REVIEW / NOT_IMPLEMENTED

Never write: `A7 = CLOSED`

## 12. Stop state

```text
CI_VERIFIED = PENDING_EXACT_HEAD_CI
MERGE_AUTHORIZATION_GRANTED = NO
POSTFREEZE_03B_D_DURABLY_CLOSED = NO
```

F004: do not later create a doc-only Head solely to replace pending CI text.

Do not Ready / merge / start 03B-E / enable canonical Runtime raw OCR.
