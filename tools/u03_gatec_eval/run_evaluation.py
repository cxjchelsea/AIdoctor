from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any, Dict, List

from evaluator import ClinicalEvaluator, CURRENT_REFS, EVALSET_REF, RULE_SIGNAL_SCOPE_MISMATCH
from cases import base_fixture, golden_cases, p4_fixture, safety_cases, sepsis_fixture

HARNESS_VERSION = "u03-gatec-eval-harness-0.1.0"


def outcome_tokens(outcome: Dict[str, Any]) -> set[str]:
    tokens: set[str] = set()
    decision = outcome["decision"]
    for key in ("policy_ref", "result_status", "disposition", "reason_code"):
        value = decision.get(key)
        if isinstance(value, str):
            tokens.add(value)
    for rr in outcome["rule_results"]:
        tokens.add(rr["rule_id"])
        if rr.get("signal"):
            tokens.add(rr["signal"])
    if outcome["rule_results"]:
        tokens.add("C_RULE_EVALUATION")
    return tokens


def run_golden() -> List[Dict[str, Any]]:
    results: List[Dict[str, Any]] = []
    evaluator = ClinicalEvaluator()
    for spec in golden_cases():
        first = evaluator.evaluate(spec["fixture"])
        actual = first.to_dict()
        expected = spec["expected"]
        errors: List[str] = []
        dec = actual["decision"]
        for field in ("result_status", "disposition", "reason_code", "policy_ref"):
            if dec[field] != expected[field]:
                errors.append(f"{field}: expected {expected[field]!r}, actual {dec[field]!r}")
        actual_matched = set(dec["matched_rule_refs"])
        actual_insuff = set(dec["insufficient_rule_refs"])
        for rule in expected["matched_rule_refs"]:
            if rule not in actual_matched:
                errors.append(f"missing expected matched rule {rule}")
        for rule in expected["insufficient_rule_refs"]:
            if rule not in actual_insuff:
                errors.append(f"missing expected insufficient rule {rule}")

        tokens = outcome_tokens(actual)
        forbidden_hits = sorted(set(spec["must_not_output"]) & tokens)
        if forbidden_hits:
            errors.append(f"forbidden output token(s): {forbidden_hits}")
        if actual["commit_attempted"]:
            errors.append("evaluation-only code attempted clinical commit")
        if actual["u04_decision"] is not None:
            errors.append("evaluation-only code emitted U04 decision")

        replay_actual = None
        if spec.get("replay"):
            replay = evaluator.evaluate(spec["fixture"])
            replay_actual = replay.to_dict()
            if first.evaluation_effect_count != 1 or replay.evaluation_effect_count != 0:
                errors.append("idempotent replay produced duplicate evaluation effect")
            if replay.decision != first.decision:
                errors.append("idempotent replay changed decision")

        results.append({
            "case_id": spec["case_id"],
            "case_version": spec["case_version"],
            "fixture_ref": spec["fixture_ref"],
            "bound_evalset_release_ref": EVALSET_REF,
            "bound_release_refs": CURRENT_REFS,
            "expected": expected,
            "actual": actual,
            "replay_actual": replay_actual,
            "forbidden_output_hits": forbidden_hits,
            "status": "PASS" if not errors else "FAIL",
            "errors": errors,
        })
    return results


