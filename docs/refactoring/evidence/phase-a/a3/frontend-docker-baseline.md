# Phase A3 Frontend / Docker baseline

## Scope and status

- Execution date: `2026-07-31` (Asia/Shanghai).
- Base branch: `agent/enterprise-agent-refactoring-plan`.
- Base SHA at task start: `b3001c04b8dc2765e0bf794e91b362c84ad3376a`.
- Work branch: `agent/phase-a3-frontend-docker-baseline`.
- Work branch SHA at task start: `b3001c04b8dc2765e0bf794e91b362c84ad3376a`.
- Overall status: `PARTIALLY_VALIDATED`.
- Scope is inventory and validation only. No frontend, backend, API, clinical rule, dependency, lockfile, Docker, or Compose implementation was changed.

This baseline distinguishes dependency installation, lint, typecheck, test, build, development-server startup, Compose configuration, image build, container startup, and health checks. A successful earlier stage does not imply a later stage passed.

## Evidence levels

| Level | Meaning in A3 |
|---|---|
| `DOCUMENTED` | Stated only in documentation or package metadata. |
| `CODE_CONFIRMED` | Confirmed from tracked source or configuration. |
| `BUILD_VERIFIED` | A real frozen install or build lifecycle succeeded; no runtime/API claim is implied. |
| `RUNTIME_VERIFIED` | A controlled local runtime path was executed. The A3 frontend claim is limited to the Vite development servers listening on isolated loopback ports. |
| `DATA_VERIFIED` | Real data was inspected. A3 makes no such claim. |
| `TEST_VERIFIED` | Repeatable automated tests with effective assertions passed. A3 makes no such claim. |
| `UNKNOWN` | Evidence is insufficient. |
| `BLOCKED` | A concrete configuration, source, dependency, image, safety, or environment condition prevented deeper validation. |
| `PARTIALLY_VALIDATED` | Some stages were verified while material stages remain failed, blocked, or unknown. |

## Execution environment

- Host: Windows 11 / PowerShell, Asia/Shanghai.
- Node: `v22.14.0`.
- npm: `10.9.2`.
- pnpm: not installed.
- Yarn: not installed.
- Docker CLI: `28.1.1`.
- Docker Compose: `v2.35.1-desktop.1`.
- Docker daemon access required elevated execution. No container was created or started.
- Both frontend packages have npm lockfile v3 and no declared Node/npm engine requirement.
- Package manifests use version ranges; frozen installs resolved React `18.3.1`, Vite `5.4.21`, TypeScript `5.9.3`, Ant Design `5.29.3`, Axios `1.13.2`, Zustand `4.5.7`, ReactFlow `11.11.4`, SockJS `1.6.1`, and STOMP `7.2.1`.

## Discovery results

| Asset | Count | Evidence |
|---|---:|---|
| Frontend projects | 2 | `frontend/package.json`; `frontend-admin/package.json` |
| npm lockfiles | 2 | one lockfile v3 per project |
| Frontend test files/configurations | 0 | tracked-file scan; neither package defines a test script |
| ESLint configuration files | 0 | tracked-file scan |
| TypeScript configurations | 4 | root and node configs in both projects |
| Vite configurations | 2 | one per project |
| Dockerfiles | 12 | recursive tracked-file scan |
| Compose files | 2 | root and `monitoring/` |
| Compose services | 16 | 11 root services and 5 monitoring services |
| `.dockerignore` files | 0 | recursive tracked-file scan |
| nginx configurations | 0 | recursive tracked-file scan |

Five Dockerfiles are not referenced by either Compose file: clinical parsing, dialog, explanation, health-state assessment, and knowledge management. The frontends and execution-trace service have no Dockerfile and are not part of the root Compose topology.

## Commands and results

Commands were run from the named project directory unless otherwise stated. The first sandboxed npm install could not write npm's global log directory and returned npm's `Exit handler never called` error; the same frozen command was rerun with authorized host access and succeeded. That first result is an environment failure, not a repository install failure.

