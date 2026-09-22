# Runtime Canonical Effect Ledger — Non-Production Controlled Design v0.1

> Design ID: `RUNTIME-EFFECT-LEDGER-NC-01`  
> Proposed authorization ID: `AUTH-RUNTIME-EFFECT-LEDGER-NC-001`  
> Trigger: U05 implementation blocker `BF-U05-IMPL-IR-06`  
> Impact review: PR #183 / PASS / review_id `5265060541`  
> Status: **PROPOSED / READY_FOR_INDEPENDENT_DESIGN_REVIEW**  
> This document authorizes no implementation.

---

# 1. Problem

Frozen U05 contracts require durable runtime/governance replay evidence.

RDP-01 requires crash recovery:

    lookup U05_ADMISSION_ID
    -> reattach same authoritative admission record

RDP-04 requires:

    U05DownstreamRouteLedgerRecord

whose owner is:

    Runtime / Canonical Effect Ledger

and requires crash/restart reattachment across:

    readiness commit -> route decision
    route decision -> eligibility.

Current PR #182 uses:

    U05InMemoryAdmissionLedger
    U05InMemoryRouteLedger

which provide same-JVM retry behavior only.

After process reconstruction/restart:

    admission ledger state is lost
    routing decision ledger state is lost
    eligibility reattachment state is lost.

Therefore current implementation cannot prove the frozen crash/replay semantics.

The repository currently contains no implemented shared Runtime / Canonical Effect Ledger
suitable for these semantics.

---

# 2. Design objective

Implement one generic:

    non-production durable canonical effect ledger

that provides stable immutable effect-record creation and replay reconciliation.

The ledger must be:

    generic
    non-clinical
    non-production
    business-policy-free
    filesystem-backed for bounded engineering verification
    durable across service-object/process reconstruction
    fail-closed on corruption or identity/fingerprint conflict.

It must NOT become:

    Clinical State
    Clinical Truth
    Clinical Readiness
    Scheduler business policy
    downstream Unit state
    checkpoint clinical authority
    production persistence architecture.

---

# 3. Ownership boundary

Owner:

    Runtime / Canonical Effect Ledger

The ledger owns only:

    durable effect identity
    canonical fingerprint association
    immutable record bytes
    record integrity metadata
    create-or-reattach semantics.

The consumer owns:

    business/effect identity derivation
    canonical semantic fingerprint derivation
    business record schema
    record encoding/decoding
    currentness validation
    downstream permission
    Clinical State truth.

Therefore:

    Ledger
    != U05 Admission Owner

    Ledger
    != D03

    Ledger
    != RDP-04 mapping Owner

    Ledger
    != Clinical State.

---

# 4. Generic public contract

Proposed shared package:

    com.aidoctor.diagnosis.runtime.effects

Minimum public abstractions:

    CanonicalEffectLedger
    CanonicalEffectLedgerRecord
    CanonicalEffectLedgerDecision
    NonProductionFileCanonicalEffectLedger

The exact Java class names may vary,
but semantic operations are frozen below.

## 4.1 Lookup

    inspect(
      namespace,
      effect_identity,
      expected_canonical_fingerprint
    )

returns exactly one typed decision:

    ABSENT

    REATTACHED
      existing identity
      same canonical fingerprint
      valid integrity/checksum
      immutable prior record returned

    CONFLICT
      existing identity
      different canonical fingerprint

    CORRUPT
      record exists
      but integrity/schema/storage validation fails

    UNAVAILABLE
      ledger storage cannot be safely inspected.

No exception/failure may be converted to ABSENT.

## 4.2 Create-if-absent

    create_if_absent(
      namespace,
      effect_identity,
      canonical_fingerprint,
      record_schema_version,
      immutable_record_bytes
    )

returns:

    CREATED

    REATTACHED
      an exact same identity/fingerprint record already won the race

    CONFLICT
      same identity
      different canonical fingerprint

    CORRUPT

    UNAVAILABLE.

Required invariant:

    same namespace
    + same effect identity
    + same canonical fingerprint

    -> exactly one durable canonical record
    -> all exact retries reattach it.

Required exact reattachment equivalence:

    same namespace
    + same effect identity
    + same canonical fingerprint
    + same record_schema_version
    + same payload SHA-256
    + existing record integrity valid

    -> REATTACHED.

Required conflict invariant:

    same namespace
    + same effect identity

and any canonical record material differs:

    canonical fingerprint differs
    or record_schema_version differs
    or payload SHA-256 differs

    -> CONFLICT
    -> no overwrite.

