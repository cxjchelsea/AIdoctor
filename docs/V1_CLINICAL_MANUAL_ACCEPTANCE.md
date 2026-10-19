# V1 Clinical Agent — Nonproduction Manual Acceptance

> Status: AUTOMATED BASELINE GREEN; FULL HTTP/MYSQL TWO-TURN E2E NOT ACCEPTED.
> Scope: PR #507, synthetic inputs only. Not a patient-facing release.

## What is implemented

- Opt-in `POST /internal/nonproduction/v1/clinical/start-through-wait`, which coordinates the existing U01/U02/U03/U04 and U05/U06/durable-WAIT owners.
- Opt-in `POST /internal/nonproduction/v1/clinical/answer-through-u04`, which consumes an independently authenticated WAIT answer through F8, commits U02, then invokes governed U03 risk replacement and U04 safety re-evaluation.
- A separate browser test route `/test/v1-clinical` in `frontend/`. It displays actual HTTP results or failures. It does **not** yet display a patient-facing selected question.
- Diagnostics `GET /internal/nonproduction/v1/clinical/runtime-readiness`, gated by `readiness-diagnostics-enabled=true`. Presence does not constitute authorization or E2E evidence.

## Verified baseline

PR #507 exact commit `94af8bf10da9f05393839ae20f14ff3b53bcda3c`:
7 GitHub Actions workflows successful: Foundation-0; V1 Clinical Agent Public Entry; Trusted U01/U02 MySQL 8 Physical Readback; U07 Limited Regression; two U07 D5 workflows; V1 Clinical Synthetic Manual Frontend Build.

These are **not** proof of a live browser-to-MySQL full first-round WAIT or resumed second-round U04.

## Mandatory independent host dependencies

Before the HTTP route can perform an accepted clinical round, install independent, nonproduction implementations for:
1. Clinically authorized U03 command and frozen release / Gate-C scope (`AuthorizedClinicalHost` and resumed `ResumedRiskOwner`).
2. F3/A1 current-run ownership, governed U05/U06 question decision (`AuthorizedQuestionHost`), authoritative state reference and independently confirmed delivery (`FirstRoundOwner`).
3. Independently verified delivery receipt and durable WAIT authority backed by database tables (no forged receipt).
4. Independently authenticated/canonicalized F8 answer and digest (`TrustedAnswerHost`), durable F8 consumption schema and trusted new Run/Event.
5. Real C01 gateways, accepted-evidence attestation, StateCommitter, trusted trace and clinical repositories.

Do not implement these as production defaults or by accepting caller-provided authorizations. Enable only under a nonproduction profile with disposable synthetic records. Controller-level conditional bean registration must be validated in the actual Spring context.

## Manual local testing once the host is installed

1. Start a MySQL 8 test database with compatible migrations, then run the diagnosis-service Spring Boot application under a dedicated **nonproduction** profile.
2. Enable (only in that environment):
   - `aidoctor.v1.clinical-e2e.readiness-diagnostics-enabled=true`
   - `aidoctor.v1.clinical-e2e.u01-u04-http-enabled=true`
   - `aidoctor.v1.clinical-e2e.first-round-wait-http-enabled=true`
   - `aidoctor.v1.clinical-e2e.answer-resume-http-enabled=true`
   - `aidoctor.v1.clinical-e2e.answer-risk-http-enabled=true`
3. Confirm diagnostics report the required beans. The diagnostics response explicitly never claims full clinical E2E verification.
4. From `frontend/`, run `npm ci` and `npm run dev`; open `http://localhost:3000/test/v1-clinical` (Vite proxy expects diagnosis-service on localhost:8084).
5. With **synthetic** users and symptoms, request `start-through-wait`; inspect actual persisted CDP versions, U06 selection, delivered receipt, Consultation WAIT, thread WAIT, and checkpoint consistency. HTTP success alone is insufficient.
6. Submit a synthetic answer through `answer-through-u04`; verify F8 checkpoint consumption, new Event/Run, U02 clinical fact, replaced U03 risk, and committed U04. Reject replay/mismatched user, checkpoint, thread, version or release.
7. Confirm no route is enabled on production profiles; do not use patient data.

## Outstanding acceptance blockers — must close before full manual acceptance or merge request

- A **single running HTTP + MySQL** integration test for first-round success including independent trusted owners and durable WAIT readback.
- A second actual HTTP round through F8/U02/U03/U04 with clinical state/version readback and negative replay tests.
- A verified local nonproduction host configuration and real Spring bean activation; absent authorities must remain unavailable.
- A patient-visible question payload, if the intended acceptance is natural-language back-and-forth rather than diagnostic-only checkpoint testing.

No merge to main is authorized by this document. Production use remains prohibited.
