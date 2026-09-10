# EB-REMEDIATE-A2-STARTUP-01 Evidence Report

> Dated: 2026-08-18
>
> Classification: `BOUNDED_STARTUP_REMEDIATION` / `NO CLINICAL CHANGE`
>
> Authorization token: `EB_REMEDIATE_A2_STARTUP_01_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Authorized Base: `39b0275eb201647b36168b13582bcf886dc7badd`
>
> Authorized Base tree: `34b39bceff839ea3c3757303771962f34ac970a6`
>
> Binding RESCOPE-EXIT: `RESCOPE_EXIT_ENGINEERING_REMEDIATION_REQUIRED`

```text
RUNTIME_VERIFIED_LOCAL != REPOSITORY_RUNTIME_VERIFIED
blocker remediation evidence != RESCOPE-EXIT PASS
!= ENGINEERING_FREEZE_ELIGIBLE
!= ENGINEERING_FROZEN_BASELINE
```

## 1. Purpose

Remediate exactly three confirmed Engineering Freeze startup blockers:

- `EB-A2-WORKUP`
- `EB-A2-DIAG-ENGINE`
- `EB-A2-OCR`

`EB-A2-DIALOG` remains excluded:
`EXPECTED_FAIL_CLOSED_CONTAINMENT` / `ACCEPTED_DURABLE_DEBT`.

This package does **not** update
`phase-a-engineering-baseline-gate-register.csv`.
The three gates remain durable blockers until independent review.

## 2. Changed production files

1. `workup-planner-service/app/api/routes.py`
2. `diagnosis-engine-service/requirements.txt`
3. `diagnosis-engine-service/app/api/routes.py`
4. `ocr-service/requirements.txt`

Evidence-only files:

5. `docs/refactoring/evidence/phase-a/engineering-baseline/eb-remediate-a2-startup-01/eb-remediate-a2-startup-report.md`
6. `docs/refactoring/evidence/phase-a/engineering-baseline/eb-remediate-a2-startup-01/eb-remediate-a2-startup-results.csv`

File count: **6**. No unexpected production files.

## 3. Workup

Before: `from app.utils.exceptions import ServiceException` while
`exceptions.py` defines only `WorkupPlannerException`.

Repository search: `ServiceException` appeared only on that unused
import. No other semantic use.

Chosen fix: remove the stale import only.

Clinical semantics changed: **NO**.

## 4. Diagnosis engine

Before: `requirements.txt` did not declare `jinja2`, but
`app/utils/prompt_templates.py` executes `from jinja2 import Template`.

Selected dependency: `jinja2==3.1.2`.

Why: the service README / implementation docs already named Jinja2
3.1.2; it is a conservative exact pin compatible with Python 3.10 and
the current FastAPI stack; no unrelated upgrades.

Secondary `LegacyLLMDisabledError` would have remained at
**module-import** time because routes eagerly constructed
`DiagnosisService()` and `get_fusion_engine()`.

Authorized shell change: lazy `get_diagnosis_service()` and keep
lazy `get_fusion_engine()`. Module-level LLM-bearing construction
removed. `ThreeLayerClassifier()` is not LLM-bearing and was left.

Invariant:

```text
HEALTH_SHELL_STARTS + LEGACY_LLM_REMAINS_FAIL_CLOSED
```

Clinical endpoints were not invoked. Prompt templates were not edited.
`common/aidoctor_llm/**` was not modified.

## 5. A7-NC unchanged proof

`common/aidoctor_llm` blobs before and after this batch:

| path | blob |
|---|---|
| `llm_client.py` | `48197f148562c5e7a5d8f05b60c6de2f79d38ffd` |
| `exceptions.py` | `584bfd2efc27cd9372fff50c6e79ccc73c0e5395` |
| `__init__.py` | `71f2655bfc5a9ddf7b168168a85d755018caaa1b` |
| `config.py` | `e7258684d383e818725a87172ee6c3f77719e6bc` |
| `prompt_manager.py` | `c341e5431fbfcf55b4c29c6528025d9372a726eb` |
| `requirements.txt` | `1590e9bb7aa788b9ac8a4fb98088bb47538080c3` |
| `setup.py` | `98593af836c64d0f4b2a66bad6f31195decc356b` |
| `README.md` | `b052017bac5f8ebcd578ecd9d0452609497846fc` |
| `tests/__init__.py` | `19ecb2b6bd0dcd29f0b5890776ac43b75088d5d1` |
| `tests/test_legacy_disabled.py` | `2628d6687fe9f517c7f95efa4da1d7105e67b274` |

Identical before and after. Constructor still raises
`LegacyLLMDisabledError`. Provider / network / credential / clinical
Prompt paths remain disabled.

## 6. OCR

Before: `opencv-python==4.8.1.78` with unconstrained numpy; historical
clean resolve selected NumPy 2.2.6 and failed ABI import.

Strategy A: keep OpenCV 4.8.1.78; pin `numpy==1.24.3` (already used by
`diagnosis-engine-service`). Isolated Python 3.10 install/import
proved: `cv2.__version__=4.8.1`, `numpy.__version__=1.24.3`.

OpenCV changed: **NO**. OCR algorithm changed: **NO**.

## 7. Isolated validation environment

Outside the repository:

`%TEMP%\AIdoctor-EB-A2-STARTUP-01\{workup,diag,ocr}`

Interpreter: `D:\anaconda\envs\aidoctor\python.exe` → venv
**Python 3.10.19**. Three separate venvs. No shared polluted env.
`PYTHONNOUSERSITE=1`. Provider-like environment variables cleared
for the validation processes.

Per service:

1. `python -m pip install -r requirements.txt` (diagnosis from its
   service directory so `-e ../common/aidoctor_llm` resolves)
2. `python -m pip check`
3. extra imports where required (`jinja2` / `cv2`+`numpy`)
4. `python -c "import app.main"`
5. `uvicorn app.main:app` on `127.0.0.1` ephemeral port
6. `GET /health`
7. terminate process

## 8. Results

See `eb-remediate-a2-startup-results.csv`.

| service | install | pip check | import | process | /health |
|---|---|---|---|---|---|
| workup-planner-service | PASS | PASS | PASS | STARTED | 200 |
| diagnosis-engine-service | PASS | PASS | PASS | STARTED | 200 |
| ocr-service | PASS | PASS | PASS | STARTED | 200 |

Resolved versions of interest:

- diagnosis: `jinja2==3.1.2`
- OCR: `opencv-python==4.8.1.78`, `numpy==1.24.3`

Boundary:

```text
provider_used=false
credentials_used=false
phi_used=false
clinical_endpoint_called=false
llm_enabled=false
```

## 9. Evidence classification

```text
DOCUMENTED: YES
CODE_CONFIRMED: YES
RUNTIME_VERIFIED_LOCAL: YES for the three authorized services
CI_VERIFIED: pending exact-head Phase A CI MVP after Draft PR
REPOSITORY_RUNTIME_VERIFIED: NO
CLINICAL_VALIDATED: NO
PRODUCTION_VERIFIED: NO
```

## 10. Remaining UNKNOWNs

- Production occupancy of these services
- Multi-service compose / image build
- Clinical / fusion / OCR report endpoint behavior (out of scope)
- Fresh independent RESCOPE-EXIT judgment
- Durable gate-register write-back (forbidden in this batch)

## 11. Non-claims

This batch does not claim RESCOPE-EXIT PASS, Engineering Freeze
eligibility, Engineering Baseline frozen, A7 COMPLETE, A11 PASS,
clinical validation, or production readiness.
