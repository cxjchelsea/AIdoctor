from __future__ import annotations

from dataclasses import asdict, dataclass
from typing import Any, Dict, List, Optional, Tuple

EVALSET_REF = "ER-U03-RISK-001@0.1.0-candidate"
KNOWLEDGE_REF = "KR-U03-SOURCE-001@0.1.0-candidate"
RULE_REF = "RR-U03-RISK-001@0.2.1-candidate"
COVERAGE_REF = "U03_D09_COVERAGE_V0_2_1_CANDIDATE"
POLICY_REF = "PR-U03-D09-001@0.2.1-candidate"
POLICY_PAIR_REF = "PF-U03-C-POLICY-001"

CURRENT_REFS = {
    "knowledge_release_ref": KNOWLEDGE_REF,
    "rule_release_ref": RULE_REF,
    "coverage_contract_ref": COVERAGE_REF,
    "policy_release_ref": POLICY_REF,
    "policy_pair_ref": POLICY_PAIR_REF,
}

RULE_SIGNAL_CRITICAL_RED_FLAG = "RULE_SIGNAL_CRITICAL_RED_FLAG"
RULE_SIGNAL_MUST_NOT_MISS = "RULE_SIGNAL_MUST_NOT_MISS"
RULE_SIGNAL_SEPSIS_HIGH = "RULE_SIGNAL_SEPSIS_HIGH_RISK_CRITERION"
RULE_SIGNAL_SEPSIS_MODHIGH = "RULE_SIGNAL_SEPSIS_MODERATE_HIGH_RISK_CRITERION"
RULE_SIGNAL_INPUT_INSUFFICIENT = "RULE_SIGNAL_INPUT_INSUFFICIENT"
RULE_SIGNAL_SCOPE_MISMATCH = "RULE_SIGNAL_SCOPE_MISMATCH"

MISSING_STATES = {
    "UNKNOWN",
    "UNMEASURED",
    "NOT_ASKED",
    "AMBIGUOUS",
    "CONFLICTING",
    "REMOTE_NOT_OBSERVED",
    "NOT_ESTABLISHED",
    "INVALID",
}

BASELINE_RULES = [
    "C-RULE-RESP-001",
    "C-RULE-NEURO-001",
    "C-RULE-NEURO-002",
    "C-RULE-CARD-001",
    "C-RULE-ALLERGY-001",
]
DYSPNOEA_RULES = [
    "C-RULE-DYSPNOEA-APPEAR-001",
    "C-RULE-DYSPNOEA-CONFUSION-001",
]
SEPSIS_RULES = [
    "C-RULE-SEPSIS-RR-HIGH-001",
    "C-RULE-SEPSIS-RR-MODHIGH-001",
    "C-RULE-SEPSIS-SBP-HIGH-001",
    "C-RULE-SEPSIS-SBP-MODHIGH-001",
    "C-RULE-SEPSIS-HR-HIGH-001",
    "C-RULE-SEPSIS-HR-MODHIGH-001",
    "C-RULE-SEPSIS-APPEAR-HIGH-001",
    "C-RULE-SEPSIS-RASH-HIGH-001",
]
ALL_RULES = BASELINE_RULES + DYSPNOEA_RULES + SEPSIS_RULES

BASELINE_EVIDENCE = {
    "C-RULE-RESP-001": ("EV-RF-RESP-001", RULE_SIGNAL_CRITICAL_RED_FLAG),
    "C-RULE-NEURO-001": ("EV-MNM-NEURO-001", RULE_SIGNAL_MUST_NOT_MISS),
    "C-RULE-NEURO-002": ("EV-MNM-NEURO-002", RULE_SIGNAL_MUST_NOT_MISS),
    "C-RULE-CARD-001": ("EV-MNM-CARD-001", RULE_SIGNAL_MUST_NOT_MISS),
    "C-RULE-ALLERGY-001": ("EV-RF-ALLERGY-001", RULE_SIGNAL_CRITICAL_RED_FLAG),
}

HIGH_SIGNALS = {
    RULE_SIGNAL_CRITICAL_RED_FLAG,
    RULE_SIGNAL_MUST_NOT_MISS,
    RULE_SIGNAL_SEPSIS_HIGH,
}