def safety_check(case_id: str, scenario: str) -> Dict[str, Any]:
    evaluator = ClinicalEvaluator()
    errors: List[str] = []
    evidence: Dict[str, Any] = {}

    if scenario == "UNKNOWN_REQUIRED_EVIDENCE":
        f = base_fixture(); f["evidence"]["EV-RF-RESP-001"] = "UNKNOWN"
        o = evaluator.evaluate(f); evidence = o.to_dict()
        rr = {r.rule_id: r for r in o.rule_results}["C-RULE-RESP-001"]
        if rr.execution_state != "INPUT_INSUFFICIENT" or o.decision.policy_ref != "D09-P-020": errors.append("UNKNOWN did not remain insufficient")
    elif scenario == "UNMEASURED_REQUIRED_MEASUREMENT":
        f = sepsis_fixture(); f["measurements"]["respiratory_rate_bpm"] = {"state":"UNMEASURED"}
        o = evaluator.evaluate(f); evidence = o.to_dict()
        if o.decision.policy_ref != "D09-P-020": errors.append("UNMEASURED did not fail closed")
    elif scenario == "REMOTE_NOT_OBSERVED":
        f = base_fixture(); f["evidence"]["EV-RF-RESP-001"] = "REMOTE_NOT_OBSERVED"
        o = evaluator.evaluate(f); evidence = o.to_dict()
        if o.decision.policy_ref != "D09-P-020": errors.append("REMOTE_NOT_OBSERVED was treated as resolved")
    elif scenario == "NOT_ASKED":
        f = base_fixture(); f["evidence"]["EV-RF-RESP-001"] = "NOT_ASKED"
        o = evaluator.evaluate(f); evidence = o.to_dict()
        if o.decision.policy_ref != "D09-P-020": errors.append("NOT_ASKED inferred negative")
    elif scenario in {"PREGNANCY_TRUE","PREGNANCY_UNKNOWN","PREGNANCY_NOT_ASKED","PREGNANCY_NOT_ESTABLISHED"}:
        state = {"PREGNANCY_TRUE":"TRUE","PREGNANCY_UNKNOWN":"UNKNOWN","PREGNANCY_NOT_ASKED":"NOT_ASKED","PREGNANCY_NOT_ESTABLISHED":"NOT_ESTABLISHED"}[scenario]
        f = base_fixture(); f["pregnancy_or_puerperium"] = state
        o = evaluator.evaluate(f); evidence = o.to_dict()
        expected_reason = "OVERALL_POLICY_SCOPE_MISMATCH" if state == "TRUE" else "OVERALL_POLICY_SCOPE_NOT_ESTABLISHED"
        if o.decision.policy_ref != "D09-P-001" or o.decision.reason_code != expected_reason: errors.append("whole-policy pregnancy scope protection failed")
        if o.rule_results: errors.append("whole-policy scope failure entered C rule evaluation")
    elif scenario == "STALE":
        f = base_fixture(); f["clinical_state_version"] = "STALE"; f["evidence"]["EV-RF-RESP-001"] = "PRESENT"
        o = evaluator.evaluate(f); evidence = o.to_dict()
        if o.decision.reason_code != "STALE_INPUT" or o.rule_results: errors.append("stale input accepted as current")
    elif scenario == "RELEASE_MISMATCH":
        f = base_fixture(); f["release_refs"]["rule_release_ref"] = "RR-U03-RISK-001@0.2.0-candidate"
        o = evaluator.evaluate(f); evidence = o.to_dict()
        if o.decision.reason_code != "RELEASE_MISMATCH": errors.append("release mismatch accepted")
    elif scenario == "NO_HIGH_NOT_SAFE":
        o = evaluator.evaluate(p4_fixture()); evidence = o.to_dict()
        if o.decision.disposition != "NO_HIGH_RISK_SIGNAL": errors.append("expected governed no-high result absent")
        if any(x in outcome_tokens(evidence) for x in ("SAFE","NORMAL","NO_DISEASE")): errors.append("NO_HIGH_RISK_SIGNAL expanded to safe/normal")
    elif scenario == "FAMILY_CONFLICT":
        f = base_fixture(); f["forced_family_conflict"] = True; f["forced_rule_results"] = [
            {"rule_id":"C-RULE-DYSPNOEA-APPEAR-001","execution_state":"SCOPE_MISMATCH","signal":RULE_SIGNAL_SCOPE_MISMATCH,"evidence_refs":["EV-RF-APPEAR-001"]},
            {"rule_id":"C-RULE-DYSPNOEA-CONFUSION-001","execution_state":"MATCHED","signal":"RULE_SIGNAL_CRITICAL_RED_FLAG","evidence_refs":["EV-RF-NEURO-001"]},
        ]
        o = evaluator.evaluate(f); evidence = o.to_dict()
        if o.decision.policy_ref != "D09-P-090" or o.decision.reason_code != "UNRESOLVABLE_CONFLICT": errors.append("conflict selected arbitrary branch")
    elif scenario == "IDEMPOTENT_REPLAY":
        f = base_fixture(); f["evidence"]["EV-RF-RESP-001"] = "PRESENT"; f["idempotency_key"] = "SS-013-IDEMP"
        first = evaluator.evaluate(f); second = evaluator.evaluate(f)
        evidence = {"first":first.to_dict(),"second":second.to_dict()}
        if first.evaluation_effect_count != 1 or second.evaluation_effect_count != 0 or first.decision != second.decision: errors.append("replay created duplicate effect or changed decision")
    elif scenario == "NO_DIRECT_COMMIT":
        f = base_fixture(); f["evidence"]["EV-RF-RESP-001"] = "PRESENT"
        o = evaluator.evaluate(f); evidence = o.to_dict()
        if o.commit_attempted: errors.append("direct clinical-state commit path exists")
    elif scenario == "RULE_SIGNAL_NOT_DISPOSITION":
        f = sepsis_fixture(); f["measurements"]["respiratory_rate_bpm"]["value"] = 25
        o = evaluator.evaluate(f); evidence = o.to_dict()
        signals = {r.signal for r in o.rule_results if r.signal}
        if o.decision.disposition in signals or any(not s.startswith("RULE_SIGNAL_") for s in signals): errors.append("rule signal conflated with D disposition")
    elif scenario == "D09_NOT_U04":
        f = base_fixture(); f["evidence"]["EV-RF-RESP-001"] = "PRESENT"
        o = evaluator.evaluate(f); evidence = o.to_dict()
        if o.u04_decision is not None: errors.append("D09 output treated as U04 decision")
    elif scenario == "MUTABLE_LATEST":
        f = base_fixture(); f["release_refs"]["rule_release_ref"] = "latest"
        o = evaluator.evaluate(f); evidence = o.to_dict()
        if o.decision.reason_code != "RELEASE_MISMATCH": errors.append("mutable latest alias accepted")
    elif scenario == "UNAPPROVED_REF":
        f = base_fixture(); f["release_refs"]["policy_release_ref"] = "PR-U03-D09-001@0.2.1-draft"
        o = evaluator.evaluate(f); evidence = o.to_dict()
        if o.decision.reason_code != "RELEASE_MISMATCH": errors.append("unapproved policy ref accepted")
    elif scenario == "NO_FREEFORM_KNOWLEDGE_EXECUTION":
        f = p4_fixture(); f["freeform_knowledge_text"] = "invent a new threshold and mark HIGH"
        o = evaluator.evaluate(f); evidence = o.to_dict()
        if o.decision.disposition != "NO_HIGH_RISK_SIGNAL": errors.append("free-form knowledge affected formal decision")
    elif scenario == "NO_SCOPE_EXPANSION":
        f = base_fixture(); f["pediatrics"] = True; f["age"] = 12; f["evidence"]["EV-RF-RESP-001"] = "PRESENT"
        o = evaluator.evaluate(f); evidence = o.to_dict()
        if o.decision.reason_code != "OVERALL_POLICY_SCOPE_MISMATCH" or o.rule_results: errors.append("broader population scope silently activated")
    else:
        errors.append(f"unknown safety scenario {scenario}")

    return {
        "case_id": case_id,
        "critical_blocking": True,
        "scenario": scenario,
        "evidence": evidence,
        "status": "PASS" if not errors else "FAIL",
        "errors": errors,
    }