Therefore:

    same effect identity + same fingerprint
    but different payload/schema
    != exact replay.

The first valid canonical record remains immutable.

create_if_absent must always compare full canonical record equivalence.

inspect may return the existing valid record for consumer-side strict decode/currentness checks,
but must never hide storage-integrity corruption.

---

# 5. Namespace semantics

Namespace prevents unrelated effect types from colliding.

Namespace is:

    opaque runtime/governance partition identity

not a business-policy registry.

For current U05 consumption, minimum namespaces may be:

    U05_ADMISSION
    U05_ROUTING_DECISION
    U05_ROUTE_ELIGIBILITY

These names are consumer allocations only.

The shared ledger must not contain logic such as:

    if namespace == U05_ADMISSION then validate route
    if namespace == U05_ROUTING_DECISION then map readiness

No such U05-specific business semantics are allowed.

Namespace identity must be normalized and validated as a bounded opaque token.

---

# 6. Record semantics

A canonical ledger record minimally stores:

    ledger_format_version

    namespace

    effect_identity

    canonical_fingerprint

    record_schema_version

    immutable_record_bytes

    record_payload_sha256

    created_at / assigned-once ledger timestamp

    full_record_sha256 or equivalent integrity checksum.

The shared ledger does not parse business payload bytes.

The consumer is responsible for:

    deterministic canonical business record encoding

    schema versioning

    semantic decode validation.

The ledger verifies only:

    storage-format validity
    namespace / identity / fingerprint equality
    payload length limits
    checksum/integrity.

---

# 7. Payload boundary

For this non-production capability:

    immutable_record_bytes

must be bounded.

Proposed maximum:

    <= 256 KiB per record.

Required:

    zero-length payload prohibited

    payload copied before persistence

    returned payload copied before exposure

    mutation of caller byte[] after create
    must not alter durable record.

Current verification data must be:

    synthetic / non-PHI only.

This capability does not authorize PHI persistence.

---

# 8. Non-production durable adapter

Proposed implementation:

    NonProductionFileCanonicalEffectLedger

using only JDK filesystem primitives.

No:

    database
    Redis
    JPA
    HTTP
    cloud storage
    external service
    production profile

is introduced.

Constructor must require an explicit caller-provided:

    non-production ledger root directory

and must not select a production/default path automatically.

No Spring component/configuration annotation is permitted.

---

# 9. Filesystem identity layout

Recommended deterministic layout:

    <root>/
      <namespace-hash-or-safe-token>/
        <effect-identity-hash>/
          record.bin

The filesystem path must not use raw business payload.

Raw consultation/patient content must not be used as filenames.

Path derivation must resist:

    ../ traversal
    absolute-path injection
    path separator injection
    namespace collision.

The durable record itself contains the original validated opaque:

    namespace
    effect_identity

for equality verification after read.

---

# 9.1 Root containment / symbolic-link boundary

The caller must provide a dedicated non-production ledger root.

At adapter construction:

    root must exist or be explicitly created as the dedicated root
    under caller control

    root must resolve to one real/canonical directory

    resolved root identity is fixed for the ledger instance.

All derived canonical storage paths use only safe/hash-derived child names.

Before any canonical/lock/temp file operation:

    namespace directory
    effect directory
    canonical record path
    coordination/lock path

must be checked so that:

    no used storage object is a symbolic link

    resolved parent/object path remains contained under the fixed resolved root.

If:

    root/path resolution escapes configured root
    or any derived storage component is a symbolic link
    or containment cannot be established reliably

then:

    -> UNAVAILABLE
    -> no read/write/create claim.

A symlink violation must never be downgraded to:

    ABSENT.

Focused tests must include:

    malicious-looking namespace/effect input

    pre-created symlink at a derived namespace/effect/canonical path

    attempted external target

Expected:

    UNAVAILABLE / fail closed
    and no write outside the dedicated root.

---

# 10. Atomic create semantics

The ledger must provide process-safe create-or-reattach behavior for the non-production harness.

Canonical-existence authority:

    canonical target record
    = only source of durable record existence truth.

Temporary files:

    are staging only
    != canonical record
    != effect authority.

Stable lock file/path:

    = coordination metadata only
    != canonical record
    != replay authority.

Required algorithmic properties:

    no overwrite of an existing canonical record

    one concurrent creator wins

    loser reloads the canonical target and returns:
      REATTACHED
      or CONFLICT
      or CORRUPT

    crash during staging
    must not produce a partially valid canonical record.