| Directory | Command | Exit | Result and evidence |
|---|---|---:|---|
| repo | `node --version` | 0 | `v22.14.0` |
| repo | `npm.cmd --version` | 0 | `10.9.2` |
| both frontends | `npm.cmd ci --ignore-scripts` | 0 | 358 packages installed per project; lockfiles unchanged; 22 audit findings per project: 1 low, 5 moderate, 15 high, 1 critical |
| `frontend` | `npm.cmd run lint` | 2 | FAIL: ESLint could not find a configuration file |
| `frontend` | `npm.cmd exec -- tsc --noEmit` | 2 | FAIL: 82 TypeScript errors; leading groups are missing/incompatible DTO properties and unused declarations |
| `frontend` | `npm.cmd run test` | 1 | FAIL: missing `test` script; test count 0 |
| `frontend` | `npm.cmd run build` | 2 | FAIL during `tsc`; Vite bundling was not entered |
| `frontend-admin` | `npm.cmd run lint` | 2 | FAIL: ESLint could not find a configuration file |
| `frontend-admin` | `npm.cmd exec -- tsc --noEmit` | 2 | FAIL: 12 TypeScript errors |
| `frontend-admin` | `npm.cmd run test` | 1 | FAIL: missing `test` script; test count 0 |
| `frontend-admin` | `npm.cmd run build` | 2 | FAIL during `tsc`; Vite bundling was not entered |
| both frontends | `npm.cmd run dev -- --host 127.0.0.1 --port 13000/13001 --strictPort` | wrapper 1; servers ready | Vite reported ready and both ports were observed listening. The first cleanup wrapper returned 1 while child Vite processes still held its redirected logs; the four exact npm/Vite processes were then identified by command line, stopped, and both ports confirmed released. No backend request or patient flow was executed. |
| repo | `docker compose -f docker-compose.yml config --quiet` | 0 | PASS with obsolete `version` warning; 11 services |
| repo | `docker compose -f monitoring/docker-compose.yml config --quiet` | 0 | PASS with obsolete `version` warning; 5 services |
| repo | `docker compose -f docker-compose.yml build` | 1 | FAIL before application layers: `openjdk:8-jdk-alpine` not found; configured Python registry mirror returned EOF while resolving `python:3.10-slim` |
| repo | `docker compose ... ps -a --format json` | 0 | no task container created or started |

Generated `node_modules`, any `dist` directory, temporary logs, and controlled Vite processes were removed after validation. Lockfile SHA256 values remained:

- `frontend/package-lock.json`: `F694E986E00F34D11C15A3A21796FC8C88632A41533A37947A494A30EAF5C616`
- `frontend-admin/package-lock.json`: `B217D286CD2CD68749B1D717222E6FFFD8AD855102A62CFC791B4E2CAD4FE3D8`

## Frontend project baseline

### `frontend`

- Purpose: patient-facing diagnosis/wellness prototype plus direct CDP visualization and a clinical-parsing test page.
- Routes actually wired by `src/App.tsx`: `/`, `/diagnosis`, `/cdp/:cdpId`, and `/test/clinical-parsing`.
- Vite default port: `3000`.
- Proxies: health `8081`, parsing `8082`, dialog `8088`, and general `/api` to diagnosis `8084`.
- Install: `BUILD_VERIFIED`.
- Lint, typecheck, test, and build: `BLOCKED`.
- Startup: `RUNTIME_VERIFIED` only for a local static Vite development server; API and business workflows remain `UNKNOWN`.

The 82 TypeScript errors directly demonstrate frontend/DTO drift. Examples include wellness fields used by components but absent from the selected type, question and answer shape mismatches, undeclared `timeout` status, raw result fields absent from `DiagnosisResult`, and invalid component properties. No source fix was made because this is a cross-cutting contract/build repair, not an unambiguous A3-only mechanical fix.

### `frontend-admin`

- Purpose: execution trace, knowledge graph schema, and book management prototype.
- Routes: `/trace`, `/knowledge/schema`, and `/books`.
- Vite default port: `3001`.
- Proxies: trace REST/WebSocket to `8093`; other `/api/v1` requests to knowledge management `8094`.
- Install: `BUILD_VERIFIED`.
- Lint, typecheck, test, and build: `BLOCKED`.
- Startup: `RUNTIME_VERIFIED` only for a local static Vite development server; admin API paths remain `UNKNOWN`.

There is no source-level role or authentication boundary in the admin clients. The admin app is a distinct project/surface, but this does not prove an enforced admin security boundary.

## API, streaming, state, and delivery findings

Detailed rows are in [frontend-api-contract-inventory.csv](frontend-api-contract-inventory.csv).

- REST: 21 source call points were inventoried across the two projects.
- Streaming: one SockJS/STOMP client subscribes to `/topic/trace/{cdpId}`. No SSE or native `EventSource` usage was found.
- The patient frontend uses one Axios client with optional Bearer token loaded from `localStorage`; no cookie or sessionStorage use was found.
- Admin Axios clients do not attach authentication in source.
- REST retry is absent. Timeouts are 60 seconds for the patient client and 30 seconds for admin clients. STOMP reconnect delay is 5 seconds.
- Type definitions are hand-written and mix camelCase, snake_case, `any`, index signatures, and service-specific response envelopes.
- `diagnosisApi.getResult` accepts `any`, logs raw response/result objects, constructs fallback diagnosis content and confidence defaults, and can return the unconverted response. This is a material PHI/internal-state and contract-drift risk.
- Patient result components display internal hypotheses, numeric confidence, raw evidence strings, must-exclude status, and advice. No `PatientDeliveryView`, citation DTO, patient field allowlist, delivery version snapshot, applicability, conflict, limitation, or clinician-review DTO was found.
- `/cdp/:cdpId` directly exposes a CDP visualization route containing internal reasoning/evidence/tool/version tabs, although several components are currently placeholders.
- The admin trace surface converts WebSocket `input` and `output` objects to JSON strings and types persistent trace `inputData`/`outputData`, so PHI filtering and role enforcement cannot be inferred.
- No dedicated clinician review page was found. A separate admin project exists, but it is knowledge/trace/book management rather than a clinician review surface.