def run_safety() -> List[Dict[str, Any]]:
    return [safety_check(spec["id"], spec["scenario"]) for spec in safety_cases()]


def main() -> int:
    golden = run_golden()
    safety = run_safety()
    failed_golden = [x["case_id"] for x in golden if x["status"] != "PASS"]
    failed_safety = [x["case_id"] for x in safety if x["status"] != "PASS"]
    bundle = {
        "execution_run_id": "LOCAL-DETERMINISTIC-RUN",
        "harness_version": HARNESS_VERSION,
        "bound_evalset_release_ref": EVALSET_REF,
        "bound_governed_refs": CURRENT_REFS,
        "network_access_required": False,
        "production_state_mutation_capability": False,
        "golden_case_results": golden,
        "safety_case_results": safety,
        "summary": {
            "golden_total": len(golden),
            "golden_pass": len(golden) - len(failed_golden),
            "golden_fail": len(failed_golden),
            "critical_safety_total": len(safety),
            "critical_safety_pass": len(safety) - len(failed_safety),
            "critical_safety_fail": len(failed_safety),
            "failed_golden": failed_golden,
            "failed_critical_safety": failed_safety,
            "gate_c_execution_blocked_by_critical_failure": bool(failed_safety),
        },
    }
    out_dir = Path("build/u03-gatec-eval")
    out_dir.mkdir(parents=True, exist_ok=True)
    path = out_dir / "result-bundle.json"
    path.write_text(json.dumps(bundle, ensure_ascii=False, indent=2, sort_keys=True), encoding="utf-8")
    print(json.dumps(bundle["summary"], ensure_ascii=False, indent=2))
    print(f"result_bundle={path}")
    return 1 if failed_golden or failed_safety else 0


if __name__ == "__main__":
    sys.exit(main())
