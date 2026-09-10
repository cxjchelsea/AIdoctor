# NC-CLOSE-05 Runtime Evidence Coordination Report

> Dated: 2026-08-17
>
> Classification: `ACCOUNTING_PLUS_BOUNDED_RUNTIME_CHECKS` / `NO PRODUCTION REMEDIATION`
>
> Authorization token: `NC_CLOSE_05_RUNTIME_EVIDENCE_EXPLICIT_AUTHORIZATION_GRANTED`
>
> Exact Base: `8c95060b12f8c68a59c3e29e14d59a46e57cb888`
>
> Base tree: `b92ccd8854aed021e1f200d95f20bb3112771803`
>
> Execution ID: `NC-CLOSE-05-20260817-8C95060`

## 1. Purpose

Coordinate A1/A2/A3 runtime-evidence accounting on the exact Enterprise
Base. This package does not repair production code, install OTel, decide
01B stores, or claim A11 / Frozen Baseline.

```text
ACCOUNTING_COMPLETE != ALL_RUNTIME_PASS
ACCOUNTING_COMPLETE != REPOSITORY_RUNTIME_VERIFIED
```

## 2. Authorization and Base

Token: `NC_CLOSE_05_RUNTIME_EVIDENCE_EXPLICIT_AUTHORIZATION_GRANTED`  
Scope: `NC-CLOSE-RUNTIME-01` / `PARALLEL_BUT_PREEXISTING_AUTHORITY`  
PR #48: `MERGED_AND_VERIFIED`  
Enterprise push CI reused for covered surfaces: `32007012548` / success

## 3. Historical → current supersession

