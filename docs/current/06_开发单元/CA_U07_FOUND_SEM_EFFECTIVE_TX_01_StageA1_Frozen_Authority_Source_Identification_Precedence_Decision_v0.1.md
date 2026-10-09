# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Stage A1 Frozen Authority Source Identification + Precedence Decision v0.1

> Date 2026-10-09. Repo: cxjchelsea/AIdoctor. Exact main `86e8843197091c8c8172b7e4213537a31bdf0654`. Evidence-only analytical decision; **NOT** a signed governance amendment or execution authorization.
> Prior design PR #335 exact amended HEAD `cef1e77948403bdddab1371a2592115ce4b88365`, blob `a3e03b3ef621506294203562d9ba6fd9caaf524e`; independent PR #337 exact HEAD `8eea5bdbd4e1ce70cfd4181196b408fcdb4bafc3`, blob `27d8d2bad57a78be91cd0e85f9894f1481b03478`.
> **Source outcome: DEFAULT_BRANCH_STAGE_A1_LOCAL_R2_SIGNER_AUTHORITY_NOT_ESTABLISHED.**
> **Precedence decision: DO_NOT_OVERRIDE; propose new narrow supplemental personal-nonclinical R2 authority only through an independently authorized, exact-target controlled amendment.** This is a **source and precedence analysis**, not approval to implement a new source.

## 1. Discovery method and provenance