## Docker and Compose baseline

Detailed rows are in [docker-compose-inventory.csv](docker-compose-inventory.csv).

- Root Compose configuration parses, but it defines only 11 services. It omits both frontends and several Dockerized Python services.
- `diagnosis-engine-service` references `MILVUS_HOST=milvus`, but no `milvus` service exists in either Compose file.
- All 16 Compose services lack a healthcheck. `depends_on` therefore establishes ordering only and must not be interpreted as readiness.
- The Java Dockerfiles require prebuilt `target/*.jar` files; neither target JAR existed in the clean worktree. Their base image `openjdk:8-jdk-alpine` also failed registry resolution.
- No application Dockerfile declares `USER`. The actual effective user of third-party images was not runtime-verified.
- No `.dockerignore` exists, so Python `COPY . .` contexts can include local artifacts or secrets if present at build time.
- Root Compose embeds development credentials for MySQL and Neo4j. Monitoring Compose embeds default Grafana admin credentials. These are configuration risks, not verified production secrets.
- Monitoring images use `latest`; root infrastructure tags such as `mysql:8.0`, `redis:7-alpine`, and `neo4j:5` are mutable rather than digest-pinned.
- `node_exporter` mounts host `/`, `/proc`, and `/sys` read-only. This is a high-risk host visibility pattern.
- Monitoring Grafana maps host port `3001`, which conflicts with the documented default port of `frontend-admin` when both are started without overrides.
- Current listener scan found none of the declared frontend/Compose ports occupied at the time of inspection.

## Secret and patient-data checks

- No tracked `.env`, private-key, PEM, or key file was found.
- High-confidence scans found no private key, AWS access key, GitHub token, JWT, or actual OpenAI-style key.
- Four apparent `sk-...` matches were inspected with values suppressed and were the same hyphenated bibliography phrase ending in “Task Force”, not credentials.
- Credential-like configuration locations use environment variable names, documented placeholders, or hard-coded development defaults. No real production secret was identified.
- No tracked frontend test fixture or patient sample file was found. A3 did not inspect production data and makes no `DATA_VERIFIED` claim.

## BLOCKED, UNKNOWN, and risks

### BLOCKED

- Both frontend lint stages: no ESLint configuration.
- Both frontend typecheck/build stages: 82 and 12 current TypeScript errors.
- Both frontend test stages: no runner, configuration, test files, or test script; test count 0.
- Root Compose image build: base-image and configured registry-mirror resolution failures.
- Full Compose startup/health: unsafe and not meaningful with hard-coded credentials, persistent data services, missing healthchecks, missing Milvus service, and unresolved images.

### UNKNOWN

- Patient diagnosis flow, backend API compatibility, auth enforcement, and error/degraded behavior.
- Whether completed status can be trusted when backend result conversion fails.
- Docker image build results after base-image correction and registry availability.
- Container runtime users, startup, health, inter-service readiness, database migrations, and persistent-data behavior.
- Production secrets, data, external providers, and real deployment topology.

### Risks

- Repository visibility is public: `REPOSITORY_VISIBILITY_RISK`.
- Dependency audit reports 22 findings per frontend, including one critical. No dependency was upgraded or auto-fixed in A3.
- Current result conversion and UI can expose internal structures, raw response data in browser logs, uncalibrated confidence, and non-citation evidence.
- Compose development credentials, mutable image tags, absent healthchecks, broad host mounts, and missing services prevent a production-readiness claim.

## Reproduction

```powershell
node --version
npm.cmd --version

Set-Location frontend
npm.cmd ci --ignore-scripts
npm.cmd run lint
npm.cmd exec -- tsc --noEmit
npm.cmd run test
npm.cmd run build

Set-Location ..\frontend-admin
npm.cmd ci --ignore-scripts
npm.cmd run lint
npm.cmd exec -- tsc --noEmit
npm.cmd run test
npm.cmd run build

Set-Location ..
docker compose -f docker-compose.yml config --quiet
docker compose -f monitoring/docker-compose.yml config --quiet
docker compose -f docker-compose.yml build
```

The controlled Vite commands used loopback ports `13000` and `13001`, then stopped the exact task processes and confirmed both ports were released. Full Compose startup is intentionally omitted from reproduction because the A3 safety preconditions were not satisfied.

## Out of scope and follow-up candidates

A3 did not implement frontend DTOs, PatientDelivery, Citation/Evidence UI, clinician review, Shared Contracts, backend/API changes, clinical rules, A4 data inventory, dependency upgrades, Docker architecture changes, or any production integration.

Separate follow-ups should address:

1. frontend build repair and a minimal test-runner decision;
2. shared contract design in A5 rather than ad hoc A3 DTO edits;
3. patient/admin/clinician authorization and safe delivery/logging boundaries;
4. Docker base-image correction, immutable tags, `.dockerignore`, non-root users, healthchecks, readiness, missing services, credentials, and safe local profiles;
5. dependency vulnerability triage without bundling upgrades into this baseline.