| Item | Historical | Current |
|---|---|---|
| A1 diagnosis compile FAIL (44 errors) | HISTORICAL_ONLY | SUPERSEDED_BY_LATER_VERIFIED_WORK (repair + CI) |
| A1 diagnosis package | BUILD_VERIFIED | CURRENT_BUT_STALE_HASH (not re-packaged) |
| A1 examination compile | BUILD_VERIFIED | CURRENT_VALID (reconfirmed) |
| A2 health-state BaseLLM import fail | HISTORICAL_ONLY | SUPERSEDED (import + /health 200) |
| A2 dialog DEBUG / OPENAI_API_KEY | HISTORICAL_ONLY | SUPERSEDED; new fail = `LegacyLLMDisabledError` |
| A2 diagnosis-engine pip ResolutionImpossible | HISTORICAL_ONLY | SUPERSEDED; new fail = missing `jinja2` |
| A2 explanation pydantic/pip fail | HISTORICAL_ONLY | SUPERSEDED (import + /health 200) |
| A2 workup `ServiceException` | FAILED | FAILED_CURRENTLY_RELEVANT |
| A2 OCR OpenCV/NumPy ABI | FAILED | FAILED_CURRENTLY_RELEVANT (numpy 2.2.6) |
| A2 clinical/treatment/risk/knowledge health | stale RUNTIME | CURRENT RUNTIME_VERIFIED_LOCAL |
| A3 frontend/admin tsc/build BLOCKED | HISTORICAL_ONLY | SUPERSEDED (PR #46 + CI build) |
| A3 compose config | CONFIG_VERIFIED | CURRENT_VALID |
| A3 image build FAIL / full Compose omit | HISTORICAL / safety | NOT_REQUIRED_FOR_FREEZE |

## 4. A1 status

- diagnosis compile/test: **CI_VERIFIED** via `32007012548` (44/0/0/0)
- diagnosis package: historical **BUILD_VERIFIED** only
- diagnosis process/HTTP: **not attempted** (optional; local workspace HEAD is not the authorized Base)
- examination: `mvn -B -ntp -f examination-service/pom.xml clean test` exit **0**; 12 sources; 0 tests → **BUILD_VERIFIED**

## 5. A2 status

Isolated Conda 3.10.19 venvs under `%TEMP%\AIdoctor-NC05-8c95060` (not committed).  
`provider_used=false` `patient_data=false` `clinical_content=false`.

| Service | Import | Process /health | Highest | Result |
|---|---|---|---|---|
| health-state-assessment | OK | 200 | RUNTIME_VERIFIED_LOCAL | PASS_VERIFIED |
| clinical-parsing | OK | 200 | RUNTIME_VERIFIED_LOCAL | PASS_VERIFIED |
| dialog | LegacyLLMDisabledError | n/a | DEPENDENCIES_INSTALLABLE | FAIL_CURRENT_IMPLEMENTATION |
| diagnosis-engine | missing jinja2 | n/a | DEPENDENCIES_INSTALLABLE | FAIL_CURRENT_IMPLEMENTATION |
| workup-planner | missing ServiceException | n/a | DEPENDENCIES_INSTALLABLE | FAIL_CURRENT_IMPLEMENTATION |
| treatment-engine | OK | 200 | RUNTIME_VERIFIED_LOCAL | PASS_VERIFIED |
| risk-assessment | OK | 200 | RUNTIME_VERIFIED_LOCAL | PASS_VERIFIED |
| explanation | OK | 200 | RUNTIME_VERIFIED_LOCAL | PASS_VERIFIED |
| OCR | OpenCV/NumPy ABI | n/a | DEPENDENCIES_INSTALLABLE | FAIL_CURRENT_IMPLEMENTATION |
| knowledge-management | OK | 200 | RUNTIME_VERIFIED_LOCAL | PASS_VERIFIED |

## 6. A3 status

- frontend / frontend-admin: **CI_VERIFIED** (`tsc && vite build` on 32007012548). Not browser/E2E/full-stack.
- `docker compose -f docker-compose.yml config --quiet`: exit 0 → **CONFIG_VERIFIED**
- `docker compose -f monitoring/docker-compose.yml config --quiet`: exit 0 → **CONFIG_VERIFIED**
- image rebuild / full Compose / browser: **NOT_REQUIRED_FOR_FREEZE**

## 7. Executed commands

```text
mvn -B -ntp -f examination-service/pom.xml clean test          exit 0
docker compose -f docker-compose.yml config --quiet            exit 0
docker compose -f monitoring/docker-compose.yml config --quiet exit 0
isolated pip install + import app.main                        per service
uvicorn 127.0.0.1:<ephemeral> /health                         6 services
```

CI reuse (not re-run locally): `32007012548` java + frontend jobs.

## 8. Current implementation findings (not package defects)

1. `workup-planner-service` `app.api.routes` imports missing `ServiceException` — STARTUP_REMEDIATION — **not authorized**
2. `diagnosis-engine-service` imports undeclared `jinja2` — DEPENDENCY_REMEDIATION — **not authorized**
3. `dialog-service` constructs `LangChainLLMClient` at import; facade raises `LegacyLLMDisabledError` — STARTUP_REMEDIATION — **not authorized**
4. `ocr-service` OpenCV 4.8.1.78 vs resolved NumPy 2.2.6 — DEPENDENCY_REMEDIATION — **not authorized**

## 9. Remaining UNKNOWNs

- diagnosis-service process/HTTP on this Base (optional, not required)
- Docker image build current result (not required)
- browser / full-stack (not required)
- production occupancy of any service

## 10. A11 startup ambiguity

```text
A11_RUNTIME_GATE_AMBIGUOUS
```

This batch uses the bounded interpretation: required targets are
**accounted** by verified startup **or** current FAIL/BLOCKED/NOT_REQUIRED.
It does **not** mean every service starts, and it does **not** close A11
Freeze Review.

## 11. NC-CLOSE-05 accounting status

```text
RUNTIME_EVIDENCE_ACCOUNTING_COMPLETE
```

All required register rows have a terminal classification.  
Four current implementation fails are recorded, not repaired.

## 12. Unauthorized-work check

Production Java/Python/Docker/CI/contracts/config/deps: **0**  
Provider / PHI / clinical fixtures / 01B / OTel / Phase B: **0**  
NC-CLOSE-04 not reopened.

## 13. Next gate

Combined Independent Review + Merge Review of the exact Draft Head.  
Do not mark Ready from authoring. Do not remediate in this package.
