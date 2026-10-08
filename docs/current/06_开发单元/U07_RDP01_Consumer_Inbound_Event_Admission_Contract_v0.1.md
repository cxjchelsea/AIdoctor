# U07-RDP-01 Consumer Inbound / Event Admission Contract v0.1

> Unit: U07 — 用户回答 Resume 与幂等恢复
> Readiness finding: B-U07-RG-01
> Date: 2026-10-08
> Parent initial readiness review: PR #263 remediation head `a3b4215a49855910ce15cba73bb0e60db780db39`
> Parent targeted independent re-review: PR #265 review head `0728bbcf50e8a8103697f5b1e88f845d6a1caa29` (PASS / NO_BLOCKER)
> U07 Unit Spec reviewed design: `9c899fdbe2136d88ed1d3bf5c2dd9b6d2b702272`, independent review PR #262
> Runtime integration reference: `main@6d4fd787600e3a57f01f3e17893e6d98893ac546`
> Scope: **CONTRACT / PHYSICAL DESIGN CANDIDATE — INDEPENDENT REVIEW REQUIRED**
> Current status: **SECOND_TARGETED_REMEDIATION_CANDIDATE / PENDING_EXACT_HEAD_INDEPENDENT_RE_REVIEW**
> Independent targeted re-review PR #268 @ `2ee68ca1f15467ea942915c8ae8dcb05637c8a22`: BF-01 CLOSED; BF-02 and BF-03 OPEN. This document only proposes their second remediation.
> Independent review PR #267 @ `0a8f75f54a148ebd17243ebc1db529d9ce4d9d6e`: REVISE_REQUIRED / three blockers. This revision does not independently close them.
>
> Does NOT authorize implementation, merge, production, real patient traffic, external I/O, PROFILE-A, PHI, direct F1 activation, or U07 Runtime resume.

---

## 1. 目的、边界与接受条件

本文件只冻结用户回答到达 U07 时的：调用方与执行范围验证、业务事件规范化、幂等身份、回答负载绑定、U06 等待资格引用复核、并发准入与后续 F8 输入。**不**在这里决定 F8 的 ACCEPTED / DUPLICATE / EXPIRED / REJECTED；不恢复 Checkpoint、不将回答提升为 Clinical Fact、不写 Clinical State。

```text
Transport inbound
  -> profile / caller / scope / schema barrier
  -> canonical identity + immutable answer binding
  -> durable CanonicalBusinessEventLedger resolution
  -> authoritative snapshot references / currentness assessment
  -> U07AdmissionEnvelope (or typed admission block)
  -> F8 Business Resume Decision (RDP-02)
  -> P02 Runtime resume (RDP-04, only after F8 ACCEPTED)
```

必须保持：
- `Business Resume validity != Runtime Resume compatibility`.
- `Accepted answer != Clinical Fact`; U02 形成事实。
- U06 保有送达/等待事实所有权；F3 保有 Question/Gap 生命周期所有权；U15 保有取消/过期事实；F8 决定业务恢复合法性；P02 决定 Runtime 可恢复性。
- event ID 不等于副作用权限；canonical ledger 不等于 F8 决策账本；Trace 不等于业务状态。
- 本 RDP 的设计关闭仅指 reviewed contract/physical design；实现和 executable evidence 在授权后另行进行。

## 2. 现有仓库证据与兼容性约束

当前 main 实际存在：

| 文件 | 已观察能力 | RDP-01 严格边界 |
|---|---|---|
| `runtime/foundation/CanonicalBusinessEventLedger.java` | `resolveOrCreate(eventId, consultationId, eventType, idempotencyKey, payloadDigest)`; event_id 与 idempotency_key 检索、唯一键并发冲突处理 | 只保存 identity/correlation，不决定业务结果 |
| `runtime/foundation/CanonicalBusinessEventRecord.java` | 持久字段 `event_id`, `consultation_id`, `event_type`, `idempotency_key`, `payload_digest`, `received_at` | 没有 Question / parent-effect / scope 的原生列；需边界 binding record 或受审迁移，不能声称基础表已经完整 |
| `runtime/foundation/CanonicalBusinessEventRepository.java` | `findByIdempotencyKey`；基础 event_id PK | 保留现有唯一约束、并发 replay 行为 |
| `runtime/u06/wait/U07ResumeEligibilityProjector.java` | `u07elig` 基于 parent wait effect + checkpoint 生成标识，要求传入 `threadAwaitingUser` | 投影 ID 是线索，不是新一轮实时资格认证 |
| `runtime/foundation/RuntimeWaitCheckpointRecord.java` | 记录 consultation / thread / run / question / pending ref / parent wait / clinical version / historical dependency 等 | 仅供 provenance 关联；恢复/兼容性属于 RDP-04 |
| `runtime/foundation/RuntimeThreadStateRecord.java` | ACTIVE / WAIT_CHECKPOINTED / AWAITING_USER，versioned row | 不把状态检查变成 F8 决策；缺失 Checkpoint 不等于业务拒绝 |

已有 U06-RDP-04 对 synthetic profile、`DELIVERED_TO_USER`、`WAITING_USER`、`AWAITING_USER` 和 parent effect 的约束继续生效。U07 不能将未确认送达变成有效 Question。

## 3. Frozen event types and intent

```text
USER_ANSWER
  = 提交不可变回答内容（answer_payload_ref + digest）供 F8 业务判断；
    唯一允许引入新回答负载的 U07 事件类型。

RESUME_REQUEST
  = 对已经存在的 canonical USER_ANSWER 事件请求继续/查询/恢复处理；
    MUST 引用原始 canonical_answer_event_id；
    MUST NOT 携带新的答案正文、生成新 answer effect，或绕过 F8。
```

`RESUME_REQUEST` 自身可以有独立 canonical transport event 记录，但它的 `target_answer_event_id` 必须不可变地指向原始 USER_ANSWER，且只能复用该回答及原始业务决策/处理进度。目标不存在时，返回 typed unresolved reference（不制造回答）；目标存在但未获 F8 ACCEPTED 时，不启动 Runtime。**原事件仍在 VALIDATING/未完成时仅重新附着/查询，不启动竞争性第二条流程。**

这是一项 RDP-01 的**提议性冻结设计**，必须在 independent review 验证与 Phase 8 `K02` / Phase 9 现有规则无冲突；如冲突需 controlled amendment，不得直接以本候选覆盖冻结规范。

## 4. Logical input schemas

### 4.1 U07IngressEventV1

字段名为新设计的逻辑契约，非声称现有 Java DTO 已实现。

