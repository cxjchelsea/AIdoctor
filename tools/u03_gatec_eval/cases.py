from __future__ import annotations

from copy import deepcopy
from typing import Any, Dict, List

from evaluator import CURRENT_REFS

BASELINE_ABSENT = {
    "EV-RF-RESP-001": "ABSENT",
    "EV-MNM-NEURO-001": "ABSENT",
    "EV-MNM-NEURO-002": "ABSENT",
    "EV-MNM-CARD-001": "ABSENT",
    "EV-RF-ALLERGY-001": "ABSENT",
}

SEPSIS_NORMAL = {
    "respiratory_rate_bpm": {"state": "PRESENT", "value": 18},
    "systolic_bp_mmHg": {"state": "PRESENT", "value": 120},
    "usual_systolic_bp_mmHg": {"state": "PRESENT", "value": 120},
    "heart_rate_bpm": {"state": "PRESENT", "value": 80},
}


def base_fixture() -> Dict[str, Any]:
    return {
        "release_refs": deepcopy(CURRENT_REFS),
        "clinical_state_version": "CURRENT",
        "age": 30,
        "pregnancy_or_puerperium": "FALSE",
        "pediatrics": False,
        "region_scope_ok": True,
        "channel_scope_ok": True,
        "setting": "SOURCE_SUPPORTED_COMMUNITY",
        "suspected_sepsis": "FALSE",
        "dyspnoea_context": "FALSE",
        "evidence": deepcopy(BASELINE_ABSENT),
        "measurements": deepcopy(SEPSIS_NORMAL),
    }


def sepsis_fixture() -> Dict[str, Any]:
    f = base_fixture()
    f["suspected_sepsis"] = "TRUE"
    f["evidence"].update({"EV-RF-APPEAR-001": "ABSENT", "EV-RF-SEPSIS-001": "ABSENT"})
    return f


def p4_both_families_not_applicable_fixture() -> Dict[str, Any]:
    f = base_fixture()
    f["suspected_sepsis"] = "FALSE"
    f["dyspnoea_context"] = "FALSE"
    return f


def p4_dyspnoea_applicable_fixture() -> Dict[str, Any]:
    """Baseline all NO_MATCH; dyspnoea family applicable/all NO_MATCH; sepsis N/A."""
    f = base_fixture()
    f["dyspnoea_context"] = "TRUE"
    f["suspected_sepsis"] = "FALSE"
    f["evidence"].update({"EV-RF-APPEAR-001": "ABSENT", "EV-RF-NEURO-001": "ABSENT"})
    return f


def p4_sepsis_applicable_fixture() -> Dict[str, Any]:
    """Baseline all NO_MATCH; sepsis family applicable/all NO_MATCH; dyspnoea N/A."""
    f = sepsis_fixture()
    f["dyspnoea_context"] = "FALSE"
    return f


def p4_fixture() -> Dict[str, Any]:
    """Backward-compatible name for the original P4 baseline fixture."""
    return p4_both_families_not_applicable_fixture()


def case(
    case_id: str,
    fixture: Dict[str, Any],
    status: str,
    disposition: str,
    reason: str,
    policy: str,
    matched: List[str] | None = None,
    insufficient: List[str] | None = None,
    must_not: List[str] | None = None,
    replay: bool = False,
) -> Dict[str, Any]:
    return {
        "case_id": case_id,
        "case_version": "0.2",
        "fixture_ref": f"FX-{case_id}",
        "fixture": fixture,
        "expected": {
            "result_status": status,
            "disposition": disposition,
            "reason_code": reason,
            "policy_ref": policy,
            "matched_rule_refs": matched or [],
            "insufficient_rule_refs": insufficient or [],
        },
        "must_not_output": must_not or [],
        "replay": replay,
    }


def excluded_unproducible_cases() -> List[Dict[str, Any]]:
    """Frozen case identities that cannot be honestly produced by valid shared-scope C execution.

    BF-IR-01 requires these identities to stay visible but outside executable GC/SS counts.
    P5 is covered separately as a D09 defensive contract-boundary check; it is not represented
    as a clinical C->D execution result.
    """
    return [
        {
            "case_id": "GC-026",
            "classification": "UNPRODUCIBLE_UNDER_SHARED_SCOPE",
            "reason": (
                "A valid dyspnoea family execution uses one shared scope for both rules; "
                "SCOPE_MISMATCH + MATCHED cannot be produced by the frozen C evaluator."
            ),
            "executable_counted": False,
        },
        {
            "case_id": "SS-012",
            "classification": "UNPRODUCIBLE_UNDER_SHARED_SCOPE",
            "reason": (
                "The safety scenario depended on the same impossible mixed shared-scope family result."
            ),
            "executable_counted": False,
        },
    ]