- GitHub `GET /repos/cxjchelsea/AIdoctor/git/trees/86e8843197091c8c8172b7e4213537a31bdf0654?recursive=1` returned 2639 entries. Enumerated all 244 blob files in `docs/current/06_开发单元` and matching `CA_U07_FOUND_SEM_EFFECTIVE_TX_01_*` paths, not merely search snippets.
- Main has `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_Tier0_Producer_Contract_Manifest_v0.1.md`, blob `98189638619aa78e997a1a588239b36b89617378`. Read its actual content: authorized **SOURCE_ONLY** static/Tier-0 implementation inventory and explicit `U07 Implementation Authorization = NOT_GRANTED`; **not** an A1 Runner/R2 owner signature override. Its exact authority concerns static producer evidence, not a Linux runner permission.
- Retrieved [enterprise branch](https://github.com/cxjchelsea/AIdoctor/tree/agent/enterprise-agent-refactoring-plan) SHA `8cedbdd8d34b074d326243b4892cfc523342b346`, tree 2035 entries, `truncated=false`. Relevant files include `docs/refactoring/plans/phase-a/engineering-baseline/phase-a-engineering-freeze-record-v1.md` blob `da5768cc1fa01ab3d54e974afb368d5f6b257eb5` and `docs/refactoring/plans/phase-a/a7-cl/a7-cl-clinical-governance-charter.md` blob `07e770a905c6c481b04f0fc8e2356d2640c5721d`. Read relevant content: they concern Phase-A freeze and clinical governance; **neither provides identified Stage A1 personal Linux runner signer-substitution language**. The former expressly describes durable freeze requiring independent review/standard merge/PMV in its own historical context.
- Independent exact PR path verification: [PR #326](https://github.com/cxjchelsea/AIdoctor/pull/326) @ `6884c26e93855b97a5f1293fe81c3fa5f524a739`, file blob `72b71011331c6c8e36f2a1330d36f5bad1c786ab`; [PR #328](https://github.com/cxjchelsea/AIdoctor/pull/328) @ `3f6725f387105798d2fa4ba6cefb1b43a69a0360`, file blob `2518bdb4e0a239abf70c84f32b24ccba80c713bd`. They contain explicit infrastructure owner, Security and Foundation/U01 approval assumptions, separate R2/SETUP/CANARY gates and commands, but are **designs on open Draft PR branches**, not currently merged authority.
- Related independent review [PR #330](https://github.com/cxjchelsea/AIdoctor/pull/330) @ `e1ebaa527f95b41ec1611b2bc300872d45762882` confirms scoped command **design** acceptance, not a real signer.

**Negative finding is scoped**: no authoritative Stage A1 personal-owner R2 signer exception was found in inspected `main` U07 files or relevant enterprise historical freeze/clinical records. This does **not** prove absence of every possible undocumented authority elsewhere. No missing document or owner is fabricated.

## 2. Source-of-truth matrix and precedence decision

| Source (exact ref/blob where verified) | What governs | Stage A1 R2 authority effect | Precedence conclusion |
|---|---|---|---|
| `main@86e8843197091c8c8172b7e4213537a31bdf0654` Tier-0 Producer Contract Manifest @ `98189638619aa78e997a1a588239b36b89617378` | Source-only Tier-0 producer and evidence claims | **NONE** for personal-runner signer substitution | Cannot be amended or used to grant R2 |
| Historical enterprise Phase-A Engineering Freeze @ `8cedbdd8…` / `da5768cc…` | Engineering baseline change-gate history | No identified personal Stage A1 R2 exception | Preserve historic constraints where applicable; do not infer personal signature delegation |
| Historical enterprise A7-CL Clinical Governance Charter @ `8cedbdd8…` / `07e770a…` | Clinical scope/authority | **NONE** for personal synthetic Runner; clinical approvals remain independent | Cannot be used as personal security approval |
| PR #326 Stage A1 controlled diff runner plan @ `72b71011331c6c8e36f5bad1c786ab` | Candidate stage R0..R4 approval separation | Design only; no grant | Can inform new supplemental contract, cannot override effective policy |
| PR #328 Stage A1 R2 authorizer readiness @ `2518bdb4e0a239abf70c84f32b24ccba80c713bd` | Candidate R2-only owner/Security/Foundation signature and offline attestation | Design only; `NOT_GRANTED` | Use its exact schema/constraints as design source; never treat as signed acceptance |
| PR #333 owner Docker baseline / #334 suitability / #335 amended design / #337 re-review | Local feasibility and adaptation candidates | No independent attestation or governance grant | Background/supporting records only |

**Deterministic fail-closed order**:
1. Explicitly applicable, accepted **effective frozen** owner/Security/Foundation contract for an operation, if found, prevails over every draft or local owner declaration.
2. Accepted, version-bound **supplemental personal Stage A1 R2-only contract** may apply **only** if a competent governance owner explicitly approves its creation, source hierarchy, and nonclinical scope and no higher-priority prohibition conflicts. No such effective supplemental contract exists yet.
3. PR #326/#328/#333–#337 designs and user CMD observations are evidence/planning inputs, **no executable authority**.
4. Any ambiguity, unavailable required independent approver, mismatch, drift, expiry, or conflicting source => `NOT_READY`, no R2, setup or canary.

## 3. Exact forward decision: supplement vs override

**Selected recommendation: NEW SUPPLEMENTAL PROFILE, NOT SILENT EDIT OF UNKNOWN FROZEN CONTRACT.**

Potential new authoritative artifact path, **proposed only**, based on repository conventions:
`docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_StageA1_Personal_Nonproduction_R2_Authority_Supplement_v0.1.md`

At most one **new** controlled authority document under an expressly granted scope. Do not edit PR #326/#328 as if they were main frozen contracts. Do not overwrite main Tier-0 static manifest. A first proposal must define:
- `authority_parent_ref` pointing to verified competent governance approval (MUST NOT be `UNKNOWN`);
- exact `profile_id=PERSONAL_OWNER_LOCAL_NONPRODUCTION`, Stage A1 nonclinical synthetic and read-only **R2-only**, named personal asset/owner, context/engine/runtime/image identity and expiry;
- the **owner may consent to inspect their own asset**, while Security/Foundation/U01 signer requirements cannot be silently replaced by self-signature or AI analysis. Approval substitution, if any, must be reviewed and expressly approved by the governing authority;
- reviewed narrow exact-command allowlist/typed output/retention/independent readback and no raw terminal/daemon data disclosure;
- fail closed on unknown signature/owner, no independent reviewer, identity drift, absent attestation, unapproved argv/phase or broken trace binding;
- **non-inheritance:** local R2 owner consent neither signs independent evidence acceptance nor grants Stage A1 implementation, R3 setup, R4 negative canaries, Stage B Spring, PHI, clinical/production, CI or merges.

### Smallest explicit future diff inventory

| Item | Proposed change | Present authorization |
|---|---|---|
| One named nonproduction Stage A1 R2 supplemental authority Markdown file | ADD_ONLY after competent explicit change authority verified and granted | **NOT_AUTHORIZED** |
| PR #335 design | Leave unchanged; cite exact remedial author HEAD as proposal | NONE |
| PR #326/#328 Draft plans | Do not edit until separate targeted approval and target reconciliation | NONE |
| main Tier-0 static evidence contract | NO_CHANGE | N/A |
| Clinical/production/PHI/Java/CI/DB/runtime code | NO_CHANGE | NONE |

**Decision not to be confused with approval:** The source analysis selects how an acceptable future amendment **should** be structured, not whether the user/assistant can now create an effective governance contract bypassing missing independent signers. The issue that requires a real owner/security authority decision remains explicit and narrow. This is a stopping condition, not permission to keep generating self-approving Draft PRs.

## 4. IR-01 conclusion and remaining blocker

```text
RF-U07-A1-PGA-IR-01_SOURCE_DISCOVERY = COMPLETED_FOR_INSPECTED_REFS
RF-U07-A1-PGA-IR-01_AUTHORITY_EFFECTIVENESS = BLOCKED_ON_APPROVAL
FROZEN_SOURCE_FOR_PERSONAL_STAGE_A1_R2 = NOT_IDENTIFIED_AS_EXISTING_EFFECTIVE_EXCEPTION
PRECEDENCE_DECISION = PRESERVE_EFFECTIVE_HIGHER_PRIORITY_CONTRACT; PROPOSE_NEW_SCOPED_SUPPLEMENT
COMPETENT_SUPPLEMENT_AUTHORIZER = NOT_IDENTIFIED_OR_APPROVED
SIGNED_SUPPLEMENT = NOT_PRESENT
PERSONAL_LOCAL_ASSET_OWNER = CANDIDATE_NOT_ATTESTED
R2_COLLECTION_GRANT = NOT_GRANTED
STAGE_A1_POLICY_DENIAL = NOT_PROVEN
STAGE_A1_IMPLEMENTATION = NOT_AUTHORIZED
SETUP_CANARY_STAGE_B_CLINICAL_PRODUCTION = NOT_AUTHORIZED
MERGE = NOT_AUTHORIZED
```

## 5. Next concrete action (no repetitive generic re-review)

**One actionable upstream blocker:** acquire explicit competent owner authorization for the **specific new supplemental nonclinical R2-only authority profile** with its signer non-substitution rule; if such authority cannot be supplied, keep `NOT_READY`. In parallel, the owner can continue ordinary local nonclinical exploratory Docker introspection **outside the formally authorized R2 evidence and Stage A1 canary pipeline**, labeling those observations as exploratory and without treating them as test acceptance or governance closure.

No local workstation/CI access was exercised by this report. No governance contract is amended here. Separate security/human approval may be required by the project's adopted frozen policy.
