package com.aidoctor.diagnosis.evaluation.u03;

import com.aidoctor.diagnosis.evaluation.u03.U03EvaluationClinicalEvaluator.FactState;
import com.aidoctor.diagnosis.evaluation.u03.U03EvaluationClinicalEvaluator.Fixture;
import com.aidoctor.diagnosis.evaluation.u03.U03EvaluationClinicalEvaluator.ScopeState;

/** Fixture-only synthetic inputs approved for Gate C evaluation. */
public final class U03EvaluationFixtureFactory {
    private U03EvaluationFixtureFactory() {}

    public static Fixture golden(String id) {
        int n = Integer.parseInt(id.substring(3));
        Fixture f = base(id);
        switch (n) {
            case 1: return f.evidence("EV-RF-RESP-001", FactState.PRESENT);
            case 2: return f.evidence("EV-MNM-NEURO-001", FactState.PRESENT);
            case 3: return f.evidence("EV-MNM-NEURO-002", FactState.PRESENT);
            case 4: return f.evidence("EV-MNM-CARD-001", FactState.PRESENT);
            case 5: return f.evidence("EV-RF-ALLERGY-001", FactState.PRESENT);
            case 6: f.dyspnoeaContext = ScopeState.TRUE; return f.evidence("EV-RF-APPEAR-001", FactState.PRESENT);
            case 7: f.dyspnoeaContext = ScopeState.TRUE; return f.evidence("EV-RF-NEURO-001", FactState.PRESENT);
            case 8: return sepsisVitals(f, 25, 120, 80);
            case 9: return sepsisVitals(f, 21, 120, 80);
            case 10: return sepsisVitals(f, 18, 90, 80);
            case 11: f = sepsisVitals(f, 18, 95, 80); f.usualSbp = Integer.valueOf(120); return f;
            case 12: return f;
            case 13: return sepsisVitals(f, 18, 120, 100);
            case 14: f.sepsisContext = ScopeState.FALSE; return f;
            case 15: f.dyspnoeaContext = ScopeState.FALSE; return f;
            case 16: f.evidence("EV-RF-RESP-001", FactState.PRESENT); f.evidence("EV-MNM-NEURO-001", FactState.UNKNOWN); return f;
            case 17: return f.evidence("EV-RF-RESP-001", FactState.UNKNOWN);
            case 18: f = sepsisVitals(f, 18, 120, 80); f.rr = null; f.rrState = FactState.UNMEASURED; return f;
            case 19: return f.evidence("EV-RF-RESP-001", FactState.REMOTE_NOT_OBSERVED);
            case 20: f.pregnancy = ScopeState.TRUE; return f;
            case 21: f.pregnancy = ScopeState.UNKNOWN; return f;
            case 22: f.pregnancy = ScopeState.NOT_ASKED; return f;
            case 23: f.pregnancy = ScopeState.NOT_ESTABLISHED; return f;
            case 24: f.currentVersion = false; return f.evidence("EV-RF-RESP-001", FactState.PRESENT);
            case 25: f.ruleRelease = "RR-U03-RISK-001@wrong"; return f;
            case 26: f.conflict = true; return f;
            case 27: f = sepsisVitals(f, 21, 120, 80); f.evidence("EV-MNM-NEURO-001", FactState.UNKNOWN); return f;
            case 28: f.idempotencyKey = "IDEMP-GC-028"; return f.evidence("EV-RF-RESP-001", FactState.PRESENT);
            case 29: f = sepsisVitals(f, 18, 120, 80); return f.evidence("EV-RF-APPEAR-001", FactState.PRESENT);
            case 30: f = sepsisVitals(f, 18, 120, 80); return f.evidence("EV-RF-SEPSIS-001", FactState.PRESENT);
            case 31: return sepsisVitals(f, 18, 120, 131);
            default: throw new IllegalArgumentException("unknown golden case: " + id);
        }
    }

    public static Fixture safetyBase(String id) {
        Fixture f = base(id);
        return f;
    }

    private static Fixture base(String id) {
        Fixture f = new Fixture(id);
        f.evidence("EV-RF-RESP-001", FactState.ABSENT)
         .evidence("EV-MNM-NEURO-001", FactState.ABSENT)
         .evidence("EV-MNM-NEURO-002", FactState.ABSENT)
         .evidence("EV-MNM-CARD-001", FactState.ABSENT)
         .evidence("EV-RF-ALLERGY-001", FactState.ABSENT)
         .evidence("EV-RF-APPEAR-001", FactState.ABSENT)
         .evidence("EV-RF-NEURO-001", FactState.ABSENT)
         .evidence("EV-RF-SEPSIS-001", FactState.ABSENT);
        return f;
    }

    private static Fixture sepsisVitals(Fixture f, int rr, int sbp, int hr) {
        f.sepsisContext = ScopeState.TRUE;
        f.rr = Integer.valueOf(rr); f.rrState = FactState.PRESENT;
        f.sbp = Integer.valueOf(sbp); f.sbpState = FactState.PRESENT; f.usualSbp = Integer.valueOf(120);
        f.hr = Integer.valueOf(hr); f.hrState = FactState.PRESENT;
        return f;
    }
}
