# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Targeted Independent Design Re-Review v0.1

> Review date: 2026-10-08
> Target: author [PR #300](https://github.com/cxjchelsea/AIdoctor/pull/300)
> **Exact reviewed author HEAD:** `09736a2764a7ea5ba640fbc82bd87bc403ad0b52`
> **Exact document blob:** `3d4b1ece54858afbe8795bdbc079f0b37d62641a`
> Previous [PR #301](https://github.com/cxjchelsea/AIdoctor/pull/301) independent review: REVISE_REQUIRED at author `dad40fb399225290c4774d4b748b174e9868c2ed`
> Main physical source remains `6d4fd787600e3a57f01f3e17893e6d98893ac546`
> **Independent verdict = PASS / CONDITIONAL_DESIGN_ACCEPTANCE ONLY**
> Review only; no application startup, test harness installation, database access, Runtime changes, authorization, or merge.

## 1. Exact-head scope and amended artifacts

Independently fetched the entire amended contract and compared original reviewed author HEAD to the new author HEAD. GitHub compare reports **one Markdown document changed**, 94 additions and 8 deletions; no executable code or database migration. The unchanged original `TX-G01..08` gate identifiers remain consecutive. `TX-T01..16` retains original case identities; `TX-T17..26` adds ten design-only negative cases. All 26 cases are sequentially numbered; **no tests were run**.

## 2. BF-U07-FOUND-SEM-TX-IR-01 — pre-bootstrap no-side-effect containment

**CLOSED / CONDITIONAL_DESIGN.**

The revised §2.1 freezes a single deny-first Tier-1 proposal rather than after-the-fact instrumentation:
1. Explicit Foundation/U01 plus Security grant in an isolated PROFILE-B synthetic, nonpatient execution.
2. Disposable isolated OS sandbox/container with outbound network and local socket/metadata/DB endpoints denied, no clinical credentials or data, read-only source mounts and controlled scratch.
3. Protective OS policy attested **before JVM execution**; test-only JDBC, pool, JPA/Hibernate, Flyway, socket, HTTP, scheduler/lifecycle, filesystem and outbound attempt guards installed **before** ApplicationContext creation.
4. Any forbidden attempt, **even when blocked**, is `CONTEXT_UNSAFE`; any missing preventive policy/guard prevents boot; instrumentation coverage gaps cannot assert zero.
5. The explicit bootstrap override list records each property/Bean/manager/EMF/DataSource impact. Stub substitution or auto-config alterations that change the target resource graph are `OBSERVED_WITH_TEST_OVERRIDES / REPRESENTATIVENESS_NOT_PROVEN`, not positive original-profile manager equivalence.
6. Unsafe startup yields `NOT_EXECUTABLE_UNDER_READ_ONLY_SCOPE` and Tier-0 static evidence only. No retry with looser network policy, original dev Oracle, secrets or PHI.

**Why conditional:** no actual sandbox or interceptor coverage has been built, deployed or independently evidenced; “no side effects” is a testable design obligation, not a verified historical fact. A future detailed physical probe authorization/readiness must prove OS enforcement, interceptibility, early Spring initializer coverage and configuration representativeness *before any Tier-1 context execution*. A mere successful context bootstrap cannot upgrade the evidence.

## 3. BF-U07-FOUND-SEM-TX-IR-02 — deterministic effective-manager resolution

**CLOSED / CONDITIONAL_DESIGN.**

The amended §4.0 freezes a per-proxied-method metadata algorithm `R0..R9`:

```text
Exact source/target method + blob
→ BeanFactory selected Bean, injection edge, actual JDK/CGLIB proxy
→ actual transaction advisor ordering + TransactionInterceptor
→ TransactionAttributeSource resolution for proxy method and target class
→ effective @Transactional qualifier / value
→ interceptor's qualified manager lookup or configured default /
   TransactionManagementConfigurer / exact Spring fallback
→ actual PlatformTransactionManager object identity
→ EntityManagerFactory + persistence unit + DataSource/proxy-chain identity
→ per-edge E01..E05 comparator, including override impact and unknowns
```

Explicit method qualifier cannot be replaced by a generic `@Primary` guess; multiple eligible managers without established interceptor resolution => `MANAGER_RESOLUTION_UNKNOWN`. JDK interface/target attribute conflicts, self-invocation, final/non-proxied method or ambiguous advisor order are not accepted as evidence of applied transaction advice. Equality is separately classified at Manager, EMF, persistence unit and DataSource tiers; **none establishes a shared physical JDBC transaction**. Only actual same-object identity tokens, not class names, can support same-bean classification. Test substitute manager/EMF/DataSource prevents unqualified `E01..E05` PASS.

New `TX-T21..24` and `TX-T26` explicitly exercise qualified versus primary manager, `TransactionManagementConfigurer` fallback, JDK proxy/self-call discrepancies, same DataSource under different managers and override-induced change.

**Why conditional:** the exact runtime Spring version's `TransactionInterceptor`/BeanFactory fallback semantics still require an independently reviewed comparator against the actual deployed version, plus a safe, representative ApplicationContext. The contract appropriately returns UNKNOWN where purely introspective metadata cannot determine true selection. Do not promote configuration equivalence to actual JDBC enlistment or COMMIT proof.

## 4. Retained authority and evidence boundaries

| Domain | Verdict after design re-review |
|---|---|
| `TX-G01..08` review gates | DESIGN_PRESERVED / NOT_EXECUTED |
| `TX-T01..26` scenarios | DESIGN_PRESERVED / NOT_EXECUTED |
| Tier-0 static inspection | contract accepted, previously sourced lexical scan remains PASS |
| Tier-1 offline ApplicationContext probe | **NOT_AUTHORIZED / NOT_EXECUTED** |
| Effective Spring manager and U01 configured edge proof | **NOT_PROVEN** |
| Real JDBC connection/rollback/commit proof (Tier-2) | **OUT_OF_SCOPE / NOT_AUTHORIZED** |
| `BF-U07-FOUND-SEM-02` duplicate-key collision/rollback safety | **OPEN / INDEPENDENT** |
| Foundation audit gate | **NOT_PASSED** |
| U07 implementation readiness / authorization | **NOT_READY / NOT_GRANTED** |
| PROFILE-A, PHI, real patients, production | **BLOCKED** |

The accepted design may proceed to a **separately scoped diagnostic implementation/physical-environment feasibility and authorization decision**. The decision must identify exactly who approves offline execution, how safety enforcement will be independently attested, which artifacts and comparator are authorized, and whether startup is viable without changing the target manager semantics. If not viable, remain `SOURCE_ONLY`; do not lower safety requirements.

A later positive Tier-1 evidence review may close the *configuration topology* subgate only. It cannot close `BF-U07-FOUND-SEM-02`, U07 Event+Binding physical atomicity, or the Foundation gate itself without their own accepted authority and evidence.

## 5. Formal decision

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
Targeted Independent Design Re-Review
= PASS / CONDITIONAL_DESIGN_ACCEPTANCE

Exact reviewed author HEAD = 09736a2764a7ea5ba640fbc82bd87bc403ad0b52
BF-U07-FOUND-SEM-TX-IR-01 = CLOSED / CONDITIONAL_DESIGN
BF-U07-FOUND-SEM-TX-IR-02 = CLOSED / CONDITIONAL_DESIGN

CA-U07-FOUND-SEM-EFFECTIVE-TX-01
= CONDITIONALLY_ACCEPTED_DESIGN / NOT_AUTHORIZED

TX-G01..08 = NOT_EXECUTED
TX-T01..26 = NOT_EXECUTED

BF-U07-FOUND-SEM-01 = OPEN / MANAGER_TOPOLOGY_NOT_PROVEN
BF-U07-FOUND-SEM-02 = OPEN / COLLISION_ROLLBACK_NOT_PROVEN
BF-U07-FOUND-AUD-01..04 = OPEN
BF-U07-FOUND-AUD-03 = LEXICAL_SUBGATE_COMPLETE

GATE-U07-RDP01-FOUNDATION-REFERENCE-AUDIT-01 = NOT_PASSED
U07 Implementation Readiness = NOT_READY
U07 Implementation Authorization = NOT_GRANTED
PROFILE-A / PHI / real-patient / production = BLOCKED
```

**Recommended next step:** `CA-U07-FOUND-SEM-EFFECTIVE-TX-01 Producer / Diagnostic Harness Integration Readiness + Exact-Diff Authorization Review` (pre-implementation). Evaluate sandbox enforceability, source/proxy context boot feasibility and exact proposed diagnostic-only files before a **separate explicit authorization decision**. Neither this design re-review nor the prior U07 aggregate design review authorizes code changes, physical Spring bootstrap or DB access.
