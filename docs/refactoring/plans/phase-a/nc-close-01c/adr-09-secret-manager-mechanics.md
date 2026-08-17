# ADR-09 Secret Manager Mechanics / Secret Access Boundary

| Field | Value |
|---|---|
| ADR ID | ADR-09 |
| Title | Secret Manager Mechanics / Secret Access Boundary |
| Status | `PROPOSED_DECIDED_PENDING_REVIEW` |
| Exact Base | `e9f4baa791009ae16b6cc956a31bcd4bbd428213` |
| Parent batch | `NC-CLOSE-01` |
| Refined batch | `NC-CLOSE-01C` |
| Decision depth | `BOUNDARY_DECISION_REQUIRED` |
| Disposition | `BOUNDARY_FROZEN_CONCRETE_BACKEND_DEFERRED` |
| implementation_authorized | `false` |

## 1. Decision scope

Freeze secret-access **ownership**, **consumer boundary**, **local/dev
semantics**, and **missing-secret behavior**.

This ADR does not select a Secret Manager vendor, migrate credentials,
enable providers, or implement rotation.

## 2. Context

Target architecture documents name “Secret Manager” as a future platform
concern. Current services retrieve configuration and credential-like
keys through environment variables, Spring YAML, optional Nacos
discovery, Docker/Compose literals, and gitignored local files.

`packages/model_runtime` and the disabled `common/aidoctor_llm` facade
already forbid runtime secret/provider access. That guard is not a
Secret Manager.

## 3. Current repository evidence

| Evidence | Classification | Path / note |
|---|---|---|
| Python `os.getenv` / `BaseSettings` / dotenv | `CODE_CONFIRMED` | multiple `*/app/config/settings.py` |
| Provider key **names** (`OPENAI_API_KEY`, `CHATGLM_API_KEY`, `CUSTOM_API_KEY`) | `CODE_CONFIRMED` | settings modules; values not reproduced |
| Spring `@Value` / `Environment.getProperty` | `CODE_CONFIRMED` | Feign URLs, datasource, trace URLs |
| Docker/Compose infrastructure key names | `CODE_CONFIRMED` | `docker-compose.yml`, `monitoring/docker-compose.yml` — example/dev classification |
| Nacos present as optional discovery, not a secret store | `CODE_CONFIRMED` | Compose + `diagnosis-service` dependency |
| No Secret Manager / Vault / cloud SM client | `CODE_CONFIRMED` | repository-wide absence |
| Model Runtime forbids env secret lookup | `CODE_CONFIRMED` / `TEST_VERIFIED` | `packages/model_runtime/tests/test_provider_security.py` |
| Legacy LLM facade fail-closed | `CODE_CONFIRMED` | `common/aidoctor_llm` |
| Real production credentials in tracked files | not identified | scan classified Compose/YAML defaults as **example** |
| Gitignored `.env` / `application-dev.yml` contents | `UNKNOWN` | not inspected |
| Secret Manager design intent | `DOCUMENTED` | `docs/refactoring/数据与基础设施迁移.md`, prompt/runtime design |

No secret **values** are recorded in this ADR.

## 4. Problem

If business and runtime code remain the long-term secret owners, later
provider enablement will embed credentials in env sprawl, Compose files,
and service settings. Phase A can freeze the abstraction without
choosing a vendor or authorizing real credentials.

## 5. Architectural invariants

1. Future **platform-level secret access** owns retrieval.
2. Direct environment / YAML / Compose lookup is **CURRENT**, not target
   production authority.
3. Concrete vendor is **DEFERRED**.
4. Local/dev may use explicit non-production placeholders only.
5. Required production/provider secret missing → **FAIL_CLOSED** for
   that capability.
6. Optional technical telemetry destination missing → disable exporter;
   do not fake clinical or state success.
7. Real provider credentials remain `FORBIDDEN` / `NOT_AUTHORIZED`.

## 6. Alternatives considered

| ID | Alternative |
|---|---|
| A9-A | Select AWS / Azure / GCP / Vault / Kubernetes Secrets now |
| A9-B | Keep env/YAML as permanent production secret architecture |
| A9-C | **Selected.** Freeze platform secret-access boundary; defer concrete backend |
| A9-D | Implement Secret Manager in this package |

## 7. Selected decision

**A9-C — Secret-access boundary frozen; concrete Secret Manager backend
deferred.**

### 7.1 Ownership

**TARGET** owner of secret retrieval: a future platform-level secret
access abstraction under Platform / Security governance.

Business, domain, and runtime components must not treat
`os.getenv` / Spring property / Compose literal lookup as the target
production secret architecture.

### 7.2 Current mechanism

**CURRENT** (legacy, still present):

- environment variables
- Spring configuration
- Nacos / configuration-center wiring where used for discovery
- Docker / local-dev configuration
- gitignored `.env` / profile YAML

This is a transitional access path, not target authority.

### 7.3 Consumer boundary