| 字段 | USER_ANSWER | RESUME_REQUEST | 规则 |
|---|---|---|---|
| `contract_version` | REQUIRED | REQUIRED | 例如 `u07.inbound.v1`，不匹配 fail closed |
| `event_type` | REQUIRED | REQUIRED | 只允许两种枚举 |
| `business_event_id` | REQUIRED | REQUIRED | client / trusted ingress 一次业务动作生成，重试保持不变；非空，长度受限 |
| `transport_attempt_id` | OPTIONAL | OPTIONAL | 每次传输可变化；永不参与业务 effect ID |
| `idempotency_key` | REQUIRED | REQUIRED | 同一业务提交/恢复请求跨重试不变；不能由重试时随机生成 |
| `consultation_id` | REQUIRED | REQUIRED | 与认证作用域和权威 Consultation 一致 |
| `question_id` | REQUIRED | REQUIRED | 同当前或历史受保护 pending Question 绑定 |
| `pending_question_ref` | REQUIRED | REQUIRED | 明确绑定 Pending Question |
| `parent_wait_effect_id` | REQUIRED | REQUIRED | U06 delivered-wait 业务效应引用 |
| `resume_eligibility_id` | REQUIRED | REQUIRED | 必须独立解析当前/可重附着资格 |
| `thread_id`, `run_id` | REQUIRED | REQUIRED | 只作关联，不作为恢复许可 |
| `checkpoint_id` | OPTIONAL | OPTIONAL | 缺失不会自动业务 REJECTED；进入 RDP-04 修复能力评估 |
| `expected_clinical_state_version` | REQUIRED | REQUIRED | 必须记录提交时绑定值；currentness 决策留给 F8/P01 |
| `occurred_at` | REQUIRED | REQUIRED | RFC3339/UTC offset-aware；服务端单独记录 received_at |
| `answer_payload_ref` | REQUIRED | FORBIDDEN | 受控加密存储引用，不得用 raw text 替代 trace |
| `answer_payload_digest` | REQUIRED | FORBIDDEN | 规范化负载的带版本指纹，禁止直接泄露内容 |
| `target_answer_event_id` | FORBIDDEN | REQUIRED | 精确原始 canonical USER_ANSWER ID |
| `scope_authorization_ref` | REQUIRED | REQUIRED | 非生产 synthetic scope 证明 |
| `actor_binding_ref` | REQUIRED | REQUIRED | 授权患者/渠道/会话主体，不信任客户端自报主体 |
| `correlation_id`, `trace_parent_ref` | REQUIRED | REQUIRED | 可审计；不作为合法性来源 |

辅助 `U07AuthenticatedIngressContext` 必须由可信 adapter 注入：principal/scope, environment, profile, tenant/consultation boundary, trusted channel, granted actions, authenticated_at, policy_ref。**客户端提交的 actor、scope、role 不构成授权证据。**

### 4.2 Canonical binding side-record

现有基础 Event Ledger 缺失物理绑定字段。建议新增独立的 `U07CanonicalEventBinding` 持久记录（或经独立审查的最小表迁移）；至少包括：

```text
canonical_event_id (FK to foundation canonical event)
contract_version
consultation_id / event_type
originating_business_event_id (first persisted canonical event_id only) / storage_idempotency_key
question_id / pending_question_ref
parent_wait_effect_id / resume_eligibility_id
thread_id / run_id / checkpoint_id?
expected_clinical_state_version
answer_payload_ref? / answer_payload_digest?
target_answer_event_id?
actor_binding_ref / scope_authorization_ref
bound_profile / environment / channel
created_at / received_at
binding_fingerprint (U07-owned; excludes request alias id, transport attempt and received time)
binding_schema_version
trace_ref
```

约束：one canonical event -> one immutable binding; 绑定字段发生冲突时 fail-closed；基础 Event Ledger 与 side-record 的同一事务/可修复原子写入必须在物理设计核查中进一步证明。持久化失败不能进入 F8 或对外形成成功确认。

### 4.3 Frozen Foundation compatibility (remediation BF-U07-RDP01-IR-02)

**Chosen V1: Preserve Foundation `payload_digest` semantic as the fingerprint of the canonical immutable event payload. Do NOT redefine or migrate this column into an entire binding digest.** A separate U07-owned `binding_fingerprint` checks Question, wait provenance, scope and other protected context. Foundation `sameCanonicalInput` remains unchanged.

Canonical *event payload* digest uses versioned `SHA-256` of immutable canonical event payload fields:
- `USER_ANSWER`: event type, answer_payload_ref, trusted answer_payload_digest, canonical payload schema version.
- `RESUME_REQUEST`: event type, target_answer_event_id, canonical payload schema version; **no new answer body**.
- Neither payload digest nor binding digest contains incoming alias business_event_id, transport_attempt_id or received_at.

`CanonicalBusinessEventRecord.payload_digest` stays a 64-character lower-case SHA-256 hexadecimal value, well inside `VARCHAR(128)`. The `U07CanonicalEventBinding.binding_fingerprint` is a *distinct* 64-character digest described in §5. No Foundation DB column or semantic change is part of this RDP design.

**Storage choices are frozen:** one relational database transaction with the existing canonical_business_event row and one `u07_canonical_event_binding` row; `canonical_event_id` is its unique PK and FK. U07 V1 does **not** adopt cross-database `CANONICAL_BINDING_PENDING` as an alternate successful path. Outer U07 admission coordinator invokes the Foundation ledger and writes the side-binding under the **same physical transaction**. No F8 handoff/success response until both writes have committed. If the two repositories are not demonstrably on one transaction manager/data source at implementation readiness, this design is NOT_APPLICABLE and requires a reviewed physical amendment (not an implicit best-effort fallback).

**Race/rollback rule:** `CanonicalBusinessEventLedger.resolveOrCreate` is `@Transactional` and catches `DataIntegrityViolationException`. Under some JPA/SQL database semantics a failed flush/unique-key violation marks the transaction rollback-only, so the in-transaction `findBy...` fallback cannot be assumed to work. U07's outer coordinator must treat a transaction marked rollback-only, any persist exception, or missing side-binding as **failure with no admission**; after rollback, a separately scoped **new transaction** may re-read the committed winner, check the complete binding and retry idempotently. No use of a potentially poisoned transaction to finalize admission. This is a **design obligation**, not proof the current Foundation catch path is safe. Exact implementation approach (transaction interceptor placement/new transaction retry and DB isolation) requires physical review and authorized integration tests.

**Global storage idempotency key** (≤128 ASCII chars) is frozen as:
```text
storage_idempotency_key = "u07-" + lowercase_hex(SHA256(frame(
  "u07-storage-key-v1",
  trusted_environment_id,
  trusted_profile_id,
  trusted_tenant_scope_id,
  trusted_consultation_id,
  trusted_actor_scope_id,
  event_type,
  ingress_supplied_stable_idempotency_token
)))
```
Every frame item uses UTF-8 length prefix + NFC normalized exact value; no concatenation ambiguities. Key is 68 ASCII chars; caller-supplied token is a stable **opaque** token per logical event, not an auto-generated retry ID. Same trusted scope/token regenerates the same key; different tenant/consultation/profile/type has a different namespace. Hash collision or a pre-existing row with incompatible scoped binding => `IDENTITY_CONFLICT` (never silently allocate a new key). Consent and trusted actor must be validated before derivation.

### 4.3.1 Explicit Foundation consumer / migration impact inventory (BF-U07-RDP01-IR-02)

Inventory reference: non-truncated recursive `main@6d4fd787600e3a57f01f3e17893e6d98893ac546` tree (2,634 entries). Exact reviewed file contents are recorded below; this is **a bounded, path-/class-based impact inventory**, not a false claim of full-text search across all 2,634 files. At physical implementation readiness the repository-wide `resolveOrCreate(`, `canonical_business_event`, `payload_digest`, `idempotency_key` reference scan must be executed with an auditable manifest and SHA, and all additional consumer hits must be reconciled. **That pending exhaustive scan is an explicit readiness prerequisite.**

