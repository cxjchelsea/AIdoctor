#!/usr/bin/env python3
"""Build machine-reviewable CD-07 non-production runtime evidence.

This tool summarizes verification facts only. It does not authorize merge,
production activation, clinical semantics, U04 execution, or real-patient use.
The generated file is intended to be retained in both the Actions artifact and a
persistent PR review record; the time-limited artifact alone is not treated as a
permanent evidence store.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import xml.etree.ElementTree as ET
from pathlib import Path

FROZEN_REFS = [
    "KR-U03-SOURCE-001@0.1.0-candidate",
    "RR-U03-RISK-001@0.2.1-candidate",
    "U03_D09_COVERAGE_V0_2_1_CANDIDATE",
    "PR-U03-D09-001@0.2.1-candidate",
    "PF-U03-C-POLICY-001",
]

MANDATORY_NEGATIVES = {
    "N1": "n1StaleClinicalStateVersionFailsBeforeC02AndCommit",
    "N2": "n2MissingReleaseRefFailsBeforeRuntimeOutput",
    "N3": "n3WrongReleaseRefFailsBeforeC02OrCommit",
    "N4": "n4CrossReleaseIncompatibleSetFailsBeforeRuntimeOutput",
    "N5": "n5DependencyFailureDoesNotInvokeD09CommitOrInventSafetyOutput",
    "N6": "n6MalformedC02ResultFailsBeforeD09ProposalOrCommit",
    "N7": "n7UnsupportedProposalTypeCannotCommit",
    "N8": "n8DuplicateEventWithDifferentFingerprintConflictsWithoutSecondCommit",
    "N9": "n9StateCommitterVersionConflictProducesNoMutationOrOutbound",
    "N10": "n10AttemptedDirectMutationBypassIsRejectedBeforeStateRepository",
    "N11": "n11ImplicitLatestReleaseSelectionIsRejectedBeforeRuntimeOutput",
    "N12": "n12ProductionEnvironmentUseIsRejectedBeforeC02OrCommit",
    "N13": "n13AttemptedU04OwnerExecutionWithoutAuthorizationFailsClosedWithNoOwnerCallOrOutput",
}

TRACE_RECONCILIATION_TEST = (
    "committedStateRemainsCommittedWhenTracePersistenceFailsAndNoNormalOutboundIsProduced"
)
RUNTIME_E2E_TEST = (
    "nonProductionRuntimeE2EBindsC02D09K09P01TraceAndOutboundWithoutU04Execution"
)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(65536), b""):
            digest.update(chunk)
    return digest.hexdigest()


def parse_bool(value: str, name: str) -> bool:
    normalized = value.strip().lower()
    if normalized == "true":
        return True
    if normalized == "false":
        return False
    raise SystemExit(f"{name} must be true or false, got {value!r}")


def read_surefire(report_dir: Path) -> tuple[dict, set[str]]:
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    passed_names: set[str] = set()
    files = sorted(report_dir.glob("TEST-*.xml"))
    if not files:
        raise SystemExit(f"no Surefire XML reports found in {report_dir}")

    for file in files:
        root = ET.parse(file).getroot()
        totals["tests"] += int(root.attrib.get("tests", "0"))
        totals["failures"] += int(root.attrib.get("failures", "0"))
        totals["errors"] += int(root.attrib.get("errors", "0"))
        totals["skipped"] += int(root.attrib.get("skipped", "0"))
        for case in root.findall(".//testcase"):
            if case.find("failure") is None and case.find("error") is None and case.find("skipped") is None:
                name = case.attrib.get("name")
                if name:
                    passed_names.add(name)
    return totals, passed_names


def load_gatec(bundle: Path) -> dict:
    data = json.loads(bundle.read_text(encoding="utf-8"))
    summary = data["summary"]
    expected = {
        "approved_golden_id_total": 31,
        "executable_golden_total": 30,
        "excluded_golden_ids": ["GC-026"],
        "golden_pass": 30,
        "golden_fail": 0,
        "approved_critical_safety_id_total": 20,
        "executable_critical_safety_total": 19,
        "excluded_critical_safety_ids": ["SS-012"],
        "critical_safety_pass": 19,
        "critical_safety_fail": 0,
        "failed_non_case_checks": [],
        "gate_c_execution_blocked_by_critical_failure": False,
    }
    for key, value in expected.items():
        if summary.get(key) != value:
            raise SystemExit(f"Gate-C regression mismatch for {key}: {summary.get(key)!r} != {value!r}")
    if data.get("production_state_mutation_capability") is not False:
        raise SystemExit("Gate-C regression bundle unexpectedly reports production mutation capability")
    if data.get("network_access_required") is not False:
        raise SystemExit("Gate-C regression bundle unexpectedly requires network access")
    return {"summary": summary, "sha256": sha256(bundle)}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--implementation-sha", required=True)
    parser.add_argument("--workflow-run-id", required=True)
    parser.add_argument("--run-attempt", required=True)
    parser.add_argument("--repository", required=True)
    parser.add_argument("--ref", required=True)
    parser.add_argument("--event-name", required=True)
    parser.add_argument("--environment-id", required=True)
    parser.add_argument("--real-patient-traffic-attested", required=True)
    parser.add_argument("--production-mutation-attested", required=True)
    parser.add_argument("--u04-execution-attested", required=True)
    parser.add_argument("--java-report-dir", type=Path, required=True)
    parser.add_argument("--gatec-bundle", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    real_patient_attested = parse_bool(
        args.real_patient_traffic_attested, "real-patient-traffic-attested"
    )
    production_mutation_attested = parse_bool(
        args.production_mutation_attested, "production-mutation-attested"
    )
    u04_execution_attested = parse_bool(
        args.u04_execution_attested, "u04-execution-attested"
    )
    if real_patient_attested or production_mutation_attested or u04_execution_attested:
        raise SystemExit("controlled CD-07 verification attestation must remain non-production / no-U04")

    totals, passed = read_surefire(args.java_report_dir)
    missing_negatives = {
        case: name for case, name in MANDATORY_NEGATIVES.items() if name not in passed
    }
    if missing_negatives:
        raise SystemExit(f"mandatory negative tests missing/not passed: {missing_negatives}")

    if RUNTIME_E2E_TEST not in passed:
        raise SystemExit("NON_PRODUCTION_RUNTIME_E2E test missing or not passed")
    if TRACE_RECONCILIATION_TEST not in passed:
        raise SystemExit("trace-failure reconciliation test missing or not passed")

    if totals["failures"] or totals["errors"]:
        raise SystemExit(f"Java verification has failures/errors: {totals}")

    gatec = load_gatec(args.gatec_bundle)

    evidence = {
        "schema": "u03-cd07-runtime-verification-evidence-v2",
        "verification_label": "NON_PRODUCTION_RUNTIME_E2E",
        "retention_model": {
            "actions_artifact_is_time_limited_copy": True,
            "persistent_review_record_required": True,
            "persistent_review_record_location": "PR_CONVERSATION_RECORD",
        },
        "governance_disclaimer": {
            "runtime_verified_does_not_mean_clinically_evaluated": True,
            "clinical_evaluation_does_not_mean_production_authorized": True,
            "merge_authorization_granted": False,
            "production_authorization_granted": False,
            "u04_implementation_authorized": False,
        },
        "E1_exact_implementation_commit_sha": args.implementation_sha,
        "E2_test_evaluation_code_identity": {
            "same_repository_sha": args.implementation_sha,
            "negative_test_classes": [
                "U03FailClosedNegativeCoverageTest",
                "U03UnauthorizedU04AttemptTest",
            ],
            "trace_reconciliation_test_class": "U03PostCommitFinalizerTest",
            "runtime_e2e_test_class": "U03NonProductionRuntimeE2ETest",
            "gate_c_harness": "tools/u03_gatec_eval",
        },
        "E3_runtime_environment_identity": {
            "environment_id": args.environment_id,
            "binding_mode": "EXPLICIT_NON_PRODUCTION_BINDING_ONLY",
            "execution_kind": "GITHUB_ACTIONS",
            "repository": args.repository,
            "ref": args.ref,
            "event_name": args.event_name,
            "workflow_run_id": args.workflow_run_id,
            "run_attempt": int(args.run_attempt),
        },
        "E4_exact_governed_release_refs": FROZEN_REFS,
        "E5_command_workflow_invocation_provenance": {
            "workflow_run_id": args.workflow_run_id,
            "run_attempt": int(args.run_attempt),
            "implementation_sha": args.implementation_sha,
            "event_name": args.event_name,
        },
        "E6_test_inventory_and_results": {
            "surefire_totals": totals,
            "runtime_e2e": {"test": RUNTIME_E2E_TEST, "status": "PASS"},
            "trace_failure_reconciliation": {
                "test": TRACE_RECONCILIATION_TEST,
                "status": "PASS",
            },
            "gate_c_regression": {
                "status": "PASS",
                "bundle_sha256": gatec["sha256"],
                "golden_pass": gatec["summary"]["golden_pass"],
                "critical_safety_pass": gatec["summary"]["critical_safety_pass"],
            },
        },
        "E7_negative_test_results": {
            case: {
                "test": name,
                "status": "PASS",
                "proof_kind": "verification_harness" if case == "N13" else "runtime_or_contract_test",
            }
            for case, name in MANDATORY_NEGATIVES.items()
        },
        "E8_non_production_e2e_result": {
            "status": "PASS",
            "label": "NON_PRODUCTION_RUNTIME_E2E",
            "machine_verified": {
                "synthetic_test_inputs": True,
                "mechanical_in_memory_state_repository": True,
                "post_commit_trace_gate_exercised": True,
                "u04_consumer_not_wired_in_v8": True,
            },
        },
        "E9_trace_provenance_summary": {
            "correlates": [
                "Clinical State Version",
                "Thread/Run/Event",
                "accepted evidence/provenance",
                "exact governed release refs",
                "C02 result",
                "D09 decision",
                "K09 proposal",
                "StateCommitter result",
                "P05 trace identity",
                "S14 outbound identity",
            ],
            "trace_is_clinical_truth": False,
            "trace_failure_behavior_machine_verified": {
                "committed_state_not_rewritten": True,
                "normal_s14_handoff_withheld": True,
                "operational_reconciliation_required": True,
            },
        },
        "E10_no_production_mutation_or_real_patient_traffic": {
            "machine_verified": {
                "N12_production_environment_rejected_before_C02_or_commit": True,
                "N13_unauthorized_U04_owner_attempt_rejected_before_owner_call_or_output": True,
                "v8_uses_mechanical_in_memory_repository": True,
                "gate_c_bundle_network_access_required": False,
                "gate_c_bundle_production_state_mutation_capability": False,
            },
            "execution_attestations": {
                "environment_id": args.environment_id,
                "real_patient_traffic_used": real_patient_attested,
                "production_mutation_used": production_mutation_attested,
                "u04_execution_used": u04_execution_attested,
            },
            "evidence_limitations": [
                "Workflow environment declarations are execution attestations, not independent proof of external infrastructure state.",
                "V8 proves the checked repository wiring uses synthetic inputs and an in-memory mechanical state repository; it does not claim visibility into systems outside this workflow run.",
            ],
        },
        "E11_known_exclusions_and_residual_risks": [
            "U04 implementation and consumer-side Safety Gate behavior remain unauthorized and unverified.",
            "N13 uses a verification harness because CD-07 intentionally contains no authorized U04 consumer/runtime wiring.",
            "U14 routing is excluded.",
            "Real-patient traffic and production Clinical State mutation are excluded.",
            "Candidate releases remain NOT_PUBLISHED / NOT_ACTIVE_FOR_PRODUCTION.",
            "This evidence proves runtime wiring/guard behavior only; it does not replace Gate-C clinical evaluation evidence.",
            "The Actions artifact is a 90-day retained copy; E1-E11 must also be copied verbatim into a persistent PR review record.",
            "Independent implementation/evidence review and explicit Merge Authorization remain required.",
        ],
    }

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(evidence, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    print(json.dumps({
        "output": str(args.output),
        "sha256": sha256(args.output),
        "java_tests": totals,
        "negative_cases": len(MANDATORY_NEGATIVES),
        "trace_failure_reconciliation": "PASS",
        "gate_c_regression": "PASS",
        "runtime_e2e": "PASS",
        "persistent_review_record_required": True,
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