def golden_cases() -> List[Dict[str, Any]]:
    c: List[Dict[str, Any]] = []

    f = base_fixture(); f["evidence"]["EV-RF-RESP-001"] = "PRESENT"
    c.append(case("GC-001", f, "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT", "D09-P-010", ["C-RULE-RESP-001"], must_not=["SAFE", "NORMAL"]))

    f = base_fixture(); f["evidence"]["EV-MNM-NEURO-001"] = "PRESENT"
    c.append(case("GC-002", f, "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT", "D09-P-010", ["C-RULE-NEURO-001"], must_not=["DIAGNOSTIC_CERTAINTY", "SAFE"]))

    f = base_fixture(); f["evidence"]["EV-MNM-NEURO-002"] = "PRESENT"
    c.append(case("GC-003", f, "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT", "D09-P-010", ["C-RULE-NEURO-002"], must_not=["FIRST_HIT_MUTATION", "SAFE"]))

    f = base_fixture(); f["evidence"]["EV-MNM-CARD-001"] = "PRESENT"
    c.append(case("GC-004", f, "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT", "D09-P-010", ["C-RULE-CARD-001"], must_not=["CONFIRMED_ACS", "CONFIRMED_MI", "SAFE"]))

    f = base_fixture(); f["evidence"]["EV-RF-ALLERGY-001"] = "PRESENT"
    c.append(case("GC-005", f, "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT", "D09-P-010", ["C-RULE-ALLERGY-001"], must_not=["MILD_ALLERGY_GENERALIZATION", "SAFE"]))

    f = base_fixture(); f["dyspnoea_context"] = "TRUE"; f["evidence"]["EV-RF-APPEAR-001"] = "PRESENT"
    c.append(case("GC-006", f, "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT", "D09-P-010", ["C-RULE-DYSPNOEA-APPEAR-001"], must_not=["CONTEXT_CREATED_BY_APPEARANCE", "SAFE"]))

    f = base_fixture(); f["dyspnoea_context"] = "TRUE"; f["evidence"]["EV-RF-NEURO-001"] = "PRESENT"
    c.append(case("GC-007", f, "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT", "D09-P-010", ["C-RULE-DYSPNOEA-CONFUSION-001"], must_not=["GLOBAL_CONFUSION_REDFLAG", "SAFE"]))

    f = sepsis_fixture(); f["measurements"]["respiratory_rate_bpm"]["value"] = 25
    c.append(case("GC-008", f, "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT", "D09-P-010", ["C-RULE-SEPSIS-RR-HIGH-001"], must_not=["RR_CREATES_SEPSIS_CONTEXT", "SAFE"]))

    f = sepsis_fixture(); f["measurements"]["respiratory_rate_bpm"]["value"] = 21
    c.append(case("GC-009", f, "VALID", "CAUTION", "MODERATE_HIGH_RULE_SIGNAL_PRESENT", "D09-P-030", ["C-RULE-SEPSIS-RR-MODHIGH-001"], must_not=["HIGH_RISK", "NO_HIGH_RISK_SIGNAL"]))

    f = sepsis_fixture(); f["measurements"]["systolic_bp_mmHg"]["value"] = 90; f["measurements"]["usual_systolic_bp_mmHg"] = {"state": "UNKNOWN"}
    c.append(case("GC-010", f, "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT", "D09-P-010", ["C-RULE-SEPSIS-SBP-HIGH-001"], must_not=["DOWNGRADE_DUE_TO_USUAL_BP_UNKNOWN", "SAFE"]))

    f = sepsis_fixture(); f["measurements"]["systolic_bp_mmHg"]["value"] = 95; f["measurements"]["usual_systolic_bp_mmHg"]["value"] = 120
    c.append(case("GC-011", f, "VALID", "CAUTION", "MODERATE_HIGH_RULE_SIGNAL_PRESENT", "D09-P-030", ["C-RULE-SEPSIS-SBP-MODHIGH-001"], must_not=["NO_HIGH_RISK_SIGNAL"]))

    # BF-IR-02: three independent P4 coverage paths.
    c.append(case("GC-012", p4_both_families_not_applicable_fixture(), "VALID", "NO_HIGH_RISK_SIGNAL", "COVERAGE_COMPLETE_GOVERNED_RULE_SET_EVALUATED_NO_SIGNAL", "D09-P-040", must_not=["SAFE", "NORMAL", "NO_DISEASE"]))

    f = sepsis_fixture(); f["measurements"]["heart_rate_bpm"]["value"] = 100
    c.append(case("GC-013", f, "VALID", "CAUTION", "MODERATE_HIGH_RULE_SIGNAL_PRESENT", "D09-P-030", ["C-RULE-SEPSIS-HR-MODHIGH-001"], must_not=["NO_HIGH_RISK_SIGNAL"]))

    c.append(case("GC-014", p4_dyspnoea_applicable_fixture(), "VALID", "NO_HIGH_RISK_SIGNAL", "COVERAGE_COMPLETE_GOVERNED_RULE_SET_EVALUATED_NO_SIGNAL", "D09-P-040", must_not=["OVERALL_POLICY_SCOPE_MISMATCH", "SAFE"]))
    c.append(case("GC-015", p4_sepsis_applicable_fixture(), "VALID", "NO_HIGH_RISK_SIGNAL", "COVERAGE_COMPLETE_GOVERNED_RULE_SET_EVALUATED_NO_SIGNAL", "D09-P-040", must_not=["OVERALL_POLICY_SCOPE_MISMATCH", "SAFE"]))

    f = base_fixture(); f["evidence"]["EV-RF-RESP-001"] = "PRESENT"; f["evidence"]["EV-RF-ALLERGY-001"] = "UNKNOWN"
    c.append(case("GC-016", f, "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT", "D09-P-010", ["C-RULE-RESP-001"], ["C-RULE-ALLERGY-001"], ["P2_DOWNGRADE", "P3_DOWNGRADE"]))

    f = base_fixture(); f["evidence"]["EV-RF-RESP-001"] = "UNKNOWN"
    c.append(case("GC-017", f, "FAILED", "NONE", "INSUFFICIENT_INFORMATION", "D09-P-020", insufficient=["C-RULE-RESP-001"], must_not=["NO_MATCH", "NO_HIGH_RISK_SIGNAL"]))

    f = sepsis_fixture(); f["measurements"]["respiratory_rate_bpm"] = {"state": "UNMEASURED"}
    c.append(case("GC-018", f, "FAILED", "NONE", "INSUFFICIENT_INFORMATION", "D09-P-020", insufficient=["C-RULE-SEPSIS-RR-HIGH-001", "C-RULE-SEPSIS-RR-MODHIGH-001"], must_not=["NORMAL", "NO_HIGH_RISK_SIGNAL"]))

    f = base_fixture(); f["evidence"]["EV-RF-RESP-001"] = "REMOTE_NOT_OBSERVED"
    c.append(case("GC-019", f, "FAILED", "NONE", "INSUFFICIENT_INFORMATION", "D09-P-020", insufficient=["C-RULE-RESP-001"], must_not=["EXCLUDED", "NO_HIGH_RISK_SIGNAL"]))

    f = base_fixture(); f["pregnancy_or_puerperium"] = "TRUE"
    c.append(case("GC-020", f, "FAILED", "NONE", "OVERALL_POLICY_SCOPE_MISMATCH", "D09-P-001", must_not=["C_RULE_EVALUATION", "BASELINE_DENOMINATOR", "HIGH_RISK", "NO_HIGH_RISK_SIGNAL"]))

    for cid, state, forbidden in [
        ("GC-021", "UNKNOWN", ["FALSE_INFERENCE", "BASELINE_DENOMINATOR", "NO_HIGH_RISK_SIGNAL"]),
        ("GC-022", "NOT_ASKED", ["INFER_NON_PREGNANT", "BASELINE_DENOMINATOR"]),
        ("GC-023", "NOT_ESTABLISHED", ["RULE_SIGNAL_SCOPE_MISMATCH", "BASELINE_DENOMINATOR"]),
    ]:
        f = base_fixture(); f["pregnancy_or_puerperium"] = state
        c.append(case(cid, f, "FAILED", "NONE", "OVERALL_POLICY_SCOPE_NOT_ESTABLISHED", "D09-P-001", must_not=forbidden))

    f = base_fixture(); f["clinical_state_version"] = "STALE"; f["evidence"]["EV-RF-RESP-001"] = "PRESENT"
    c.append(case("GC-024", f, "FAILED", "NONE", "STALE_INPUT", "D09-P-001", must_not=["HIGH_RISK", "CURRENT_ACCEPTANCE"]))

    f = base_fixture(); f["release_refs"]["rule_release_ref"] = "RR-U03-RISK-001@0.2.0-candidate"
    c.append(case("GC-025", f, "FAILED", "NONE", "RELEASE_MISMATCH", "D09-P-001", must_not=["LATEST_FALLBACK", "HIGH_RISK", "CAUTION", "NO_HIGH_RISK_SIGNAL"]))

    # GC-026 is intentionally excluded by BF-IR-01; see excluded_unproducible_cases().

    f = sepsis_fixture(); f["measurements"]["respiratory_rate_bpm"]["value"] = 21; f["evidence"]["EV-RF-ALLERGY-001"] = "UNKNOWN"
    c.append(case("GC-027", f, "FAILED", "NONE", "INSUFFICIENT_INFORMATION", "D09-P-020", ["C-RULE-SEPSIS-RR-MODHIGH-001"], ["C-RULE-ALLERGY-001"], ["CAUTION", "NO_HIGH_RISK_SIGNAL"]))

    f = base_fixture(); f["evidence"]["EV-RF-RESP-001"] = "PRESENT"; f["idempotency_key"] = "IDEMP-GC-028"
    c.append(case("GC-028", f, "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT", "D09-P-010", ["C-RULE-RESP-001"], replay=True, must_not=["DUPLICATE_CLINICAL_EFFECT"]))

    f = sepsis_fixture(); f["evidence"]["EV-RF-APPEAR-001"] = "PRESENT"
    c.append(case("GC-029", f, "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT", "D09-P-010", ["C-RULE-SEPSIS-APPEAR-HIGH-001"]))

    f = sepsis_fixture(); f["evidence"]["EV-RF-SEPSIS-001"] = "PRESENT"
    c.append(case("GC-030", f, "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT", "D09-P-010", ["C-RULE-SEPSIS-RASH-HIGH-001"]))

    f = sepsis_fixture(); f["measurements"]["heart_rate_bpm"]["value"] = 131
    c.append(case("GC-031", f, "VALID", "HIGH_RISK", "HIGH_RISK_RULE_SIGNAL_PRESENT", "D09-P-010", ["C-RULE-SEPSIS-HR-HIGH-001"]))

    assert len(c) == 30
    return c