@dataclass(frozen=True)
class RuleResult:
    rule_id: str
    execution_state: str
    signal: Optional[str] = None
    evidence_refs: Tuple[str, ...] = ()


@dataclass(frozen=True)
class Decision:
    policy_ref: str
    result_status: str
    disposition: str
    reason_code: str
    matched_rule_refs: Tuple[str, ...] = ()
    insufficient_rule_refs: Tuple[str, ...] = ()


@dataclass
class EvaluationOutcome:
    decision: Decision
    rule_results: List[RuleResult]
    commit_attempted: bool = False
    u04_decision: Optional[str] = None
    evaluation_effect_count: int = 1

    def to_dict(self) -> Dict[str, Any]:
        return {
            "decision": asdict(self.decision),
            "rule_results": [asdict(r) for r in self.rule_results],
            "commit_attempted": self.commit_attempted,
            "u04_decision": self.u04_decision,
            "evaluation_effect_count": self.evaluation_effect_count,
        }


def state_value(value: Any, default: str = "ABSENT") -> str:
    if isinstance(value, dict):
        return str(value.get("state", default))
    if value is None:
        return default
    if isinstance(value, str):
        return value
    return "PRESENT"


def evidence_rule(rule_id: str, evidence_ref: str, signal: str, fixture: Dict[str, Any]) -> RuleResult:
    scope = fixture.get("rule_scope", {}).get(rule_id, "TRUE")
    if scope in MISSING_STATES:
        return RuleResult(rule_id, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, (evidence_ref,))
    if scope == "FALSE":
        return RuleResult(rule_id, "SCOPE_MISMATCH", RULE_SIGNAL_SCOPE_MISMATCH, (evidence_ref,))
    state = state_value(fixture.get("evidence", {}).get(evidence_ref, "ABSENT"))
    if state == "PRESENT":
        return RuleResult(rule_id, "MATCHED", signal, (evidence_ref,))
    if state == "ABSENT":
        return RuleResult(rule_id, "NO_MATCH", None, (evidence_ref,))
    return RuleResult(rule_id, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, (evidence_ref,))


def _measurement(fixture: Dict[str, Any], key: str) -> Tuple[str, Optional[float]]:
    raw = fixture.get("measurements", {}).get(key)
    if raw is None:
        return "UNMEASURED", None
    if isinstance(raw, dict):
        state = str(raw.get("state", "PRESENT"))
        value = raw.get("value")
    else:
        state = "PRESENT"
        value = raw
    if state != "PRESENT":
        return state, None
    try:
        return state, float(value)
    except (TypeError, ValueError):
        return "INVALID", None


def _shared_sepsis_scope(fixture: Dict[str, Any]) -> str:
    age = fixture.get("age", 30)
    pregnancy = fixture.get("pregnancy_or_puerperium", "FALSE")
    suspected = fixture.get("suspected_sepsis", "FALSE")
    setting = fixture.get("setting", "SOURCE_SUPPORTED_COMMUNITY")
    if age in MISSING_STATES or pregnancy in MISSING_STATES or suspected in MISSING_STATES or setting in MISSING_STATES:
        return "INSUFFICIENT"
    if not isinstance(age, (int, float)):
        return "INSUFFICIENT"
    if age < 16 or pregnancy == "TRUE" or suspected == "FALSE" or setting not in {
        "SOURCE_SUPPORTED_COMMUNITY",
        "SOURCE_SUPPORTED_CUSTODIAL",
    }:
        return "MISMATCH"
    if suspected != "TRUE" or pregnancy != "FALSE":
        return "INSUFFICIENT"
    return "APPLICABLE"


def _sepsis_scope_result(rule_id: str, evidence_ref: str, fixture: Dict[str, Any]) -> Optional[RuleResult]:
    scope = _shared_sepsis_scope(fixture)
    if scope == "INSUFFICIENT":
        return RuleResult(rule_id, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, (evidence_ref,))
    if scope == "MISMATCH":
        return RuleResult(rule_id, "SCOPE_MISMATCH", RULE_SIGNAL_SCOPE_MISMATCH, (evidence_ref,))
    return None


