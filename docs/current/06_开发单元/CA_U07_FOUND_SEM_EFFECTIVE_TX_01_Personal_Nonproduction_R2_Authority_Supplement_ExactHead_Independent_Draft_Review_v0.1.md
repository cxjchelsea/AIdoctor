# CA-U07-FOUND-SEM-EFFECTIVE-TX-01 — Personal Nonproduction R2 Authority Supplement Exact-HEAD Independent Draft Review v0.1

> 2026-10-09 · independent **analytical** Draft scope/content review; not a credentialed Security/Foundation authority.
>
> Reviewed [PR #341](https://github.com/cxjchelsea/AIdoctor/pull/341) exact HEAD `ece0c12b4ab90c2629d315c7c9b81153196f18f5`, blob `c1098d2ac6a120cebd8e503725919486e8fd31e9`, vs. `main@86e8843197091c8c8172b7e4213537a31bdf0654`.
> Design [PR #339](https://github.com/cxjchelsea/AIdoctor/pull/339) @ `18ea53c653f3d805734ad7cdcdf814f7a9570c53`, design review [PR #340](https://github.com/cxjchelsea/AIdoctor/pull/340) @ `016cdc1f307c71e8ee6a292a3b7d5240c76245d7`.
>
> **Verdict: PASS / DRAFT_SCOPE_ONLY; EFFECTIVE_AUTHORITY = NOT_GRANTED.** This is acceptance of the **explicitly authorized Draft-only authoring scope and content**, **not** an owner/Security/Foundation signature, authorization to activate the proposed rule, R2 collection grant, Stage A1 canary authorization or merge decision.

## 1. Exact source verification

GitHub independently returned:
- `main` current commit `86e8843197091c8c8172b7e4213537a31bdf0654`.
- PR #341 state `OPEN`, `DRAFT=true`, base SHA `86e8843197091c8c8172b7e4213537a31bdf0654`, exact head `ece0c12b4ab90c2629d315c7c9b81153196f18f5`, one changed file and 147 additions.
- Exact changed path: `docs/current/06_开发单元/CA_U07_FOUND_SEM_EFFECTIVE_TX_01_StageA1_Personal_Nonproduction_R2_Authority_Supplement_v0.1.md`; source blob `c1098d2ac6a120cebd8e503725919486e8fd31e9` retrieved by `fetch_file` at this exact commit.
- PR #339 and PR #340 are still Draft with independently matched exact HEADs above. Their reviews are **design-only** and have no signed operational privilege.

The source expressly quotes the user's authorization as `AUTHORIZE DRAFT ONLY`, and disclaims **activation, R2 execution and merger**. That quotation is a provenance statement **for draft creation only**, not a transferable owner signature for later operations.

This review adds **only this separate Markdown review** to its own Draft PR; it does not modify or approve the original PR #341 changes for merge.

## 2. Targeted gate-by-gate independent assessment

| Gate | Requirement | Result | Evidence |
|---|---|---|---|
| `PR341-DIR-01` | Exact target HEAD/blob/base and single-file diff | **PASS** | Verified GitHub head, one path, blob readback |
| `PR341-DIR-02` | User authorizes only draft file creation | **PASS** | Exact user text cited in PR #341; scope boundary preserved |
| `PR341-DIR-03` | New file must remain NOT_EFFECTIVE and non-operational | **PASS** | Title `(DRAFT ONLY)`, YAML `proposal_status: DRAFT_ONLY_NOT_EFFECTIVE`, booleans for effective/grants all false |
| `PR341-DIR-04` | Personal, synthetic-only, nonclinical Stage A1 R2 scope | **PASS** | §1/§2; explicitly excludes real patients, PHI, existing workloads, Spring, live DB/network, R3/R4 |
| `PR341-DIR-05` | Independent Security, Foundation and evidence acceptance are not silently replaced | **PASS** | §3 signer separation, required independent signatures and fail-closed approver-unavailable case |
| `PR341-DIR-06` | No existing frozen rules overwritten or implied precedence | **PASS** | §1/§3 preserve effective higher-priority authorities; separate new Draft file only |
| `PR341-DIR-07` | Bounded R2 fixed-command / safe evidence inventory designed | **PASS_CONDITIONAL** | §4 references still-proposed PR #328 R2-01..10, distinguishes collector from target, requires argv and redaction validation |
| `PR341-DIR-08` | Expiry, revocation, drift and explicit evidence provenance | **PASS_DESIGN_ONLY** | §2/§5 include unset grant refs, validity fields and fail-closed conditions; no external signed provenance yet |
| `PR341-DIR-09` | Actual competent signer + supplement activation grant | **NOT_READY** | `governing_parent_effective_ref: UNRESOLVED_REQUIRED`, `effective_supplement_authorization_ref: NOT_GRANTED` |
| `PR341-DIR-10` | Actual R2 collection, isolation canary and merge permissions | **NOT_GRANTED** | All required booleans false; no run, no CI or source code change |

No blocking issue was found **within the narrow Draft-only authoring scope**. Unset authority fields are deliberate, correct fail-closed markers rather than text defects. They are **blocking for activation** and must remain unresolved until actual competent authorization occurs.

## 3. Contract diff and applicability findings

- The one allowed new document is the expected proposed supplement path; this is not the already established enterprise authorization document and has no policy effect simply because the filename says `Authority_Supplement`.
- Independent Security/Foundation signatory obligations continue; repository admin access is evidence of a GitHub permission, **not** independent safety approval.
- R2-01..05 introspection applies to the collector process; the two prior ephemeral local Docker samples are limited user-provided observations, not independently bound preventive-policy evidence or A1-N01..10 negative canary acceptance.
- The exact fixed `R2-01..10` command/profile authority is still a candidate in PR #328, not executable authorization. Pre-bootstrap isolation cases must retain their original negative Oracle, policy-origin attribution and separate grants.
- The user can later explicitly authorize an exact, separately reviewed next step, but this review has **no power to issue that consent on their behalf**.

## 4. Remaining activation blockers and decision boundaries

| Actual future action | Current verdict | Mandatory separate precondition |
|---|---|---|
| Accept PR #341 as a Draft-design artifact | **PASS / DRAFT_SCOPE_ONLY** | Already met by exact-source review; **no automatic merge** |
| Declare proposed supplement effective | **NOT_READY** | Actual competent governing owner approval; precise effective parent contract and conflict check; required independent signatures |
| Permit personal runner R2 read-only evidence collection | **NOT_GRANTED** | Effective accepted profile plus exact reviewed bounded commands, owner declaration and separate time-scoped R2 grant |
| Claim independent real-runner attestation | **NOT_PROVEN** | R2 collection under grant and separately accepted source/target provenance |
| Run fixture setup or A1-N01..N10 negative canaries | **NOT_AUTHORIZED** | Separate implemented and reviewed Stage A1 source, R3/R4 grants and independent Oracle |
| Merge any PR | **NOT_AUTHORIZED** | Later exact diff/CI/merge conditions and explicit user authorization; standard merge commit only |

**Next step:** an actual *owner/governance authorization readiness decision* tied to an authenticated competent signer and effective authority, not another automatically self-approved Draft. Where that role cannot be established, keep the supplement `DRAFT_NOT_EFFECTIVE` and R2 `NOT_GRANTED`. Do not repeat concept-level design reviews absent changes.

## 5. Formal exact-head verdict

```text
CA-U07-FOUND-SEM-EFFECTIVE-TX-01
PERSONAL_NONPRODUCTION_R2_AUTHORITY_SUPPLEMENT_EXACT_HEAD_INDEPENDENT_DRAFT_REVIEW
= PASS / DRAFT_SCOPE_ONLY

AUTHOR_PR341_HEAD = ece0c12b4ab90c2629d315c7c9b81153196f18f5
AUTHOR_PR341_BLOB = c1098d2ac6a120cebd8e503725919486e8fd31e9
BASE_MAIN = 86e8843197091c8c8172b7e4213537a31bdf0654
SCOPE_DIFF = ONE_MARKDOWN_ADD_ONLY
AUTHORIZATION_SOURCE = USER_EXPLICIT_AUTHORIZE_DRAFT_ONLY
DRAFT_CONTRACT_CONTENT = ACCEPTED_FOR_DRAFT_ONLY
EFFECTIVE_PERSONAL_SUPPLEMENT = FALSE
COMPETENT_SECURITY_FOUNDATION_APPROVAL = NOT_GRANTED
R2_COLLECTION_AUTHORIZATION = NOT_GRANTED
R3_SETUP = NOT_AUTHORIZED
R4_CANARIES = NOT_AUTHORIZED
STAGE_A1_IMPLEMENTATION = NOT_AUTHORIZED
FOUNDATION_AUDIT = NOT_PASSED
CLINICAL_AND_PRODUCTION = NOT_AUTHORIZED
MERGE = NOT_AUTHORIZED
```

This review reflects independent reasoning about GitHub-pinned files but **does not constitute** an independently credentialed human security audit or organizational approval.