| Inventory category | Exact observed baseline | Compatibility conclusion / required later verification |
|---|---|---|
| Foundation Service | `diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/foundation/CanonicalBusinessEventLedger.java` | `resolveOrCreate(eventId,consultationId,eventType,idempotencyKey,payloadDigest)`; compares existing immutable tuple; behavior retained; `@Transactional` race recovery needs DB-backed verification |
| Foundation entity/repository | `.../foundation/CanonicalBusinessEventRecord.java`, `CanonicalBusinessEventRepository.java` | Event ID primary key and **globally** unique idempotency key; `payload_digest` opaque string, 128 chars; U07-specific 64-hex does not constrain other event types |
| Existing Foundation unit test | `diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/foundation/FoundationRuntimeBaseTest.java` | Uses example `sha256:abc` and `sha256:different`; **do not** introduce Foundation-wide regex `^[0-9a-f]{64}$` or change old fixture expectations |
| MySQL foundation migration | `diagnosis-service/src/main/resources/db/migration/V2__create_clinical_runtime_foundation.sql` | Existing `canonical_business_event`: `event_id VARCHAR(128) PK`, `idempotency_key VARCHAR(128) UNIQUE`, `payload_digest VARCHAR(128)` |
| Oracle foundation migration | `diagnosis-service/src/main/resources/db/migration-oracle/V3__create_clinical_runtime_foundation.sql` | Same logical columns / unique constraints with `VARCHAR2`, Oracle naming |
| U06 wait schema | MySQL `db/migration/V6__add_u06_wait_runtime.sql`; Oracle `db/migration-oracle/V6__add_u06_wait_runtime.sql` | Durable wait/checkpoint/trace provenance already modeled; **no existing U07 canonical side-binding or U06 issuance table evidenced** |
| U06 eligibility execution | `runtime/u06/wait/U06WaitCoordinator.java`, `U07ResumeEligibilityProjector.java` | Eligibility returned, not independently persisted by these two classes; see BF-03 and upstream amendment |
| Potential additional consumers | All non-inventoried callers, deployment migration scripts, fixture/provisioners and other modules | `NOT_EXHAUSTIVELY_SEARCHED`; must pass hash-pinned full-source impact scan at RDP-05/aggregate before any implementation authorization; new hits trigger exact design compatibility review |

**Frozen compatibility decision:** preserve original canonical table/schema and existing `resolveOrCreate` call signature. U07 uses only a **new U07-owned** side-binding schema and per-U07 digest/key rules. The chosen V1 design is compatible *at the directly inspected interfaces and migrations*, but **repository-wide compatibility closure remains a separate evidenced readiness check**.

### 4.3.2 Concrete dual-dialect binding migration / physical schema

Append-only new migration files (do not rewrite executed V2/V3/V6):
- MySQL: `diagnosis-service/src/main/resources/db/migration/V7__add_u07_canonical_event_binding.sql`
- Oracle: `diagnosis-service/src/main/resources/db/migration-oracle/V7__add_u07_canonical_event_binding.sql`

Names are **planned migration paths**, not existing files. Each must create the following equivalent logical schema (abbreviated design DDL; actual scripts require syntax/DB validation):

```sql
-- MySQL V7 DDL proposal
CREATE TABLE u07_canonical_event_binding (
  canonical_event_id VARCHAR(128) NOT NULL,
  contract_version VARCHAR(64) NOT NULL,
  storage_idempotency_key VARCHAR(128) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  consultation_id VARCHAR(128) NOT NULL,
  question_id VARCHAR(128) NOT NULL,
  parent_wait_effect_id VARCHAR(128) NOT NULL,
  binding_fingerprint CHAR(64) NOT NULL,
  payload_digest VARCHAR(128) NOT NULL,
  binding_payload_ref VARCHAR(256) NOT NULL,
  protected_binding_ref VARCHAR(256) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  PRIMARY KEY (canonical_event_id),
  CONSTRAINT fk_u07_binding_canonical_event
    FOREIGN KEY (canonical_event_id) REFERENCES canonical_business_event(event_id),
  INDEX idx_u07_binding_wait (consultation_id,parent_wait_effect_id,question_id)
);
-- Oracle: VARCHAR2(n CHAR), CHAR(64 CHAR), TIMESTAMP,
-- CONSTRAINT pk_u07_binding PRIMARY KEY (canonical_event_id),
-- CONSTRAINT fk_u07_binding_event FOREIGN KEY (canonical_event_id)
-- REFERENCES canonical_business_event(event_id),
-- CREATE INDEX idx_u07_binding_wait ON u07_canonical_event_binding(...)
```

`protected_binding_ref` points to an immutable, access-controlled U07 binding payload holding **all §5.1 fields**; unlike a truncated table, the full binding must be durably verifiable. `binding_payload_ref` is the controlled immutable event payload reference. Both references must be created in the **same authorized synthetic state/store boundary**, and must resolve immutably before commit; no untrusted URL/raw PHI. Database storage of the full binding as a canonical byte/blob column in this same transaction is the **V1 selected option** if ref-store durability cannot be atomically established, rather than a two-store half-commit. A future large-object implementation must demonstrate atomicity before authorized implementation.

Dialect obligations: Oracle table/constraint names within configured identifier limits; existing global unique key remains only in Foundation; no new global uniqueness on `question_id` (different canonical candidates go to F8/RDP-03); index for query, not business verdict; immutable binding row after commit (no UPDATE that rewrites fingerprint/payload), enforce at repository/service privilege and audit; FK means no orphan side binding; any missing binding after a supposedly committed canonical event is quarantined and never forwarded to F8.

### 4.3.3 Concrete transaction / retry-as-new-transaction topology

```text
U07InboundAdapter
 -> preledger authorized scope/payload validation (read-only, fail closed)
 -> U07AdmissionTransactionCoordinator @Transactional(REQUIRED)
      -> CanonicalBusinessEventLedger.resolveOrCreate (joins SAME transaction)
      -> upsert only-if-absent U07CanonicalEventBinding + exact immutable equality
      -> flush BOTH, check rollbackOnly == false
    -> transaction COMMIT
 -> after commit: fresh authoritative business-state snapshot
 -> only then enqueue/submit admission context to F8 RDP-02

exception / integrity constraint violation / rollbackOnly:
 -> no F8, no success response
 -> rollback transaction entirely
 -> U07CanonicalWinnerReconciler @Transactional(REQUIRES_NEW)
      -> re-read Foundation winner by event ID / global scoped key
      -> validate whole side binding and trusted scope
      -> distinguish (winner+binding) valid replay from
         (winner absent) safe new attempt or (winner without binding) QUARANTINE
 -> if retry allowed, new transaction; else fail-closed
```

`REQUIRES_NEW` must cross an actual transactional proxy boundary; self-invocation is forbidden. `save` without `flush` or `saveAndFlush` is not proof a uniqueness violation has surfaced. `CanonicalBusinessEventLedger` catches `DataIntegrityViolationException` within its own `@Transactional`; U07 must not mistake its catch-and-lookup behavior for proof that a database transaction remained usable. Verify MySQL and Oracle separately for auto-flush, rollback-only, isolation and winner-query visibility in authorized database integration tests; this design review does not mark tests PASS.

Transaction-outcome matrix:

