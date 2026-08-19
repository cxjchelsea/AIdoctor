# POSTFREEZE-03A RAW OCR Technical Separation Evidence

> Dated: 2026-08-19
>
> Classification: `BOUNDED NON-CLINICAL TECHNICAL SEPARATION` + `LEGACY BEHAVIOR PRESERVATION` + `ISOLATED OCR DEPENDENCY VERIFICATION` + `CI INTEGRATION` + `DRAFT PR`
>
> Batch: `POSTFREEZE-03A`
>
> Status: `IMPLEMENTED_PENDING_INDEPENDENT_REVIEW`
>
> Authorization token:
> `POSTFREEZE_03A_RAW_OCR_TECHNICAL_SEPARATION_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Authorized Base: `e4487d9af543864651543caec187fb0c062d18d9`
>
> Authorized Base tree: `5cd33f40b83779281027a2c121853532fb835696`
>
> Frozen engineering snapshot:
> `840a6fc7f83cec4fda6d53d739d7fd6b8013f031` /
> `d68d96d49eda96fe93dfb0c6c013b961a8e4b966`

```text
Creating RawOcrEngine
!=
Raw OCR HTTP route
!=
Runtime Tool Adapter
!=
canonical Runtime OCR capability
!=
Tesseract engine verified
!=
clinical extraction redesign
!=
PHI enablement
!=
Java OCR route repair
!=
Phase B
```

## 1. Authorization

- Token: `POSTFREEZE_03A_RAW_OCR_TECHNICAL_SEPARATION_IMPLEMENTATION_EXPLICIT_AUTHORIZATION_GRANTED`
- Repository: `cxjchelsea/AIdoctor`
- Base branch: `agent/enterprise-agent-refactoring-plan`
- Implementation branch: `agent/postfreeze-03a-raw-ocr-separation`
- Assessment verdict consumed: `POSTFREEZE_03_RAW_OCR_TECHNICAL_SEPARATION_REQUIRED_FIRST`
- POSTFREEZE-01: `DURABLY_CLOSED`
- POSTFREEZE-02: `DURABLY_CLOSED`
- PR #58: remains MERGED at `e4487d9af543864651543caec187fb0c062d18d9`
- Push CI `32207289544`: completed / success
- Drift: none

## 2. Purpose

Establish an explicit reusable RAW OCR engineering boundary inside `ocr-service`
without executing clinical extraction from that boundary, without changing
legacy OCR observable behavior, without adding a new HTTP endpoint, and
without connecting canonical Python Runtime.

## 3. Changed files

Exactly 6:

NEW:

1. `ocr-service/app/services/raw_ocr.py`
2. `ocr-service/tests/test_raw_ocr.py`
3. `ocr-service/tests/test_legacy_ocr_compat.py`
4. `docs/refactoring/evidence/phase-a/engineering-baseline/postfreeze-03a/postfreeze-03a-raw-ocr-separation.md`

MODIFY:

5. `ocr-service/app/services/ocr_service.py`
6. `.github/workflows/ci.yml`

Unexpected: 0

Not changed (explicitly required):

- `ocr-service/requirements.txt`
- `ocr-service/Dockerfile`
- `ocr-service/app/main.py`
- `ocr-service/app/api/routes.py`
- Java / Runtime / Contracts / Capability / Model Runtime

## 4. Raw OCR boundary

Public API in `ocr-service/app/services/raw_ocr.py`:

- `RawOcrEngine.preprocess_image(image: PIL.Image) -> PIL.Image`
- `RawOcrEngine.recognize_raw_text(image: PIL.Image, language=DEFAULT_OCR_LANGUAGE) -> str`
- `RawOcrEngine.recognize_from_bytes(image_bytes: bytes, language=DEFAULT_OCR_LANGUAGE) -> str`

Boundary roles must not be collapsed:

```text
preprocess_image
= reusable historical-equivalent preprocessing primitive;
direct preprocessing exception semantics preserved for legacy compatibility

recognize_raw_text
= raw text or typed Tesseract technical failure

