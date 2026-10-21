# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Personal Owner Governance Targeted Independent Design Re-Review v0.1

> 2026-10-09 · Analytical independent review, **not** external human/organizational Security or Foundation authorization.
> Author [PR #335](https://github.com/cxjchelsea/AIdoctor/pull/335) exact revised HEAD `cef1e77948403bdddab1371a2592115ce4b88365`, blob `a3e03b3ef621506294203562d9ba6fd9caaf524e`.
> Prior independent [PR #336](https://github.com/cxjchelsea/AIdoctor/pull/336) @ `7c6b62d7ea56bdb815f3e5fb7d5f958632fcb40e`, findings `RF-U07-A1-PGA-IR-01..03`.
> Repository baseline `main@86e8843197091c8c8172b7e4213537a31bdf0654`.
> **VERDICT: PARTIAL_PASS / REVISE_REQUIRED_FOR_EFFECTIVE_AUTHORITY.** Two findings closed **for design only**, frozen-authority precedence/source **remains a blocker**. This is not an effective governance amendment, R2 grant, environment attestation, A1 canary PASS, or merge approval.

## 1. Exact-input review and methodology

Re-fetched the PR #335 current HEAD, `main` latest commit, changed paths and revised Markdown blob from GitHub rather than relying on the prior message. The author PR remains **one ADD-only Markdown document** relative to `main@86e8843197091c8c8172b7e4213537a31bdf0654`. Its §10 is an explicit targeted appendix and supersedes ambiguous conclusions in §7–§9 on findings disposition. No other PR is implicitly incorporated into this branch.

The review's concrete evidence is limited to repository source readback and the developer's previously reported local Docker CMD outputs. It did **not** execute any command on the personal computer, produce a signed runner attestation, inspect other business containers, or verify denial via OS audit.

## 2. Finding-by-finding independent decision

| Finding | Prior state | Revised source evidence | Targeted result |
|---|---|---|---|
| `RF-U07-A1-PGA-IR-01` authoritative frozen source / exact diff | BLOCKER | §10.1 pins draft PR #328 HEAD/blob `3f6725f… / 2518bdb…`, PR #326 HEAD/blob `6884c26… / 72b710…`, enumerates main tree and marks applicable new-supplement diff as proposed only | **PARTIAL / BLOCKER_OPEN**. Identified **candidate documents**, but NOT the exact effective governing contract, precedence branch, authorizing policy or effective target |
| `RF-U07-A1-PGA-IR-02` per-signer substitution | BLOCKER at original design level | §10.2 binds original PR #328 draft fields to proposed personal substitutions, forbids Security/Foundation-U01 auto-substitution, defines `APPROVER_UNAVAILABLE => NOT_READY` | **CLOSED_CONDITIONAL_DESIGN_ONLY**. Clearer authority matrix, but no actual governing exception or signer credential granted |
| `RF-U07-A1-PGA-IR-03` R2 one-attempt provenance/capture | REQUIRED | §10.3 introduces typed `U07StageA1LocalR2EvidenceEnvelopeV1`, runner/image/context/engine/kernel, fresh attempt/process IDs, collector/argv/output/retention/drift fields and no-raw-data constraint | **CLOSED_CONDITIONAL_DESIGN_ONLY**. Good minimum schema; command binaries, trusted collector, output ACL and live evidence still unapproved/unexecuted |

**Why IR-01 remains open:** a default-branch tree filename enumeration cannot prove that some other branch or binding contract is not authoritative, nor can absence of a Stage A1 file prove that the draft-signature rules can be waived. The author correctly distinguishes `main` Tier-0 Producer Manifest (tree blob `98189638619aa78e997a1a588239b36b89617378`) from draft R2 authority. Its suggested `parent_approval_profile_ref` is still a placeholder. It therefore cannot be described as an exact, executable, authority-bearing controlled-diff design.

**Review qualification on IR-02:** the role matrix is adequate as a *safe design option* but an AI reviewer is not the external Security/Foundation person required by a valid controlling policy. An owner signed local asset declaration proves ownership consent, not independent prevention assurance.

**Review qualification on IR-03:** R2-01..05 reads from a collector; they do not attest all target canary processes. `Seccomp: 2`, loopback-only namespace and readonly root demonstrate sample configuration, not `DENIED_BY_POLICY`; independently verified rule-origin coverage remains a future gate.

## 3. Exact gate matrix

| Gate | Result | Basis |
|---|---|---|
| `PGA-TIR-01` author HEAD/blob and main exact source | PASS |
| `PGA-TIR-02` focused signer substitution, individual-vs-independent role separation | PASS_CONDITIONAL_DESIGN |
| `PGA-TIR-03` bounded R2 result schema and non-invasive collection | PASS_CONDITIONAL_DESIGN |
| `PGA-TIR-04` authoritative effective frozen source and precedence | BLOCKED |
| `PGA-TIR-05` target-specific effective controlled amendment exact diff | BLOCKED |
| `PGA-TIR-06` actual named owner/grants, expiry/revocation and independent issuer verification | NOT_GRANTED |
| `PGA-TIR-07` actual policy denial / negative-canary evidence | NOT_PROVEN |
| `PGA-TIR-08` clinical, production, Spring, PHI, DB, live network and CI exclusions | PASS_SCOPE |
| `PGA-TIR-09` merge and Stage A1 implementation authority | NOT_AUTHORIZED |

## 4. Singular next action to unlock the design

Perform **`Stage A1 Frozen Authority Source Identification + Precedence Decision`**, not a second round of generic signatory prose:

1. Enumerate and read *actual controlling* design/owner/safety authorization documents across the **explicitly pinned applicable governance ref(s)**, including frozen enterprise branch if applicable. Log repo/path/ref/blob, exact section/field and source-of-authority for each.
2. Compare effective predecessor clauses against Stage A1 R2 grant draft `U07StageA1R2CollectionGrantV1` and the local profile. Determine whether Stage A1's nonclinical owner exception may be implemented as a new supplemental document, or only via controlled amendment of an already frozen owner/security clause.
3. Produce the smallest field-by-field authority precedence table: controlling authority, default/exception, independent signer retention, no-approver fail closed, affected Gate, revocation and expiry.
4. If no governing path/party can be established, keep `FROZEN_AUTHORITY_SOURCE_UNRESOLVED / NOT_READY`. Do not invent a more permissive source.
5. Only after that **new** exact-source review and separately authorized amendment could a local R2 read-only collection permit be issued. Synthetic fixture SETUP, canary and Spring still require further separate gates.

This is intended to avoid endless design loops: do not reopen already conditionally closed IR-02/03 absent changed semantics. Focus all subsequent work on IR-01 source-of-truth discovery and make a clear **found vs not found** decision.

## 5. Formal decision

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
PERSONAL_OWNER_GOVERNANCE_TARGETED_INDEPENDENT_DESIGN_RE_REVIEW
= PARTIAL_PASS / REVISE_REQUIRED_FOR_EFFECTIVE_AUTHORITY

AUTHOR_PR335_HEAD = cef1e77948403bdddab1371a2592115ce4b88365
AUTHOR_PR335_BLOB = a3e03b3ef621506294203562d9ba6fd9caaf524e
BASE_MAIN = 86e8843197091c8c8172b7e4213537a31bdf0654

RF-U07-A1-PGA-IR-01 = BLOCKER_OPEN / TARGET_DESIGN_NOT_FULLY_GROUNDED
RF-U07-A1-PGA-IR-02 = CLOSED_CONDITIONAL_DESIGN
RF-U07-A1-PGA-IR-03 = CLOSED_CONDITIONAL_DESIGN
FROZEN_GOVERNING_AUTHORITY_PRECEDENCE = UNKNOWN
PERSONAL_OWNER_PROFILE = PROPOSED_NOT_EFFECTIVE
PERSONAL_DOCKER_RUNNER = CANDIDATE_NOT_ATTESTED
R2_COLLECTION_AUTHORIZATION = NOT_GRANTED
STAGE_A1_IMPLEMENTATION = NOT_AUTHORIZED
STAGE_A1_FIXTURE_SETUP_OR_CANARY = NOT_AUTHORIZED
CLINICAL_AND_PRODUCTION = NOT_AUTHORIZED
FOUNDATION_AUDIT = NOT_PASSED
MERGE = NOT_AUTHORIZED
```

This re-review is an independent analytical report of pinned source, **not** a separately credentialed human Security/Foundation approval. No runtime command, host change, CI, testing or merge was performed.