| State | Admission response | Durable condition |
|---|---|---|
| Both rows committed | `ADMITTED_FOR_F8` or exact reattach after owner check | same scoped key and entire U07 immutable binding |
| Ledger insert flushed, binding insert failed | fail closed | both rolled back; never success |
| Unique race caused rollback-only | fail closed; optional post-rollback separate read/retry | no F8 from poisoned transaction |
| Existing winner and full binding equal | reattach original ID | no new event/effect |
| Existing winner, side-binding missing | quarantine | no accepted new binding fabricated from retry |
| Existing winner, different full binding | identity conflict | no overwrite or alternative new key fallback |
| Database unavailable / commit unknown | indeterminate; reconcile by exact identity in new transaction | never optimistic ACCEPTED |

**Compatibility condition at authorization:** if either MySQL or Oracle schema/transaction topology cannot satisfy these requirements, return `NOT_READY` and require an independently reviewed controlled amendment; do not silently switch to eventual consistency.

### 4.4 Side-binding atomicity

`U07CanonicalEventBinding` stores **originating** `canonical_event_id` only, never overwrites this with retry aliases. Immutable fields may be checked against the incoming request's immutable payload/business binding, but may not require incoming alias ID equal original canonical ID. If an attempted alias needs an audit trail, it lives in a separate append-only `U07CanonicalEventAliasAudit` keyed by (canonical_event_id, request_alias_id, attempt correlation); this audit is not a second business event, and audit failure cannot authorize new effects.

Binding consistency requires checking:
1. Foundation owner row's consultation_id, event_type, **global storage idempotency key**, payload_digest.
2. Binding row's corresponding canonical_event_id plus full `binding_fingerprint` equality.
3. All scope/trust references and payload-ref integrity against verified trusted context; no unverified client-supplied scope.

Any missing binding row after a committed Foundation record is a **quarantine / no F8 / no admission** condition; repair must be limited to uniquely reproducible, independently verified original binding evidence. Never infer missing protected metadata merely from the current client retry.

## 5. Identity, fingerprint and canonicalization

### 5.0 Identity authority table (remediation BF-U07-RDP01-IR-01)

| Name | Meaning | Immutable persisted? | Fingerprint input? |
|---|---|---|---|
| `business_event_id` on *first submitted request* | Original event ID, becomes Foundation `canonical_event_id` if it wins insertion | Yes, as original canonical ID | **NO**; address/reference only |
| `business_event_id` on later request with same scoped key | Incoming request/transport **alias** (never a second canonical business identity) | Optional append-only alias-audit entry; not the canonical binding | **NO** |
| `transport_attempt_id`, `correlation_id`, `trace_parent_ref` | Transport/observability | Audit only | **NO** |
| `ingress_supplied_stable_idempotency_token` | Same logical operation across retries | Stored as protected key identity; no raw token in logs | Key derivation input, **not** binding fingerprint |
| `storage_idempotency_key` | Globally namespaced Foundation idempotency key in §4.3 | Yes | **NO** (compared directly) |
| `canonical_event_id` | Foundation's original winning `event_id` | Yes | **NO** (FK, compared directly) |
| `payload_digest` | SHA-256 of immutable **event payload** in §4.3 | Yes | **YES**, as a single input |
| `binding_fingerprint` | Digest of immutable U07 business/owner bindings listed below | Yes | N/A |

Same storage key with different incoming ID maps to the old canonical winner **only if** Foundation fields and full U07 binding compare equal. The alias is never written into the original `U07CanonicalEventBinding.originating_business_event_id`. If an incoming alias ID is already a separate Foundation PK with different key, the event-ID lookup takes precedence and leads to `IDENTITY_CONFLICT`, not an alternate reattachment.

### 5.1 Exact canonical binding fingerprint: `U07-EVENT-BINDING-FP-V1`

Normalize values using Unicode NFC and field-specific strict parsers; stable binary framing `frame(name, length, UTF8(value))` in the exact field order below; prefix with `u07-event-binding-v1`. Digest is lower-case hexadecimal SHA-256. **Never normalize away any answer text semantics** (answer body is not part of this frame; only trusted opaque digest/ref). Exact included fields, in order:

1. `contract_version`
2. `event_type`
3. `trusted_environment_id`
4. `trusted_profile_id`
5. `trusted_tenant_scope_id`
6. `trusted_consultation_id`
7. `trusted_actor_scope_id`
8. `question_id`
9. `pending_question_ref`
10. `parent_wait_effect_id`
11. `resume_eligibility_id`
12. `thread_id`
13. `run_id`
14. `checkpoint_id` (nullable encoded as **typed NULL**, not empty string)
15. `expected_clinical_state_version` (canonical decimal)
16. `answer_payload_ref` (USER_ANSWER only; typed NULL otherwise)
17. `answer_payload_digest` (USER_ANSWER only; typed NULL otherwise)
18. `target_answer_event_id` (RESUME_REQUEST only; typed NULL otherwise)
19. `scope_authorization_ref`
20. `actor_binding_ref`
21. `payload_digest` from Foundation

Explicitly **EXCLUDED**: incoming `business_event_id` whether original/alias, `canonical_event_id`, stable idempotency token, derived storage idempotency key, `transport_attempt_id`, request arrival/received timestamp, `occurred_at`, trace/correlation data, attempt count, snapshot read/check times. `occurred_at` remains an immutable event-level business assertion **for the original canonical event**, stored separately once; retries with different `occurred_at` are not allowed to rewrite history and are handled as request-identity conflict **only when proven that they claim to be the same original event**. Alias retries do not change or overwrite original occurred_at, even if network timestamp differs. This distinction preserves permitted same-key aliases without hidden timestamp false conflicts.

**Comparison algorithm**: `canonical incoming payload` -> compute Foundation payload_digest -> derive storage key -> find/create Foundation winner -> immutable side-binding fingerprint check -> either `REATTACHED_TO_CANONICAL` or `IDENTITY_CONFLICT`. Side binding comparison uses fields (1)–(21), **never compares request alias to original ID**.

Deterministic identity test vectors (symbolic frame expected relations pending code tests):

| Vector | Canonical event ID | Global key | Binding data | Expected payload/binding equality | Admission |
|---|---|---|---|---|---|
| V1 | A | K | B | baseline | new canonical A |
| V2 | A | K | B | equal to V1 | reattach A |
| V3 | **alias B** | K | B | **equal to V1** | reattach A; B only alias audit |
| V4 | alias B | K | changed Question | binding unequal | IDENTITY_CONFLICT |
| V5 | alias B | K | changed answer digest | payload+binding unequal | IDENTITY_CONFLICT |
| V6 | alias B | K | changed tenant/profile/consultation | global namespace key different; old requested key cannot cross | BLOCKED_AUTHORIZATION or IDENTITY_CONFLICT |
| V7 | C | new K2 | same answer and Question as A | distinct canonical event | F8 determines duplicate/other verdict; one-winner apply RDP-03 |
| V8 | C | K | same payload but different provenance | binding unequal | IDENTITY_CONFLICT |

Numeric hash vectors require a reviewed reference fixture and exact byte serialization implementation in RDP-06; the authority relation and field order are frozen here at design level.

### 5.2 Replay rules