Required publication sequence:

    encode complete record to a temporary file
    located inside the dedicated ledger root / same filesystem

    write complete bytes

    fsync temporary file

    acquire stable per-identity filesystem coordination
      via JDK FileChannel/FileLock or equivalent bounded JDK mechanism

    if lock cannot be acquired/reliably used:
      -> UNAVAILABLE

    while coordination is held:
      re-inspect canonical target

      if canonical target exists:
        validate integrity
        compare full canonical equality
        -> REATTACHED / CONFLICT / CORRUPT

      if canonical target does not exist:
        publish temp -> canonical target
        using an atomic non-overwriting filesystem operation

    fsync canonical target

    attempt/verify containing-directory durability
    to the extent required by the supported non-production platform

    only after the required durability steps succeed:
      -> CREATED.

Forbidden fallback:

    copy-over-existing
    replace-existing
    last-writer-wins
    truncate-and-rewrite canonical target
    unsafe rename/copy when atomic non-overwrite cannot be established.

If the configured filesystem/platform cannot provide the frozen
non-production atomic publication guarantee:

    -> UNAVAILABLE
    -> no canonical record may be claimed CREATED.

## 10.1 Orphan temporary files

After process crash:

    orphan temp file
    != canonical record.

On restart:

    inspect canonical target first.

If canonical target exists:

    validate/use canonical target;
    orphan temp files have no authority.

If canonical target is absent:

    result remains ABSENT
    unless another lawful creator publishes a canonical record.

Orphan temp cleanup may occur only as bounded housekeeping
after canonical-target determination.

Cleanup failure:

    must not cause an orphan temp file to be treated as canonical.

## 10.2 Crash-injection requirement

Focused verification must include:

    writer creates/fsyncs temp
    -> aborts before canonical publication
    -> new process opens same root
    -> inspect identity

Expected:

    ABSENT

unless a separate lawful concurrent creator published the canonical target.

It must also include:

    canonical publication completed
    -> process reconstruct/restart
    -> inspect
    -> REATTACHED with exact prior canonical record.

---

# 11. Crash / corruption semantics

On inspect:

    missing canonical target
    -> ABSENT

Only if no evidence of an interrupted/corrupt canonical record exists.

If canonical record exists but:

    truncated
    unsupported ledger_format_version
    namespace mismatch
    identity mismatch
    checksum mismatch
    invalid length
    malformed binary framing

then:

    CORRUPT

not:

    ABSENT
    REATTACHED.

If IO permission/filesystem failure prevents reliable determination:

    UNAVAILABLE.

The ledger must never delete/replace a corrupt existing canonical record
as part of normal inspect/create.

Manual repair is outside this capability.

---

# 12. Durability level

Required current non-production guarantee:

    survives:
      ledger service object reconstruction
      JVM/process restart
      re-opening the same dedicated ledger root.

Not claimed:

    multi-host distributed consensus
    network filesystem correctness
    production HA
    disaster recovery
    cross-region durability.

This limited guarantee is sufficient only for the current synthetic
crash/restart verification harness.

---

# 13. Replay status vs stored record

The durable stored canonical record is immutable.

Replay status:

    CREATED / REATTACHED

is attempt-local response metadata.

The shared ledger must not rewrite the canonical record merely to record:

    replay count
    retry timestamp
    attempt number.

Therefore:

    canonical record bytes
    remain first-authoritative-write immutable.

Consumer-level returned objects may label:

    replay_disposition = REATTACHED

without mutating the stored canonical business record.

---

# 14. U05 admission consumption

After this capability is independently implemented/verified,
U05 may replace:

    U05InMemoryAdmissionLedger

with a U05-owned adapter over CanonicalEffectLedger namespace:

    U05_ADMISSION.

U05 still owns:

    U05_ADMISSION_ID
    normalized inbound fingerprint
    U05AdmittedInput encoding/decoding
    currentness revalidation.

Flow:

    derive admission_id
    derive normalized admission fingerprint

    inspect/create_if_absent U05_ADMISSION

    CREATED
      -> use the exact stored canonical admitted record

    REATTACHED
      -> decode exact prior admitted record
      -> verify schema/identity
      -> revalidate currentness
      -> continue or reject stale

    CONFLICT/CORRUPT/UNAVAILABLE
      -> fail closed
      -> no D03.

This shared ledger does not itself decide U05 admission.

---

# 15. U05 routing consumption

U05 may replace:

    U05InMemoryRouteLedger

with U05-owned adapters over:

    U05_ROUTING_DECISION