recognize_from_bytes
= raw text or typed technical failure across decode + preprocessing + OCR
```

Input: `bytes` or `PIL.Image`. No FastAPI `UploadFile`. No `ContractEnvelope`.
No `ToolResult`. No Python Runtime.

`recognize_from_bytes` output: raw text string, or typed technical exception.
`preprocess_image` may still raise implementation-specific exceptions when
called directly; that is intentional legacy compatibility.

Dependencies owned by this module: PIL, pytesseract, cv2, numpy, stdlib.

Clinical imports: 0  
Runtime / contracts / model_runtime / capability / provider / LangGraph imports: 0

Default language remains `chi_sim+eng`. Preprocessing parameters remain:

- RGB → grayscale
- OTSU threshold
- `fastNlMeansDenoising(10, 7, 21)`
- `convertScaleAbs(alpha=1.5, beta=0)`

`recognize_raw_text` does not preprocess, so legacy `recognize()` cannot
double-process an image.

## 5. Technical failures

| Condition | `recognize_from_bytes` | `recognize_raw_text` | `preprocess_image` |
|---|---|---|---|
| Undecodable bytes | `RawOcrExecutionFailedError` | n/a | n/a |
| Preprocessing failure | `RawOcrExecutionFailedError` | n/a | implementation-specific (legacy) |
| Tesseract not found / binary missing | `RawOcrEngineUnavailableError` | same | n/a |
| Other Tesseract / engine exception | `RawOcrExecutionFailedError` | same | n/a |
| Successful OCR with no text | `""` | `""` | n/a |

These are distinguishable. `recognize_from_bytes` does not collapse engine or
preprocessing failure into `""`. `preprocess_image` is not itself a typed
recognition boundary.

## 6. Legacy compatibility strategy

`OcrService._preprocess_image` and `OcrService._ocr_recognize` delegate to
`RawOcrEngine`.

`OcrService._ocr_recognize` swallows RAW technical exceptions and returns `""`.

Legacy `recognize()` still does:

raw OCR → if text exists → `extract_structured_data()`

Historical empty result for engine failure is preserved:

```text
{"raw_text": "", "structured_data": {}}
```

Existing effective HTTP route remains `/api/v1/ocr/ocr/recognize`.
No new raw HTTP route.

## 7. Clinical semantic diff

Zero semantic change to:

- report templates
- indicator list / code / unit / `normal_range`
- status thresholds and labels
- report identification
- examination-date regex
- patient name / age / gender regex

Delta: 0

Compatibility tests may call historical extractors with synthetic engineering
text. That is not clinical validation.

## 8. PHI surface diff

- Legacy OCR remains historically PHI-capable.
- Patient parsing code: unchanged.
- `NEW_EXTERNAL_RAW_OCR_ROUTE`: 0
- `CANONICAL_RUNTIME_RAW_OCR_CAPABILITY`: 0
- `REAL_PHI_TEST_FIXTURES`: 0
- `PHI_USED_IN_TEST`: 0

A new in-process raw module can read arbitrary text from an image, but this
batch does not create a new externally reachable or canonical Runtime PHI path.

## 9. NumPy pin correction

POSTFREEZE-03 Assessment reported current `ocr-service/requirements.txt` as
unpinned / historically resolving to NumPy 2.x.

Independent implementation preflight observed that exact Enterprise Base
`e4487d9af543864651543caec187fb0c062d18d9` already contains:

```text
opencv-python==4.8.1.78
numpy==1.24.3
```

Therefore:

- `NUMPY_PIN_PRESENT`: YES
- `NUMPY_PIN`: 1.24.3
- `OPENCV_PIN`: 4.8.1.78
- Historical OpenCV / NumPy 2.x ABI failure remains `HISTORICAL_DEPENDENCY_FAILURE`
- This batch did not modify `requirements.txt`
- Current ABI/import status is determined by the isolated Python 3.10
  clean-install/import test below

Do not silently rewrite historical A2 / NC-CLOSE-05 documents.

## 10. Python 3.10 dependency / import evidence

Isolated clean venv from `D:\anaconda\envs\aidoctor\python.exe` (3.10.19):

```text
python -m pip install --upgrade pip
python -m pip install -r ocr-service/requirements.txt
```

Import smoke succeeded:

| Package | Version |
|---|---|
| Python | 3.10.19 |
| cv2 | 4.8.1 |
| numpy | 1.24.3 |
| PIL | 10.1.0 |
| pytesseract | 0.3.10 |

Tesseract OS binary was not installed or executed.

`OCR_PYTHON_3_12_VERIFIED`: not claimed. Existing Phase A Runtime 3.12 jobs
were not altered.

## 11. Tests

Local isolated Python 3.10:

```text
cd ocr-service
python -m pytest tests -q
13 passed
```

Coverage:

- preprocessing accepts a small synthetic PIL image
- successful engine call returns raw text
- successful empty text remains `""` and is not a technical failure
- `TesseractNotFoundError` → `RawOcrEngineUnavailableError`
- unexpected engine exception → `RawOcrExecutionFailedError`
- language default `chi_sim+eng` is deterministic
- raw module does not import or contain clinical extraction symbols
- legacy success still executes `extract_structured_data`
- legacy engine unavailable / execution failed still returns historical empty
  result and does not leak new exceptions
- clinical extractors remain callable with synthetic engineering text
- `recognize_from_bytes` wraps preprocess failure as `RawOcrExecutionFailedError`
  without changing `preprocess_image` direct-call exception semantics

Synthetic content: `HELLO OCR 123` in-memory only. No binary fixture file.
No medical report images. No real PHI.

## 12. CI

New job: `ocr-separation`

- `runs-on: ubuntu-latest`
- Python 3.10
- cache: `ocr-service/requirements.txt`
- install: pip upgrade + `ocr-service/requirements.txt` + `pytest`
- import smoke: cv2 / numpy / PIL / pytesseract
- `cd ocr-service && python -m pytest tests -q`
- no Tesseract apt package

Existing jobs remain semantically unchanged:

- contracts
- java
- python-safety
- frontend
- java-python-protocol

Author-time CI pointer: `PENDING_AT_AUTHORING`. Exact-head Draft PR CI must be
inspected independently; do not infer from local green.

## 13. Non-claims

| Label | Value |
|---|---|
| DOCUMENTED | YES |
| CODE_CONFIRMED | YES |
| TEST_VERIFIED | YES |
| CI_VERIFIED | PENDING_AT_AUTHORING |
| RAW_OCR_TECHNICAL_BOUNDARY_ESTABLISHED | YES |
| LEGACY_OCR_BEHAVIOR_PRESERVED | YES |
| OCR_DEPENDENCIES_INSTALLABLE_PY310 | YES (local isolated 3.10.19) |
| OCR_PYTHON_IMPORT_VERIFIED_PY310 | YES (local isolated 3.10.19) |
| OPENCV_NUMPY_IMPORT_COMPATIBILITY_VERIFIED | YES (local isolated 3.10.19) |
| RAW_OCR_HTTP_VERIFIED | NO |
| TESSERACT_ENGINE_VERIFIED | NO |
| REAL_IMAGE_OCR_VERIFIED | NO |
| REAL_TOOL_ADAPTER_TEST_VERIFIED | NO |
| CANONICAL_RUNTIME_RAW_OCR_ENABLED | NO |
| REPOSITORY_RUNTIME_VERIFIED | NO |
| JAVA_PYTHON_CUTOVER_VERIFIED | NO |
| CLINICAL_VALIDATED | NO |
| PRODUCTION_VERIFIED | NO |

## 14. Future POSTFREEZE-03B prerequisites

03B remains unauthorized. If later authorized, it must separately assess:

- contained HTTP vs in-process adapter
- whether a new raw HTTP route would create an external PHI-capable surface
- ContractEnvelope / fixture transport
- ToolPort allowlist for `engineering.ocr.raw` or equivalent
- Tesseract OS binary / language pack in CI
- fail-closed mapping that does not use the legacy empty-string collapse

03A does not implement any of those.

## 15. Explicit exclusions

new OCR HTTP routes = 0  
Runtime Tool Adapter = 0  
Runtime changes = 0  
Java changes = 0  
contracts changes = 0  
clinical rule changes = 0  
patient extraction changes = 0  
real PHI = 0  
provider = 0  
LangGraph = 0  
Phase B = 0