| 输入情况 | Canonical resolution | Admission behavior | F8 authority |
|---|---|---|---|
| 同 `event_id`、同受保护绑定、同 key | 查回同一 canonical event | `REATTACHED`，无新 event/effect | 复用/待处理原 verdict |
| 新 `event_id`、同 key、同受保护绑定 | Foundation 查同 key，返回 canonical winner | `REATTACHED`，记录 attempt 关联 | 不新建独立 decision |
| 同 ID 或 key，任何受保护字段改变 | `IDENTITY_CONFLICT` | 阻断、审计、零副作用 | 不调用 |
| **新 ID + 新 key + 相同语义回答、同一 Question/wait** | 基础 ledger 可生成新 canonical event；不得假称 foundation 已去重 | `F8_REVIEW_REQUIRED` 且同 wait 维度竞争协调 | RDP-02 判 DUPLICATE / REJECTED / independent，RDP-03 保证至多一个 apply |
| 同答案但不同合法 Question/wait | 独立规范事件 | 继续权威状态校验 | F8 决定；不能全局答案去重 |
| `RESUME_REQUEST` 指向原 USER_ANSWER，同绑定 | 该 resume-request 可单独规范化，但始终 resolve target | `TARGET_REATTACHED` | 沿用目标事件的业务决策/进度 |
| `RESUME_REQUEST` 不存在/不匹配 target | 不制造新的 answer 事件 | `UNRESOLVED_TARGET` / `BINDING_CONFLICT` | 不调用 |
| 已有同 Question 已 APPLIED 的不同回答 | 不允许直接再次恢复 | `F8_REVIEW_REQUIRED`，检查 authoritative wait | RDP-02/03 负责裁决与 zero second effect |

**禁止**把 `F8_REVIEW_REQUIRED` 当成 F8 的 ACCEPTED。回答内容的“语义等价”必须由 RDP-02 的可复现政策明确；此处仅以**规范负载指纹相同**作为候选提示，不让 LLM 或字符串包含规则擅自决定临床有效性。

## 6. Admission gates, precedence and typed result

有两层不同的过滤，禁止混合：

### 6.1 Pre-ledger security/schema barrier (no ledger write)

顺序：
1. `PROFILE_SCOPE`：环境与 PROFILE-B synthetic scope 是否受已批准 authorization 限定；真实 recipient/PHI/production/live 一律阻断。
2. `CALLER_AUTHORITY`：trusted principal 与 consultation/actor/channel/action 是否有权限；跨租户、跨 Consultation 禁止。
3. `SCHEMA`：version, event enum, field presence, lengths, payload exclusivity, clock format。
4. `PAYLOAD_INTEGRITY`：answer ref 可访问、完整性与 digest/size/version 一致；从不信任客户端自报 digest；敏感正文不记审计日志。
5. `REPLAY_PROTECTED_IDENTITY`：校验已有 ID/key 的绑定一致性；不允许重用不同事件语义。

前置拒绝只产生有限拒绝审计（禁止 PHI），**不产生被接受的规范业务事件**。若提交途中部分事实持久化，必须使用补偿/恢复规则，不得直接继续到 F8。

### 6.2 Post-ledger authoritative business-context snapshot

从权威状态 owner 读取（并记录版本与来源）：
- Consultation current lifecycle/version；
- current Pending Question / Question ID、F3 authoritative `DELIVERED_TO_USER`；
- delivery confirmation 与 `QUESTION_DELIVERED_WAIT_EFFECT_ID` provenance；
- wait eligibility 当前/可重附着、thread/run/checkpoint refs；
- U15 terminal/cancel/expiry 与 Question supersession；
- source Clinical State Version、historical P06 binding refs。

输出 `U07AdmissionContextAssessment`：
```text
admission_context_status:
  VERIFIED_CURRENT
  VERIFIED_HISTORICAL_REATTACHABLE
  BUSINESS_CONTEXT_MISMATCH_CANDIDATE
  RUNTIME_RECONCILIATION_REQUIRED
  TEMPORARILY_UNAVAILABLE
  INVALID_PROVENANCE
source_snapshot_refs[]
source_versions[]
checked_at
validation_reason_codes[]
```

语义：
- `VERIFIED_CURRENT` 为 F8 决策提供候选条件，但不代替 F8 的最终 currentness / expiry / duplicate precedence。
- `VERIFIED_HISTORICAL_REATTACHABLE` 只能在可信持久历史与当前 owner 一致、仍合法时使用；**不得靠单个 hash 伪造**。
- `BUSINESS_CONTEXT_MISMATCH_CANDIDATE` 交 F8 根据冻结政策判 EXPIRED/REJECTED/DUPLICATE，RDP-01 不越权决定。
- `RUNTIME_RECONCILIATION_REQUIRED` 不自动产生 Business REJECTED；Runtime checkpoint 兼容性仍属 RDP-04。
- `TEMPORARILY_UNAVAILABLE` / `INVALID_PROVENANCE` fail-closed 不执行副作用；前者可重试、后者进入安全审计/正式裁决，不能当成事实不存在。

### 6.2.1 Authoritative wait provenance and historical reattachment (remediation BF-U07-RDP01-IR-03)

A projected U06 `resume_eligibility_id` is **never self-authenticating**. RDP-01 must assemble a read-only, versioned evidence set before labelling business wait provenance verified:

| Required evidence | Authoritative owner/source | Must match |
|---|---|---|
| Question delivered state and delivery confirmation | F3/U06 committed Question state and U06 confirmed delivery record | `Question=DELIVERED_TO_USER`, same delivery identity, immutable selection/ref |
| Current Pending Question | P01/G2 committed Clinical State | same Question/pending ref/consultation and committed state version |
| Consultation WAITING_USER | governed Consultation lifecycle owner | consultation ID, lifecycle version, not terminal/active unexpectedly |
| Delivered-wait parent effect | U06 durable parent wait effect ledger + child commit refs | same parent effect, delivery and Consultation WAITING child, trace lineage |
| U15 cancel/expire/supersede authority | U15 / lifecycle/F3 authoritative terminal record | no incompatible authoritative terminal effect and no superseding wait |
| Original eligibility issuance | U06 durable eligibility output/trace/reference **with authoritative origin binding** | original eligibility ID, parent effect, original checkpoint ID, thread/run |
| Clinical/historical bindings | P01 Clinical State version + P06 historical scope/binding refs | same waiting interaction and historically compatible bound governance |
| Thread/checkpoint evidence | P02 Runtime state/Checkpoint store | **diagnostic only** for runtime readiness; not business-validity authority |

Snapshot contract:
```text
U07AuthoritativeWaitEvidence {
  consultation_id, question_id, pending_question_ref, parent_wait_effect_id,
  delivery_confirmation_ref, question_state_ref + version,
  clinical_state_ref + version, consultation_lifecycle_ref + version,
  u15_terminal_status_ref + version,
  u06_parent_effect_ref + child_commits,
  original_eligibility_issuance_ref, historical_bound_context_ref,
  runtime_thread_ref?, runtime_checkpoint_ref?, snapshot_read_epoch,
  evidence_status, trace_refs[]
}
```

All business owner reads must be consistent at a versioned logical snapshot or else return `DEFERRED_AUTHORITY_UNAVAILABLE`; never combine incompatible owner versions into a fabricated valid wait. F8 must revalidate owner versions; RDP-03 P01/apply uses CAS/fencing and rechecks U15 terminalization before effect commit. Observation-only RDP-01 does not mutate U06/F3/U15 facts.