def safety_cases() -> List[Dict[str, Any]]:
    cases = [
        {"id": "SS-001", "scenario": "UNKNOWN_REQUIRED_EVIDENCE"},
        {"id": "SS-002", "scenario": "UNMEASURED_REQUIRED_MEASUREMENT"},
        {"id": "SS-003", "scenario": "REMOTE_NOT_OBSERVED"},
        {"id": "SS-004", "scenario": "NOT_ASKED"},
        {"id": "SS-005", "scenario": "PREGNANCY_TRUE"},
        {"id": "SS-006", "scenario": "PREGNANCY_UNKNOWN"},
        {"id": "SS-007", "scenario": "PREGNANCY_NOT_ASKED"},
        {"id": "SS-008", "scenario": "PREGNANCY_NOT_ESTABLISHED"},
        {"id": "SS-009", "scenario": "STALE"},
        {"id": "SS-010", "scenario": "RELEASE_MISMATCH"},
        {"id": "SS-011", "scenario": "NO_HIGH_NOT_SAFE"},
        # SS-012 excluded by BF-IR-01; see excluded_unproducible_cases().
        {"id": "SS-013", "scenario": "IDEMPOTENT_REPLAY"},
        {"id": "SS-014", "scenario": "NO_DIRECT_COMMIT"},
        {"id": "SS-015", "scenario": "RULE_SIGNAL_NOT_DISPOSITION"},
        {"id": "SS-016", "scenario": "D09_NOT_U04"},
        {"id": "SS-017", "scenario": "MUTABLE_LATEST"},
        {"id": "SS-018", "scenario": "UNAPPROVED_REF"},
        {"id": "SS-019", "scenario": "NO_FREEFORM_KNOWLEDGE_EXECUTION"},
        {"id": "SS-020", "scenario": "NO_SCOPE_EXPANSION"},
    ]
    assert len(cases) == 19
    return cases