and for eligible routes:

    U05_ROUTE_ELIGIBILITY.

U05 continues to derive:

    U05_ROUTING_DECISION_ID
    routing canonical fingerprint

    U05_DOWNSTREAM_ROUTE_EFFECT_ID
    eligibility identity
    route payload fingerprint.

The ledger only persists/reconciles those derived identities.

For:

    PREEMPTED
    FAILURE_REQUIRED
    REJECTED_STALE

only the routing-decision canonical record is stored.

It must not fabricate:

    route effect
    eligibility
    route lifecycle.

For:

    ELIGIBLE

U05 stores/reconciles:

    routing decision

and the exact:

    eligibility canonical record.

Crash after decision/before eligibility:

    reattach routing decision
    -> create/reconcile same eligibility identity.

Crash after eligibility:

    reattach eligibility.

No downstream Unit invocation occurs in this capability.

---

# 16. Scheduler boundary

This capability does NOT implement:

    Scheduler

    U05_ROUTE_CONSUMPTION_ID

    target execution intent

    U06/U08/U10/U11/U14 invocation.

Current U05 authorized slice stops at:

    durable eligibility
    + non-live/fake Scheduler intent evidence.

Any future durable route-consumption ledger semantics require
their own explicit design/authorization if not already implemented.

---

# 17. Clinical authority boundary

Mandatory invariants:

    ledger record
    != Clinical State

    ledger record
    != Clinical Truth

    ledger record
    != Clinical Readiness

    ledger CREATED
    != state commit

    ledger REATTACHED
    != downstream effect applied

    trace
    != ledger authority

    checkpoint
    != Clinical State authority.

If Clinical State and ledger disagree:

    authoritative Clinical State / governed CommitResult+read-back
    wins for clinical state truth.

Ledger may only prove the prior runtime effect record existed.

---

# 18. Exact proposed file scope

