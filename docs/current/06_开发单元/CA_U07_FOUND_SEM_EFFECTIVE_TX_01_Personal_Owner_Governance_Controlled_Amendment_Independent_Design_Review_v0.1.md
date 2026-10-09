# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Personal Owner Governance Controlled Amendment Independent Design Review v0.1

> Date: 2026-10-09. Independent **analytical** review, not an organizational Security / Foundation signature.
>
> **Review subject** [PR #335](https://github.com/cxjchelsea/AIdoctor/pull/335), exact author HEAD `5ba1abfe963af3ccc615d8c032ba1904916527ce`, document blob `71fbb6415c09ce1e289b707791e5057f6009ec70`.
>
> Baseline `main@86e8843197091c8c8172b7e4213537a31bdf0654`. Prerequisites [PR #333](https://github.com/cxjchelsea/AIdoctor/pull/333) @ `c51f3f2f3d60613864be03ede4b4fd2b3d85aa30` and [PR #334](https://github.com/cxjchelsea/AIdoctor/pull/334) @ `face63327e172405cc84b5ae8cde57df9b6da308`.
>
> **Verdict: REVISE_REQUIRED / CONCEPTUALLY_ACCEPTABLE_BUT_CONTROLLED_AMENDMENT_NOT_READY.** No Stage A1 runner attestation, R2 grant, governed-contract edit, actual canary execution, clinical authority or merge.

## 1. Source verification, evidence boundaries

GitHub readback in this review:
- PR #335 is one ADD-only Markdown file relative to `main@86e8843197091c8c8172b7e4213537a31bdf0654`; independently fetched exact blob `71fbb6415c09ce1e289b707791e5057f6009ec70`.
- PR #326 exact HEAD `6884c26e93855b97a5f1293fe81c3fa5f524a739` changed the design file `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_StageA1_Controlled_ExactDiff_Amendment_Runner_Authority_Evidence_Plan_v0.1.md`.
- PR #328 exact HEAD `3f6725f387105798d2fa4ba6cefb1b43a69a0360` changed `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_StageA1_Runner_Nomination_R2_Attestation_Authorization_Readiness_v0.1.md`.
- These are **draft candidate files on separate PR branches**: they cannot be described as already amended effective authority on `main`. No independent readback of an authoritative *frozen governing* contract path/paragraph/blob has yet established the target for this proposed change.
- The personal workstation Linux-container observations were supplied by the user. They indicate feasibility (`seccomp=2`, nonroot, `--network none` loopback only, readonly root, capabilities zero) but not signed physical security enforcement or negative-test outcome.

This review assesses the **actual frozen author design** rather than an amended version. No code, host, Docker, Java, service, governance contract or CI workflow has been edited.

## 2. Gate-by-gate design assessment

| Gate | Criterion | Result | Rationale |
|---|---|---|---|
| PGA-IR-01 | Exact author HEAD + blob + one-file diff and base | **PASS** | GitHub readback verified author file and two proposed related PR paths |
| PGA-IR-02 | Applicable environment and exclusion scope | **PASS** | Personal, synthetic-only, nonclinical Stage A1 R2 only; clinical/production/PHI/Spring/database excluded |
| PGA-IR-03 | Roles: owner vs collector vs independent evidence acceptor | **PASS_WITH_RESERVATION** | Developer may own/consent on their own asset; separate evidence acceptance preserved, but reviewer credential/effective authority not grounded |
| PGA-IR-04 | Fail-closed grants, expiry, drift and revocation | **PASS_CONCEPTUAL** | Clear separate grants and drift stop; required concrete validity, identity and revocation records are not yet available |
| PGA-IR-05 | Exact effective frozen contractual target/path/blob and clause | **FAIL / BLOCKER** | No verified authoritative frozen target: amendment cannot modify only historical PR #326/#328 drafts and claim the effective gate changed |
| PGA-IR-06 | Approval/signature authority substitution per exact governing clause | **FAIL / BLOCKER** | Proposal says Security/Foundation signatures cannot be assumed, but does not state which original named approvals are mandatory, which may be replaced, who may authorize the exception, and what result if independent signer does not exist |
| PGA-IR-07 | Executable R2 exact command profile and safe data disclosure | **NOT_READY / FOLLOW-ON** | Described bounded outputs, but exact command allowlist, effective local collection credential and independent provenance are not established; do not infer from previous exploratory runs |
| PGA-IR-08 | Preserve negative Oracle and Docker-to-VM escalation | **PASS_CONCEPTUAL** | Clearly prohibits unreachable/no-listener/DNS-absent becoming `DENIED_BY_POLICY` |
| PGA-IR-09 | Controlled scope only, no implicit Stage A1 code/canary/clinical grant | **PASS** | All effective authority remains explicitly ungranted |

## 3. Required findings

### RF-U07-A1-PGA-IR-01 — Effective frozen authority target not verified (BLOCKER)

**Observed:** §7 of PR #335 provides an impact inventory with verified *draft design* file names from PR #326/#328, but explicitly leaves the effective frozen Runner/Stage A1 authority contract as `BLOCKED_UNTIL_EXACT_PATH_BLOB_IDENTIFIED`.

**Risk:** Updating the mentioned draft documents could give the appearance that local R2 owner consent is authorized while the actual frozen controlling contract still requires different Security/Foundation signers. Main/PR differences and transitive gates can lead to contradictory authority.

**Required targeted remediation:**
1. Read and pinpoint the authoritative current frozen authority contract, full repository path, effective branch and **exact blob SHA**, with the exact paragraph/field requiring approval. Check whether controlling text is `main`, a frozen enterprise baseline or another pinned binding source; do not guess a path.
2. Map every affected gate to a **concrete field-level amend/leave-unchanged** operation, with version-specific precedence and a conflict/fail-closed rule.
3. Present a narrow exact-diff inventory for **only** effective governing artifacts, not a blanket edit to all design-history PRs.
4. If the authoritative frozen contract cannot be found or read, **stop with NOT_READY** and do not issue the governance amendment, R2 grant or merged policy.

### RF-U07-A1-PGA-IR-02 — Personal consent cannot silently replace independent required signers (BLOCKER)

**Observed:** PR #335 proposes `LOCAL_ENVIRONMENT_OWNER` and `LOCAL_COLLECTION_CONSENT_SIGNER` both held by the developer and retains independent review. However, the proposed local profile is not yet mapped to the exact required approvals in the existing R2 authorization chain.

**Risk:** A design-only reviewer, an AI-generated report or an owner declaration could be mistaken for enterprise Security/Foundation-U01 signatures.

**Required targeted remediation:**
1. Create a **per-gate / per-signature decision matrix**: original authority, required independence, proposed personal substitute, legal/governance permitting clause, authorizing party, forbidden substitution, and fallback when unavailable.
2. Allow personal owner consent only for **asset ownership and explicitly permitted, synthetic, local R2 read-only collection**; preserve a separately identified independent reviewer for evidence acceptance and original authorizing owners for changes to a frozen Core/clinical contract.
3. Define the `NO_INDEPENDENT_APPROVER_AVAILABLE` branch: explicitly `NOT_READY` for requirements that require such an approver; do not simulate a signature. An AI-authored technical analysis is not automatically an independent organizational/security signature.
4. Bind any future signed consent to named personal asset alias, Docker engine/context/runtime/image identity, approved exact argv, permitted outputs, explicit issue/expiry/revocation and no-PHI scope.

### RF-U07-A1-PGA-IR-03 — Temporal evidence and target process binding incomplete (REQUIRED before R2)

**Observed:** User provided results from **two distinct auto-removed containers**, each with a different introspection invocation; PR #335 contains no collector provenance from a single runner-bound, time-stamped test instance. It recognizes the limitation but does not provide an operative noninvasive capture manifest.

**Required targeted remediation:** Define a reviewed R2-only metadata record, without general Docker socket mounting or host/other workloads enumeration, that captures a safe per-attempt runner alias, collector identity, image ID, engine/kernel/runtime/profile, exact argv digest, container identifier sanitized according to the frozen disclosure contract, invocation time, expected keys, no-raw-output guarantee, log retention/ACL, drift invalidation and independent readback signature. **Do not** expand previous exploratory output into retroactive authorization.

## 4. Accepted portions (not reopening)

- Personal ownership does not require a fictional corporate server or new VM **for initial local candidate qualification**.
- Multiple authorization scopes remain distinct: owner declaration ≠ scoped R2 inventory ≠ Stage A1 fixture/canary ≠ production/clinical.
- Synthetic-data-only, no Windows host binds, Docker socket, privileged mode, host network or existing database/health-service integration.
- Proven process flags are **environment observations** only; `--network none` is not evidence of policy-origin network denial, and seccomp filter mode does not prove exact rule semantics.
- `SA-BF-01/02` and the original A1-N01..10 Oracle remain open; failure to obtain independent rule-origin proof requires `INCOMPLETE_EVIDENCE`, possibly a dedicated Linux VM for only those missing cases.

## 5. Targeted re-review criteria

A next exact-HEAD re-review can pass controlled amendment **design readiness** when:
1. `RF-U07-A1-PGA-IR-01`: exact source paths, controlling status, blobs and clause-to-diff inventory verified against the intended authoritative baseline.
2. `RF-U07-A1-PGA-IR-02`: explicit original/substitute signer matrix and no-self-independence rule mapped to those actual clauses, with no-approver = NOT_READY.
3. `RF-U07-A1-PGA-IR-03`: command/collector/evidence-bound minimal record and provenance requirements designed, distinct from any execution permission.
4. Author PR HEAD/blob reverified after remediation and **only then** design re-review and separately controlled amendment approval. No inference that a review PASS becomes an R2 grant or owner signature.

This is narrower than restarting U07 architecture or demanding an external VM immediately. Aim to resolve specific governance authority targets and reach a genuinely executable R2 read-only decision.

## 6. Formal decision

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
PERSONAL_OWNER_GOVERNANCE_CONTROLLED_AMENDMENT_INDEPENDENT_DESIGN_REVIEW
= REVISE_REQUIRED / CONCEPTUALLY_ACCEPTABLE_BUT_NOT_IMPLEMENTABLE

AUTHOR_PR335_HEAD = 5ba1abfe963af3ccc615d8c032ba1904916527ce
AUTHOR_PR335_BLOB = 71fbb6415c09ce1e289b707791e5057f6009ec70
BASE_MAIN = 86e8843197091c8c8172b7e4213537a31bdf0654
RF-U07-A1-PGA-IR-01 = BLOCKER
RF-U07-A1-PGA-IR-02 = BLOCKER
RF-U07-A1-PGA-IR-03 = REQUIRED_BEFORE_R2
LOCAL_PERSONAL_CANDIDATE = ELIGIBLE / NOT_ATTESTED
GOVERNANCE_PROFILE = DESIGN_ONLY / NOT_EFFECTIVE
GOVERNANCE_AMENDMENT_AUTHORIZATION = NOT_GRANTED
R2_COLLECTION_AUTHORIZATION = NOT_GRANTED
RUNNER_SECURITY_ATTESTATION = NOT_PROVEN
A1_IMPLEMENTATION_AND_CANARIES = NOT_AUTHORIZED
STAGE_B / CLINICAL / PRODUCTION = NOT_AUTHORIZED
FOUNDATION_AUDIT = NOT_PASSED
MERGE = NOT_AUTHORIZED
```

**Review authorship limitation:** Independently reasoned about a pinned author artifact and linked GitHub diffs, but this assistant cannot purport to be a separately credentialed human Security/Foundation approver. No in-person host attestation or local command execution occurred.