def evaluate_sepsis_rules(fixture: Dict[str, Any]) -> List[RuleResult]:
    out: List[RuleResult] = []

    for rule_id, mode in [
        ("C-RULE-SEPSIS-RR-HIGH-001", "HIGH"),
        ("C-RULE-SEPSIS-RR-MODHIGH-001", "MODHIGH"),
    ]:
        evidence_ref = "EV-VS-SEPSIS-001"
        scoped = _sepsis_scope_result(rule_id, evidence_ref, fixture)
        if scoped:
            out.append(scoped)
            continue
        state, rr = _measurement(fixture, "respiratory_rate_bpm")
        if state != "PRESENT":
            out.append(RuleResult(rule_id, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, (evidence_ref,)))
        elif mode == "HIGH" and rr is not None and rr >= 25:
            out.append(RuleResult(rule_id, "MATCHED", RULE_SIGNAL_SEPSIS_HIGH, (evidence_ref,)))
        elif mode == "MODHIGH" and rr is not None and 21 <= rr <= 24:
            out.append(RuleResult(rule_id, "MATCHED", RULE_SIGNAL_SEPSIS_MODHIGH, (evidence_ref,)))
        else:
            out.append(RuleResult(rule_id, "NO_MATCH", None, (evidence_ref,)))

    evidence_ref = "EV-VS-SEPSIS-002"
    rule_id = "C-RULE-SEPSIS-SBP-HIGH-001"
    scoped = _sepsis_scope_result(rule_id, evidence_ref, fixture)
    if scoped:
        out.append(scoped)
    else:
        sbp_state, sbp = _measurement(fixture, "systolic_bp_mmHg")
        if sbp_state != "PRESENT" or sbp is None:
            out.append(RuleResult(rule_id, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, (evidence_ref,)))
        elif sbp <= 90:
            out.append(RuleResult(rule_id, "MATCHED", RULE_SIGNAL_SEPSIS_HIGH, (evidence_ref,)))
        else:
            usual_state, usual = _measurement(fixture, "usual_systolic_bp_mmHg")
            if usual_state != "PRESENT" or usual is None:
                out.append(RuleResult(rule_id, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, (evidence_ref,)))
            elif usual - sbp > 40:
                out.append(RuleResult(rule_id, "MATCHED", RULE_SIGNAL_SEPSIS_HIGH, (evidence_ref,)))
            else:
                out.append(RuleResult(rule_id, "NO_MATCH", None, (evidence_ref,)))

    rule_id = "C-RULE-SEPSIS-SBP-MODHIGH-001"
    scoped = _sepsis_scope_result(rule_id, evidence_ref, fixture)
    if scoped:
        out.append(scoped)
    else:
        state, sbp = _measurement(fixture, "systolic_bp_mmHg")
        if state != "PRESENT" or sbp is None:
            out.append(RuleResult(rule_id, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, (evidence_ref,)))
        elif 91 <= sbp <= 100:
            out.append(RuleResult(rule_id, "MATCHED", RULE_SIGNAL_SEPSIS_MODHIGH, (evidence_ref,)))
        else:
            out.append(RuleResult(rule_id, "NO_MATCH", None, (evidence_ref,)))

    for rule_id, mode in [
        ("C-RULE-SEPSIS-HR-HIGH-001", "HIGH"),
        ("C-RULE-SEPSIS-HR-MODHIGH-001", "MODHIGH"),
    ]:
        evidence_ref = "EV-VS-SEPSIS-003"
        scoped = _sepsis_scope_result(rule_id, evidence_ref, fixture)
        if scoped:
            out.append(scoped)
            continue
        state, hr = _measurement(fixture, "heart_rate_bpm")
        if state != "PRESENT" or hr is None:
            out.append(RuleResult(rule_id, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, (evidence_ref,)))
        elif mode == "HIGH" and hr > 130:
            out.append(RuleResult(rule_id, "MATCHED", RULE_SIGNAL_SEPSIS_HIGH, (evidence_ref,)))
        elif mode == "MODHIGH" and 91 <= hr <= 130:
            out.append(RuleResult(rule_id, "MATCHED", RULE_SIGNAL_SEPSIS_MODHIGH, (evidence_ref,)))
        else:
            out.append(RuleResult(rule_id, "NO_MATCH", None, (evidence_ref,)))

    for rule_id, evidence_ref in [
        ("C-RULE-SEPSIS-APPEAR-HIGH-001", "EV-RF-APPEAR-001"),
        ("C-RULE-SEPSIS-RASH-HIGH-001", "EV-RF-SEPSIS-001"),
    ]:
        scoped = _sepsis_scope_result(rule_id, evidence_ref, fixture)
        if scoped:
            out.append(scoped)
            continue
        state = state_value(fixture.get("evidence", {}).get(evidence_ref, "ABSENT"))
        if state == "PRESENT":
            out.append(RuleResult(rule_id, "MATCHED", RULE_SIGNAL_SEPSIS_HIGH, (evidence_ref,)))
        elif state == "ABSENT":
            out.append(RuleResult(rule_id, "NO_MATCH", None, (evidence_ref,)))
        else:
            out.append(RuleResult(rule_id, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, (evidence_ref,)))
    return out