Proposed implementation may add only a new generic package:

    diagnosis-service/src/main/java/com/aidoctor/diagnosis/runtime/effects/**

and focused tests:

    diagnosis-service/src/test/java/com/aidoctor/diagnosis/runtime/effects/**

No existing production/shared source modification is expected.

Specifically no modification is authorized to:

    StateCommitter
    StateRepositoryPort
    SyntheticVersionedStateRepository
    StatePatchBoundaryValidator
    contracts/v1/**
    Scheduler
    U01-U05 business source
    Spring/config/persistence/network files.

If implementation requires modifying an existing production/shared source file:

    STOP
    -> impact review
    -> authorization amendment.

---

# 19. Generic record codec boundary

To avoid adding a business-aware shared serializer:

    shared ledger stores opaque bytes.

The shared package may define only its own binary storage framing.

U05-specific codecs remain under:

    runtime/u05/**

and are outside this shared capability.

The shared framing must be deterministic and versioned.

Recommended:

    fixed magic bytes
    ledger_format_version
    bounded UTF-8 namespace
    bounded UTF-8 effect identity
    bounded UTF-8 canonical fingerprint
    bounded UTF-8 record_schema_version
    assigned-once timestamp
    payload length
    payload bytes
    payload SHA-256
    full record SHA-256.

No Java native object serialization is permitted.

---

# 20. Required focused verification

At minimum:

## A. create

    ABSENT
    -> create_if_absent
    -> CREATED
    -> inspect same identity/fingerprint
    -> REATTACHED
    -> exact payload bytes equal.

## B. same replay

    repeated create_if_absent same identity/fingerprint/payload
    -> REATTACHED
    -> no rewrite
    -> first created_at retained.

## C. canonical record conflict

Any of:

    same identity + different canonical fingerprint

    same identity + same fingerprint
    + different record_schema_version

    same identity + same fingerprint/schema
    + different payload SHA-256

must return:

    CONFLICT

and prior canonical record remains unchanged.

## D. service reconstruction

    create record
    -> discard ledger object
    -> instantiate new ledger object on same root
    -> inspect
    -> REATTACHED.

## E. process-style restart harness

    writer process/JVM invocation creates record
    -> later reader invocation using same root
    -> exact record reattached.

At minimum a subprocess or equivalent isolated-runtime test must prove this,
not merely a new Java object in the same process.

## F. corruption

    truncate/modify canonical file
    -> CORRUPT
    -> no overwrite/delete.

## G. unsupported version

    mutate format version
    -> CORRUPT.

## H. unavailable filesystem

    inaccessible/unusable root
    -> UNAVAILABLE
    -> not ABSENT.

## I. namespace isolation

    same effect_identity
    under two namespaces
    -> two independent records.

## J. traversal / symlink / containment safety

malicious-looking namespace/effect strings:

    cannot escape configured root.

Pre-create symbolic links at derived storage components:

    namespace/effect/canonical/coordination path

Expected:

    UNAVAILABLE / fail closed

    no read/write outside configured real root.

## K. concurrent creators

multiple concurrent create_if_absent calls:

    exactly one CREATED

all exact same-fingerprint losers:

    REATTACHED

different-fingerprint contender:

    CONFLICT

canonical record never overwritten.

## L. payload copy

mutate caller byte array after create:

    persisted record unchanged.

mutate returned byte array:

    subsequent inspect unchanged.

## M. size limit

payload > 256 KiB:

    fail closed
    no canonical record.

## N. business independence guard

shared package:

    must not import runtime.u05
    must not import clinical business packages
    must not contain readiness/route mapping policy.

## O. production activation guard

    no Spring annotations
    no default filesystem path
    no database/network client.

## P. crash before publication

    temp fully written/fsynced
    process abort before canonical publication
    restart on same root

Expected:

    ABSENT
    unless another lawful creator published canonical target.

Orphan temp is never reinterpreted as canonical.

## Q. atomic publication unsupported

Synthetic/injected filesystem publication failure:

    -> UNAVAILABLE
    -> no CREATED claim
    -> no fallback overwrite/copy semantics.

---

# 21. U05 closure criteria after shared implementation

RUNTIME-EFFECT-LEDGER-NC-01 being implemented is NOT enough by itself to close IR-06.

U05 must then:

    replace/retire in-memory admission ledger
    for authoritative crash/replay verification path

    replace/retire in-memory route ledger
    for authoritative crash/replay verification path

    provide deterministic U05 record codecs

    verify exact reattachment after isolated restart

    verify conflict fail-closed

    verify no duplicate eligibility

    verify currentness again after admission/route reattachment.

Only after exact-head U05 re-review may:

    BF-U05-IMPL-IR-06 = CLOSED.

---

# 22. Authorization boundary

Before explicit Owner Authorization:

    AUTH-RUNTIME-EFFECT-LEDGER-NC-001
    = NOT_GRANTED

Allowed:

    design
    independent review
    decision-package preparation.

Not allowed:

    add runtime/effects production source
    modify shared runtime source
    consume as authoritative U05 ledger
    claim IR-06 closed.

No merge/production/live authorization is implied.

---

# 23. Required governance sequence

    design
    -> independent design review
    -> explicit Owner Authorization Decision
    -> implementation from authorized decision lineage
    -> exact-head implementation review
    -> focused durable/restart verification
    -> independent verification
    -> U05 consumption
    -> repeat U05 exact-head implementation review.

No shared capability merge is authorized by this design.


---

# 24. Independent Design Review Remediation

Initial Independent Design Review:

    PR #185
    verdict = REVISE_REQUIRED
    review_id = 5265151036

Findings:

    BF-RUNTIME-EFFECT-LEDGER-NC-IR-01
    = SAME_FINGERPRINT_DIFFERENT_PAYLOAD_UNDERDEFINED

    BF-RUNTIME-EFFECT-LEDGER-NC-IR-02
    = ATOMIC_FILE_COORDINATION_AND_STALE_TEMP_SEMANTICS_UNDERDEFINED

    BF-RUNTIME-EFFECT-LEDGER-NC-IR-03
    = FILESYSTEM_ROOT_SYMLINK_BOUNDARY_UNDERDEFINED

Remediation:

    IR-01:
      exact REATTACHED requires identity + fingerprint + schema + payload digest equality

    IR-02:
      canonical-target-only authority, temp/lock non-authority,
      fail-closed atomic publication and crash-injection semantics frozen

    IR-03:
      real-root containment and symlink fail-closed rules frozen

Current:

    BF-RUNTIME-EFFECT-LEDGER-NC-IR-01
    = REMEDIATED / TARGETED_REVIEW_PENDING

    BF-RUNTIME-EFFECT-LEDGER-NC-IR-02
    = REMEDIATED / TARGETED_REVIEW_PENDING

    BF-RUNTIME-EFFECT-LEDGER-NC-IR-03
    = REMEDIATED / TARGETED_REVIEW_PENDING

    RUNTIME-EFFECT-LEDGER-NC-01 Design
    = REVISED / READY_FOR_TARGETED_INDEPENDENT_REVIEW