**Outcome/handoff table**:

| Business evidence | Original U06 eligibility | Runtime evidence | RDP-01 disposition | Next |
|---|---|---|---|---|
| all committed delivered-wait facts current, no terminal effect | original issuance verified | AWAITING_USER + checkpoint | VERIFIED_CURRENT + ADMITTED_FOR_F8 | F8 decides business result; accepted only then P02 |
| same authoritative live business wait + stable original eligibility | verified original issuance | checkpoint now missing/stale | VERIFIED_HISTORICAL_REATTACHABLE + RUNTIME_RECONCILIATION_REQUIRED hint | F8 first, then P02 reconstruct or governed failure; no automatic REJECTED |
| same business wait established, **Thread never reached AWAITING_USER / no issuance** | **absent** | incomplete wait | DEFERRED_AUTHORITY_UNAVAILABLE / RECONCILIATION_REQUIRED | only U06/P02 authorized wait repair can later issue eligibility; U07 **cannot** invent historical eligibility |
| delivery not confirmed or parent child commits missing | any | any | BLOCKED_PROVENANCE | zero F8-resume/effects; consult U06 owner |
| Question superseded / Consultation terminal / U15 expired | any | any | BUSINESS_CONTEXT_MISMATCH_CANDIDATE with owner reason | F8 RDP-02 evaluates historical DUPLICATE / EXPIRED / REJECTED without new effects |
| owner state temporarily inaccessible / versions non-coherent | any | any | DEFERRED_AUTHORITY_UNAVAILABLE | retry fresh snapshot, zero effects |
| eligibility hash supplied, no authoritative issuance/effect chain | unverified | even compatible checkpoint | BLOCKED_PROVENANCE | no F8/resume |
| historical bindings differ and approved migration absent | original exists | runtime uncertain | F8 source refs preserved; runtime compatibility **not** approved by RDP-01 | RDP-04 determines rehydrate/INCOMPATIBLE, no silent latest release |

The `VERIFIED_HISTORICAL_REATTACHABLE` status is **not** a way to admit a revoked/terminated wait and does not claim checkpoint can be recovered; it only records that the *same lawful wait* still has affirmative business owner evidence and verifiable original eligibility. Missing checkpoint alone cannot turn business ACCEPTED into REJECTED, while absence of positive authoritative business evidence is not permission.

### 6.2.2 Physical provenance reconciliation / required U06 controlled amendment (BF-U07-RDP01-IR-03)

**Existing physically queryable facts on main (confirmed in code + V6 migrations):**

| Artifact | Query key / persistent fields | What it does / does not prove |
|---|---|---|
| `clinical_consultation_wait_effect` (V6 MySQL/Oracle), `ConsultationWaitEffectRepository` | `parent_delivered_wait_effect_id` UNIQUE, `wait_effect_id`, `consultation_id`, `question_id`, `delivery_id`, `effect_status=COMMITTED`, `committed_row_version` | Proves authoritative Consultation WAITING_USER child effect if still current; not issuance |
| `clinical_runtime_thread_state` / `RuntimeThreadStateRepository` | `thread_id`, `consultation_id` UNIQUE, `current_run_id`, `current_wait_checkpoint_id`, `current_wait_effect_id`, `runtime_status`, `row_version` | If `AWAITING_USER` + same wait refs, proves persisted thread entry; no historical transition log after state overwritten |
| `clinical_runtime_wait_checkpoint` / `RuntimeWaitCheckpointRepository` | `checkpoint_id` PK, `question_delivered_wait_effect_id` UNIQUE, `thread_id`, `run_id`, `question_id`, `delivery_confirmation_ref`, `consultation_wait_effect_id`, `clinical_state_version`, `dependency_binding_ref` | Proves stored checkpoint relation **when record exists**; may disappear/stale and is not itself business truth |
| `u06_governed_execution_trace` | `trace_id` PK, `consultation_id`, `lifecycle_status`, `outcome_status` | Operational trace alone does **not** prove eligibility issuance; no `eligibility_id` column |
| `U06WaitCoordinator.establish` and `U07ResumeEligibilityProjector.project` | in-memory `Result.eligibilityId` = `U06Ids.hash("u07elig", parent_wait_effect_id, checkpoint_id, "1")` after `enterAwaitingUser` | Deterministic ID *may be recomputed*, but neither class persists an immutable issuance statement |

**Decision:** do not claim existing main implements a queryable eligibility issuance authority. BF-03 is resolved at design level by **explicitly registering REQUIRED upstream controlled amendment `CA-U06-U07-ELIG-ISSUANCE-01`**, a prerequisite to positive U07 implementation readiness. U07 RDP-01 does **not** implement or invent this U06 authority.

Required new U06-owned durable record/query contract (logical, review candidate):
```text
U06EligibilityIssuanceV1 {
  eligibility_id (PK),
  parent_wait_effect_id (UNIQUE per active wait),
  consultation_id, question_id, pending_question_ref,
  checkpoint_id, thread_id, run_id,
  committed_consultation_wait_effect_id, consultation_row_version,
  original_question_delivery_confirmation_ref,
  u06_wait_parent_effect_ref,
  original_clinical_state_version, historical_binding_ref,
  thread_awaiting_user_row_version,
  issuance_contract_version = "u06-u07-elig-issuance-v1",
  issuer_authority_ref = U06,
  issued_at, immutable_fingerprint
}
lookup(parent_wait_effect_id, eligibility_id) -> VERIFIED_ISSUED | MISSING | CONFLICT
```

Issuance write is permitted **only after** the U06-controlled `enterAwaitingUser` transaction has durably committed, with the exact same committed wait provenance and row version. Retried issuance is idempotent on parent effect and exact fingerprint; any ambiguous commit is reconciled from the authoritative source. The U06 amendment must freeze its own issuance transaction/outbox/recovery protocol for the crash between thread AWAITING_USER and issuance, and its MySQL/Oracle schema. U07 never writes this record or backdates an issuance.

**Missing/stale Checkpoint policy (explicit):**
- If `U06EligibilityIssuanceV1` is durable, and business owner Question/Consultation/PendingQuestion/U15 facts all remain valid, U07 may mark `VERIFIED_HISTORICAL_REATTACHABLE` **even if the runtime checkpoint record has disappeared**, based on historically issued immutable refs; the checkpoint compatibility/rehydration remains P02 RDP-04.
- If original issuance is **MISSING**, even if current Thread says `AWAITING_USER` and `U06Ids.hash` can be recomputed, U07 returns `DEFERRED_AUTHORITY_UNAVAILABLE` or `BLOCKED_PROVENANCE` with a U06 owner repair referral. U07 cannot fabricate issuance by reading the current request.
- If original issuance exists but current waiting state has become superseded/cancelled/expired, route to F8 with authoritative terminal evidence; no ordinary resume effect.
- If issuance was never created because `enterAwaitingUser` never committed, no ordinary U07 Resume occurs. The U06 amendment must decide and record repair/terminal behavior in its own owner boundary.