def evaluate_rules(fixture: Dict[str, Any]) -> List[RuleResult]:
    """Execute the frozen C rule set.

    There is deliberately no fixture-provided rule-result override. BF-IR-01 requires every
    executable C path to be produced by the rule evaluator itself.
    """
    results: List[RuleResult] = []
    for rule_id, (evidence_ref, signal) in BASELINE_EVIDENCE.items():
        results.append(evidence_rule(rule_id, evidence_ref, signal, fixture))

    dysp_context = fixture.get("dyspnoea_context", "FALSE")
    for rule_id, evidence_ref in [
        ("C-RULE-DYSPNOEA-APPEAR-001", "EV-RF-APPEAR-001"),
        ("C-RULE-DYSPNOEA-CONFUSION-001", "EV-RF-NEURO-001"),
    ]:
        if dysp_context in MISSING_STATES:
            results.append(RuleResult(rule_id, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, (evidence_ref,)))
        elif dysp_context == "FALSE":
            results.append(RuleResult(rule_id, "SCOPE_MISMATCH", RULE_SIGNAL_SCOPE_MISMATCH, (evidence_ref,)))
        else:
            state = state_value(fixture.get("evidence", {}).get(evidence_ref, "ABSENT"))
            if state == "PRESENT":
                results.append(RuleResult(rule_id, "MATCHED", RULE_SIGNAL_CRITICAL_RED_FLAG, (evidence_ref,)))
            elif state == "ABSENT":
                results.append(RuleResult(rule_id, "NO_MATCH", None, (evidence_ref,)))
            else:
                results.append(RuleResult(rule_id, "INPUT_INSUFFICIENT", RULE_SIGNAL_INPUT_INSUFFICIENT, (evidence_ref,)))

    results.extend(evaluate_sepsis_rules(fixture))
    return results


def _release_check(fixture: Dict[str, Any]) -> Optional[str]:
    refs = fixture.get("release_refs", CURRENT_REFS)
    for key, expected in CURRENT_REFS.items():
        actual = refs.get(key)
        if actual != expected or (isinstance(actual, str) and "latest" in actual.lower()):
            return "RELEASE_MISMATCH"
    return None


def _p0_decision(reason: str) -> Decision:
    return Decision("D09-P-001", "FAILED", "NONE", reason)


def family_state_conflict(results: List[RuleResult], rule_ids: List[str]) -> bool:
    by_id = {r.rule_id: r for r in results}
    states = [by_id[r].execution_state for r in rule_ids if r in by_id]
    return bool(states) and "SCOPE_MISMATCH" in states and any(s != "SCOPE_MISMATCH" for s in states)


def has_shared_scope_conflict(results: List[RuleResult]) -> bool:
    return family_state_conflict(results, DYSPNOEA_RULES) or family_state_conflict(results, SEPSIS_RULES)