| Consumer class | Target rule |
|---|---|
| Infrastructure (DB, Redis, Neo4j, Nacos admin) | retrieve via platform secret access when production-managed |
| Provider runtime (LLM / model vendor keys) | retrieve via platform secret access; never embed; A7-NC remains complete and un-reopened |
| Application integration (service URLs) | non-secret endpoints may remain ordinary config; credential-bearing URLs follow secret access |
| Clinical system | no dedicated clinical secret class is created here; clinical enablement remains `NOT_AUTHORIZED` |
| Model Runtime | remains forbidden from runtime secret/provider access |

### 7.4 Local / development semantics

Local/dev **MAY** use explicitly non-production:

- environment variables
- placeholder configuration
- test fixtures

No real provider credentials are authorized by this ADR.

### 7.5 Production missing-secret semantics

If a **required** production or provider secret is unavailable:

```text
FAIL_CLOSED
for the capability that requires that secret
```

Forbidden substitutions:

- dummy credential
- fallback provider
- embedded key
- anonymous production mode

### 7.6 Optional observability secret

If an optional technical telemetry destination is disabled or
unavailable, the technical exporter **MAY** remain disabled.

This is observability degradation only. It must not become:

- a fake-success path for clinical work
- a substitute for State Committer durability
- a waiver of ComplianceAudit or ClinicalDecisionRecord writes

### 7.7 Secret values

This ADR discusses only key **names**, ownership, classes, and access
mechanics. It does not contain secret values.

## 8. Concrete backend deferral

```text
Concrete backend: DEFERRED
Disposition: BOUNDARY_FROZEN_CONCRETE_BACKEND_DEFERRED
```

Explicitly **not** selected now:

- AWS Secrets Manager
- Azure Key Vault
- GCP Secret Manager
- HashiCorp Vault
- Kubernetes-specific Secrets implementation
- any equivalent hosted secret store

| Deferral field | Value |
|---|---|
| Rationale | No production secret-backend evidence or requirement is sufficient to choose a vendor now. |
| Future owner | Platform / Security (or equivalent platform governance owner) |
| Trigger | First authorized production secret/provider integration, or a production deployment gate that requires managed secrets |
| Evidence requirement | Integration test of the chosen abstraction; credential-leakage checks; rotation/access evidence appropriate to the selected backend |
| Stop condition | Real provider credential work before separate authorization → `PHASE_A_NC_CLOSURE_PROVIDER_BOUNDARY_REACHED` |

## 9. Rationale

The repository already shows scattered CURRENT access and a designed
future Secret Manager. Freezing ownership and fail-closed semantics
prevents env sprawl from becoming the production architecture without
forcing an unjustified vendor choice.

## 10. Rejected alternatives

- **A9-A** selects a vendor without production evidence.
- **A9-B** freezes CURRENT env/YAML as target authority.
- **A9-D** is implementation and is not authorized.

## 11. Tradeoffs

Deferral leaves CURRENT sprawl in place until a later authorized
implementation. That is acceptable because this ADR forbids treating
that sprawl as the target.

## 12. Compatibility requirements

- A7-NC Model Runtime provider/routing/gateway/fallback semantics
  unchanged.
- Shared Contracts v1 unchanged.
- ADR-02 / ADR-03 / ADR-04 / ADR-05 unchanged.

## 13. Failure semantics

| Class | Missing required secret | Optional telemetry secret |
|---|---|---|
| Technical observability | N/A unless that exporter is required | disable exporter; fail-open for ordinary business |
| Provider / production capability | `FAIL_CLOSED` | N/A |
| State Committer / clinical / audit | not defined here; not fail-open-by-telemetry | separate authorities |

## 14. Security / privacy boundary

- No real credentials authorized.
- No credential migration authorized.
- No secret values in docs or code in this package.

## 15. Implementation consequences

Later authorized work may introduce a secret-access abstraction and,
only under a later vendor decision, a concrete backend. This ADR does
not add them.

## 16. Deferred items

Vendor, rotation implementation, production secret inventory of
gitignored files, and credential migration.

## 17. Explicit non-goals / forbidden claims

Do **not** read this ADR as:

- Secret Manager implemented
- credentials migrated
- production secrets secured
- provider enabled
- rotation implemented
- Vault / AWS / GCP / Azure selected

## 18. Architecture stop conditions

```text
PHASE_A_NC_CLOSURE_PROVIDER_BOUNDARY_REACHED
```

if real provider credentials, paid API, or network inference are
required to complete or apply this decision.

## 19. Evidence truth

| Claim | Label |
|---|---|
| CURRENT access patterns | `CODE_CONFIRMED` |
| Secret Manager implemented | **NO** |
| Production secrets secured | **NO** |
| Runtime secret retrieval verified | not `RUNTIME_VERIFIED` |
| Gitignored local secret files | `UNKNOWN` |

## 20. Relationships

- Soft input to later production/provider gates.
- Soft/platform input to NC-CLOSE-04 unless managed secrets are
  specifically required.
- Does not decide 01B storage vendors.

## 21. Decision owner / governance state

Owner: Phase A NC Closure / NC-CLOSE-01C.
State: `PROPOSED_DECIDED_PENDING_REVIEW`.
Not `APPROVED` / `MERGED` / `IMPLEMENTED` / `RUNTIME_VERIFIED`.

## 22. Next gate

Combined Independent Review + Merge Review of the NC-CLOSE-01C Draft PR.
Implementation remains `NOT_AUTHORIZED`.