**Required independent design gate before implementation authorization:** `CA-U06-U07-ELIG-ISSUANCE-01` detailed design + owner-equivalence check + exact-head independent review + controlled approval + verified MySQL/Oracle compatibility + U06 regression impact review. Until those artifacts exist, U07 RDP-01 may be accepted as a **conditional design contract**, but U07 overall Implementation Readiness remains NOT_READY and the historical positive resume branch is **NOT_PHYSICALLY_AVAILABLE**. This conditional readiness dependency must be carried into RDP-05 and aggregate readiness, not silently waived.

New negative test cases:
- `T12C`: checkpoint deleted, valid **durable issuance** + current business wait -> U07 business provenance verified, P02 repair needed; no fake Business REJECTED.
- `T12D`: same inputs except issuance record missing -> no ordinary U07 admission; hash recomputation cannot replace issuance.
- `T12E`: U06 Thread reached AWAITING_USER but crashed before issuance durability -> U06 owner repair required; U07 does not mint eligibility.
- `T12F`: forged issuance_id with old checkpoint / mismatched parent effect -> conflict, zero F8 resume/Runtime effects.

### 6.3 Ingress disposition (not business verdict)

```text
U07AdmissionDisposition
  ADMITTED_FOR_F8
  REATTACHED_TO_CANONICAL
  TARGET_REATTACHED
  BLOCKED_AUTHORIZATION
  BLOCKED_SCHEMA
  BLOCKED_PAYLOAD_INTEGRITY
  IDENTITY_CONFLICT
  UNRESOLVED_TARGET
  BLOCKED_PROVENANCE
  DEFERRED_AUTHORITY_UNAVAILABLE
```

这些是 admission/transport 层结果，**不是 K10 F8 verdict**。已完成原事件的查询只可指向原事件 / 当前权威状态，不可直接回放旧 Clinical Truth。正式业务上的 DUPLICATE/EXPIRED/REJECTED 归 RDP-02。

## 7. Physical design: adapter, transaction, concurrency

候选组件边界（类名非既有事实）：
```text
U07InboundAdapter
  -> TrustedScopeGate + PayloadRefResolver
  -> U07EventCanonicalizer
  -> Foundation CanonicalBusinessEventLedger
  -> U07CanonicalEventBindingRepository
  -> U07AdmissionSnapshotResolver
  -> U07AdmissionCoordinator
  -> F8DecisionPort [RDP-02]
```

- 同一 `event_id`/key 的 admission 操作串行化或依赖已验证的唯一键竞争回读；不向 F8 发两份独立命令。
- 对一个 parent wait + Question，同轮不同 event IDs 应通过 `wait_answer_claim` 或等价版本化排他机制保证**最多一个在 apply 路径可胜出**；RDP-03 必须冻结 durable claim/lock 的 authority、lease/fencing 和并发冲突行为。RDP-01 只限定 admission 不抢先批准 apply。
- 规范 event+binding **必须处于同一数据源、同一 ACID outer transaction**；ledger 与 binding 全部提交前禁止 F8。跨库、outbox-only、best-effort 或 `CANONICAL_BINDING_PENDING` 自动继续均 **OUT_OF_SCOPE / REQUIRES_CONTROLLED_AMENDMENT**。unique race 导致 rollback-only 时新事务读取胜者并完整校验，不在受污染事务内继续。
- 固定唯一约束：event_id PK、idempotency_key UNIQUE（Foundation 已有）；额外 `canonical_event_id` UNIQUE binding；`target_answer_event_id` FK/权威引用校验。若采用新业务 claim 唯一键必须和 RDP-03 协调，不能临时侵入 F3/Clinical State owner。
- canonical ledger 的 `resolveOrCreate` 当前在同 key 不同 event_id 时会以 idempotency key 匹配返回 winner，但其 `requireSame` 验证字段有限；U07 必须在返回之后再校验**完整 side-binding**。
- Identity conflict 永不被降级为「新事件重试」；数据库 unique race 不允许被当成 ACCEPTED。

**Physical design acceptance gaps requiring independent review**：
1. `payload_digest` **原语义保持事件负载摘要**、U07 私有 binding_fingerprint 与 scope-key 方案的代码消费者/迁移证据；禁止悄然替换基础列含义。
2. 前置安全验证与 canonical identity commit 的并发时序。
3. existing Foundation JPA transaction/race behavior 在 unique constraint 下是否能继续查询胜者（需实现期数据库集成验证；不在设计阶段宣称 PASS）。
4. Side-binding 原子性及不可变约束。
5. Existing other consumers of canonical ledger 兼容性。
6. caller/tenant/scope 授权由哪一已获批准的 owner 提供；如果尚无可信提供者，不得授权真实入口实现。

## 8. Failure / crash / race contract

| 窗口 | 下一次相同请求 | 不变量 |
|---|---|---|
| 准入前 schema/auth 失败 | 无 canonical event 创建；允许新合法请求 | zero Runtime/P01/U02 |
| 负载已存、ledger 未提交 | 受控未引用 payload 回收；重试同身份 | 不泄露 PHI，不假成功 |
| 单一事务提交前崩溃（ledger 已 flush、binding 未写） | 整笔事务回滚；重试后另起事务 | 无可见孤立 ledger、无 F8 调用 |
| 发现历史或异常数据里 ledger 已持久但 binding 缺失 | quarantine / 不授予准入，人工或受审权威补偿 | 禁止按当前重试参数伪造旧 binding |
| binding 已提交、F8 尚未接收 | 重试 reattach canonical record、重新权威快照 | 不造第二 event effect |
| F8 正在处理 | 查询/幂等附着原 decision | 不生成竞争性 ACCEPTED |
| 同 event ID 改答案/Question | identity conflict；留审计 | 不改历史绑定 |
| 两个不同 event IDs 同待回答 Question 并发 | 保留两个候选记录，按 RDP-02/RDP-03 决定获胜者与结果 | 至多一个 APPLIED；本 RDP 不擅自宣布 DUPLICATE |
| state check 后 Consultation 被 U15 取消 | F8 / state commit 前重新校验 version 和 owner | 不基于 stale snapshot apply |
| Runtime checkpoint 丢失 | business event 可保留原法律有效性 | 不伪造 Business REJECTED；转 P02 repair |
| RESUME_REQUEST target 尚未完成 | 仅 reattach/wait/query | 零第二恢复副作用 |

## 9. Trace, observability, privacy

Trace 最少记录：trusted ingress decision ref、canonical event ID、event type、scope authorization ref、consultation **scoped reference**、question/wait/eligibility refs、binding fingerprint version、ledger resolve outcome、snapshot authority+versions、admission disposition、F8 handoff intent ref、error code；不得记原始回答、手机号、令牌、patient identifier 原文或非必要 PHI。

P05 trace 只记录发生了什么，不能替代 Foundation event ledger 或 F8/K09 业务 authority。不可恢复的冲突以审计事件单独保存并关联 trace。敏感 payload 必须走受控、加密、带 retention/consent 的存储；`answer_payload_ref` 不得是可猜测公开 URL。临床文本是否能够被保留到 U02，由真实 profile 的后续合规授权决定。

## 10. Profile applicability

本阶段唯一可继续讨论的后续实现范围为经明确授权的 `PROFILE-B SYNTHETIC_STRUCTURAL_NONPROD`，且调用方/fixture、Consultation、Clinical State store、Event Ledger 和 answer payload store 必须全部绑定同一 synthetic environment，不连接 real patient、production store 或外部 transport。

