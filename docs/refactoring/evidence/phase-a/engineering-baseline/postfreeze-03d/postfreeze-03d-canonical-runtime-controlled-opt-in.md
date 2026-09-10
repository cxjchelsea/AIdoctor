# POSTFREEZE-03D Canonical Runtime Controlled Opt-In

Classification: BOUNDED NON_CLINICAL ENGINEERING IMPLEMENTATION

Stage: POSTFREEZE-03D

Authorized Base: `3a381c589aede50966ce5cd90c809be9f5b9ce1b`

Authorized Base tree: `77e17856ee9ecd2745cdb6c2d64dc6576ddb229f`

Predecessor: POSTFREEZE-03C DURABLY_CLOSED

## Evidence question

Can the durable Enterprise canonical Python Runtime (`create_app()` HTTP
composition) expose Raw OCR (`engineering.ocr.raw`) only when an explicit
server-side typed composition injects both registration and authorization,
proven by real localhost process HTTP with InputRef + sandbox ArtifactPort +
actual Tesseract + canonical request/output validation, while the default
`create_app()` / default uvicorn surface remains synthetic-only and Java /
legacy / production / clinical boundaries stay unchanged?

## Work packages

WP-1: Canonical typed controlled capability composition
(`controlled_composition.py`; `engineering_raw_ocr.py` thin delegate;
architecture guards).

WP-2: Existing engineering launcher uses the canonical factory; real
localhost process proof; one focused CI job.

## Exact 7-file scope

1. MODIFY `.github/workflows/ci.yml`
2. NEW `packages/python_runtime/http/controlled_composition.py`
3. MODIFY `packages/python_runtime/http/engineering_raw_ocr.py`
4. MODIFY `packages/python_runtime/tests/test_architecture_guards.py`
5. NEW `packages/python_runtime/tests/test_canonical_raw_ocr_controlled_opt_in.py`
6. MODIFY `engineering/raw_ocr_runtime_process.py`
7. NEW this document

## Target evidence flag

`CANONICAL_RUNTIME_RAW_OCR_CONTROLLED_OPT_IN_VERIFIED`

Authoring-time value: NO

Meaning after durable closure only: the canonical `create_app()` HTTP path can
expose `engineering.ocr.raw` under explicit server-side typed composition that
independently controls registration and authorization, with default Runtime
unchanged.

It does not mean default enabled, request-controlled enablement, Java
cutover, Production, clinical validation, or real-image OCR quality.

## Default-off semantics

`CANONICAL_RUNTIME_RAW_OCR_ENABLED` means ordinary no-argument `create_app()`
and `packages.python_runtime.http.app:app` activate Raw OCR by default.

Throughout 03D this remains NO.

`DEFAULT_RUNTIME_CAPABILITY_SURFACE_UNCHANGED` remains YES.

## Exclusions

Java/Python cutover: OUT
ToolCaller / AgentLoop wiring: OUT
legacy OCR migration: OUT
Production / Clinical Runtime / Phase B: OUT

## Authoring-time governance pointers (F004 preserved)

```
CANONICAL_RUNTIME_RAW_OCR_CONTROLLED_OPT_IN_VERIFIED = NO
CANONICAL_RUNTIME_RAW_OCR_ENABLED = NO
JAVA_PYTHON_CUTOVER_VERIFIED = NO
INDEPENDENT_REVIEW = NOT_PERFORMED
PMV = NOT_PERFORMED
MERGE_AUTHORIZATION_GRANTED = NO
POSTFREEZE_03D_DURABLY_CLOSED = NO
F004 = PRESERVED
CANONICAL_RUNTIME_RAW_OCR_CONTROLLED_OPT_IN_CI = PENDING_EXACT_HEAD_CI
```