def decide(fixture: Dict[str, Any], results: List[RuleResult]) -> Decision:
    release_error = _release_check(fixture)
    if release_error:
        return _p0_decision(release_error)
    if fixture.get("clinical_state_version", "CURRENT") != "CURRENT":
        return _p0_decision("STALE_INPUT")
    if fixture.get("invalid_input"):
        return _p0_decision("INVALID_INPUT")
    if fixture.get("dependency_failure"):
        return _p0_decision("DEPENDENCY_FAILURE")

    pregnancy = fixture.get("pregnancy_or_puerperium", "FALSE")
    if pregnancy == "TRUE":
        return _p0_decision("OVERALL_POLICY_SCOPE_MISMATCH")
    if pregnancy in {"UNKNOWN", "NOT_ASKED", "NOT_ESTABLISHED"}:
        return _p0_decision("OVERALL_POLICY_SCOPE_NOT_ESTABLISHED")
    if fixture.get("pediatrics", False) is True:
        return _p0_decision("OVERALL_POLICY_SCOPE_MISMATCH")
    if fixture.get("region_scope_ok", True) is False or fixture.get("channel_scope_ok", True) is False:
        return _p0_decision("OVERALL_POLICY_SCOPE_MISMATCH")

    if has_shared_scope_conflict(results):
        return Decision("D09-P-090", "FAILED", "NONE", "UNRESOLVABLE_CONFLICT")

    by_id = {r.rule_id: r for r in results}
    matched_high = [r.rule_id for r in results if r.execution_state == "MATCHED" and r.signal in HIGH_SIGNALS]
    insufficient = [r.rule_id for r in results if r.execution_state == "INPUT_INSUFFICIENT"]
    matched_mod = [r.rule_id for r in results if r.execution_state == "MATCHED" and r.signal == RULE_SIGNAL_SEPSIS_MODHIGH]

    if matched_high:
        return Decision(
            "D09-P-010",
            "VALID",
            "HIGH_RISK",
            "HIGH_RISK_RULE_SIGNAL_PRESENT",
            tuple(sorted(matched_high)),
            tuple(sorted(insufficient)),
        )
    if insufficient:
        return Decision(
            "D09-P-020",
            "FAILED",
            "NONE",
            "INSUFFICIENT_INFORMATION",
            tuple(sorted(matched_mod)),
            tuple(sorted(insufficient)),
        )
    if matched_mod:
        return Decision(
            "D09-P-030",
            "VALID",
            "CAUTION",
            "MODERATE_HIGH_RULE_SIGNAL_PRESENT",
            tuple(sorted(matched_mod)),
            (),
        )

    baseline_complete = all(
        by_id.get(rule) is not None and by_id[rule].execution_state == "NO_MATCH"
        for rule in BASELINE_RULES
    )

    def family_complete_or_na(rule_ids: List[str]) -> bool:
        states = [by_id[r].execution_state for r in rule_ids if r in by_id]
        if len(states) != len(rule_ids):
            return False
        if all(s == "SCOPE_MISMATCH" for s in states):
            return True
        if any(s == "SCOPE_MISMATCH" for s in states):
            return False
        return all(s == "NO_MATCH" for s in states)

    if baseline_complete and family_complete_or_na(DYSPNOEA_RULES) and family_complete_or_na(SEPSIS_RULES):
        return Decision(
            "D09-P-040",
            "VALID",
            "NO_HIGH_RISK_SIGNAL",
            "COVERAGE_COMPLETE_GOVERNED_RULE_SET_EVALUATED_NO_SIGNAL",
        )
    return Decision("D09-P-090", "FAILED", "NONE", "UNRESOLVABLE_CONFLICT")


class ClinicalEvaluator:
    """Offline-only evaluator with no state-commit, U04, network, DB, or runtime integration."""

    def __init__(self) -> None:
        self._seen_idempotency: set[str] = set()

    def evaluate(self, fixture: Dict[str, Any]) -> EvaluationOutcome:
        pre_reason = _release_check(fixture)
        pregnancy = fixture.get("pregnancy_or_puerperium", "FALSE")
        skip_rules = (
            pre_reason is not None
            or fixture.get("clinical_state_version", "CURRENT") != "CURRENT"
            or pregnancy in {"TRUE", "UNKNOWN", "NOT_ASKED", "NOT_ESTABLISHED"}
            or fixture.get("pediatrics", False) is True
            or fixture.get("region_scope_ok", True) is False
            or fixture.get("channel_scope_ok", True) is False
            or fixture.get("invalid_input")
            or fixture.get("dependency_failure")
        )
        results = [] if skip_rules else evaluate_rules(fixture)
        decision = decide(fixture, results)

        effect_count = 1
        idem = fixture.get("idempotency_key")
        if idem:
            if idem in self._seen_idempotency:
                effect_count = 0
            else:
                self._seen_idempotency.add(idem)
        return EvaluationOutcome(decision, results, False, None, effect_count)