`PROFILE-A`、真实患者 USER_ANSWER/live RESUME_REQUEST、实际 PHI 和生产恢复全部 `BLOCKED`。任何 profile 升级均需要独立授权和完整 E2E 证据。没有凭据不允许自动 fallback 到 synthetic，也不能把 synthetic E2E 当作临床生产验证。

## 11. RDP-01 contract acceptance scenarios (design oracle plan)

| Case | Input situation | Expected admission result | Mandatory negative assertion |
|---|---|---|---|
| U07-RDP01-T01 | PROFILE-B synthetic valid USER_ANSWER + current wait | ADMITTED_FOR_F8 | F8 not pre-decided, no Runtime resume |
| T02 | same event and key replay | REATTACHED_TO_CANONICAL | no new canonical row/effect |
| T03 | different event ID alias, same scoped key, **same fields 1–21** | REATTACHED_TO_CANONICAL | original canonical ID retained; alias excluded from digest |
| T04 | same event ID but changed answer digest | IDENTITY_CONFLICT | no overwrite |
| T05 | same key but changed question/answer binding | IDENTITY_CONFLICT | no cross-binding |
| T05A | same alias + key but changed answer payload digest | IDENTITY_CONFLICT | no overwrite of original payload |
| T05B | different trusted tenant/scope + raw same client key | distinct global namespaced key / blocked cross-scope reference | no cross-tenant identity leak |
| T06 | new event ID/key, same answer and same wait | ADMITTED_FOR_F8 / F8_REVIEW_REQUIRED | RDP-01 does not assert DUPLICATE |
| T07 | identical answer for distinct Questions | separate canonical events | no global answer-hash dedup |
| T08 | RESUME_REQUEST valid target | TARGET_REATTACHED | no new answer effect |
| T09 | RESUME_REQUEST target absent/wrong consultation | UNRESOLVED_TARGET / BLOCKED_PROVENANCE | no Runtime resume |
| T10 | RESUME_REQUEST includes new answer payload | BLOCKED_SCHEMA | no event effect |
| T11 | old/superseded/expired business context | F8 review with typed snapshot reason | RDP-01 does not pre-empt F8 precedence |
| T12 | missing/stale checkpoint, verified original eligibility and authoritative still-current business wait | VERIFIED_HISTORICAL_REATTACHABLE + RUNTIME_RECONCILIATION_REQUIRED hint | no fake business REJECTED; P02 owns repair |
| T12A | Thread never AWAITING_USER, no eligibility issuance | DEFERRED_AUTHORITY_UNAVAILABLE pending U06/P02 repair | no fabricated eligibility |
| T12B | hash matches but no authoritative U06 issuance | BLOCKED_PROVENANCE | no F8/no resume |
| T13 | missing U06 authoritative delivered confirmation | BLOCKED_PROVENANCE | no fabricated delivery |
| T13A | revoked wait / U15 expired | BUSINESS_CONTEXT_MISMATCH_CANDIDATE with owner evidence | F8 owns EXPIRED/REJECTED precedence |
| T13B | source owner temporarily unavailable / incoherent versions | DEFERRED_AUTHORITY_UNAVAILABLE | no side effect; retry owner read |
| T14 | cancelled Consultation race | versioned recheck before downstream apply | no stale state write |
| T15 | unauthenticated, wrong tenant or real recipient | BLOCKED_AUTHORIZATION | zero ledger successful admission / zero PHI |
| T16 | payload digest mismatch / inaccessible ref | BLOCKED_PAYLOAD_INTEGRITY | no raw answer in trace |
| T17 | crash after ledger before binding | quarantined pending + deterministic repair | no F8 call |
| T18 | competing submissions same wait | both candidate identities may exist | at most one downstream apply (RDP-03 proof) |
| T19 | high-concurrency same idempotency key | one canonical winner | no false success if persistence fails |
| T20 | target answer accepted but runtime failed | RESUME_REQUEST reattaches historical target | does not rewrite ACCEPTED to REJECTED |

Above are **design assertions**. Physical fixtures/runner, exact thresholds and durable evidence belong to RDP-06 and post-authorization implementation verification. A reviewed test plan is not a passed runtime test.

## 12. Cross-RDP contract dependencies and explicit handoff

| To | RDP-01 output | Receiving owner's authority |
|---|---|---|
| RDP-02 F8 | canonical event ID, protected binding, snapshot refs/reasons, original target decision ref, semantic-repeat candidate | decides ACCEPTED / DUPLICATE / EXPIRED / REJECTED, complete ordered precedence |
| RDP-03 K09/P01 | immutable event identity + answer ref + parent wait provenance + concurrency claim requirement | freezes APPLIED/state mutation/effect IDs and one-winner guarantee |
| RDP-04 P02 | checkpoint/thread/run refs and `RUNTIME_RECONCILIATION_REQUIRED` hint | exclusively validates checkpoint compatibility and Runtime rehydrate |
| RDP-05 P01/P02/P05/P06 | versioned dependency/identity/scope needs and physical inventory gaps | binds authoritative providers, availability and contract-compatible versions |
| RDP-06 | cases T01–T20, negative effects and design-only oracle expectations | formal scenario IDs, authoritative runner design, post-authorization evidence |

## 13. Explicit design-review questions and blockers

```text
BF-U07-RDP01-IR-01
  Original canonical ID versus request alias and fingerprint inputs are now explicitly separated; re-review must verify T03/T04/T05/V1–V8. RESUME_REQUEST reference policy still requires Phase 8/9 equivalence check.
BF-U07-RDP01-IR-02
  Physical dual-dialect V7 binding migration, bounded code/fixture/migration inventory, outer transaction / rollback-isolated reconciliation now specified in §4.3.1–4.3.3. Repository-wide search & database tests are explicit future authorization gates, not claimed evidence.
BF-U07-RDP01-IR-03
  Confirmed existing V6 wait/checkpoint/trace records and the absence of an issuance record in examined U06 code; mandatory controlled amendment CA-U06-U07-ELIG-ISSUANCE-01 now freezes new U06-owned issuance query contract before U07 implementation; no hash-only historical Resume.
BF-U07-RDP01-IR-04
  Can eligibility currentness be established independently of a mere parent-effect/checkpoint hash, preserving checkpoint-failure separation?
BF-U07-RDP01-IR-05
  Are ledger/binding atomicity and Foundation concurrent unique-index race handling feasible with the current storage semantics?
BF-U07-RDP01-IR-06
  Are synthetic profile, authority snapshots, PHI and trace boundaries closed without speculative live permissions?
```

Until an exact-head independent design review passes these questions:
```text
B-U07-RG-01 = DESIGN_CANDIDATE / NOT_CLOSED
U07-RDP-01 = SECOND_TARGETED_REMEDIATION_CANDIDATE / READY_FOR_INDEPENDENT_RE_REVIEW, NOT FROZEN
CA-U06-U07-ELIG-ISSUANCE-01 = REQUIRED_UPSTREAM_AMENDMENT / NOT_DESIGNED_OR_AUTHORIZED
BF-U07-RDP01-IR-01..03 = REMEDIATED_FOR_RE_REVIEW / NOT_CLOSED
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
```

No runtime source changes, merge, release, PROFILE-A, production, real external side effects or real-patient authorization granted.
