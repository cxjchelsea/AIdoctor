package com.aidoctor.diagnosis.runtime.u03;

import com.aidoctor.diagnosis.runtime.u05.U04A1U05AuthorityBridge;
import com.aidoctor.diagnosis.runtime.u06.U06ExecutionResult;
import com.aidoctor.diagnosis.runtime.u06.U06SyntheticDecisionBundle;
import com.aidoctor.diagnosis.runtime.u06.U06SyntheticPostF3SafetyBarrier;
import com.aidoctor.diagnosis.runtime.u06.U06SyntheticRevalidationAuthority;
import com.aidoctor.diagnosis.runtime.u06.state.U06SyntheticP01Runtime;

/**
 * NON-PRODUCTION test-only A1 host: derives its facts from actual U06 Mode-1
 * commit, readback, post-F3 barrier evaluation and revalidation result.
 * It MUST NOT be wired as a production issuer.
 */
final class U06F3SyntheticA1HostPort implements U04A1U05AuthorityBridge.A1BarrierAuthorityPort {
    private final U06SyntheticP01Runtime state;
    private final U06ExecutionResult f3;
    private final U06SyntheticPostF3SafetyBarrier.Evaluation barrier;
    private final U06SyntheticRevalidationAuthority.Result revalidation;
    private final String consultationId, cdpId;

    U06F3SyntheticA1HostPort(String consultationId, String cdpId, U06SyntheticP01Runtime state,
                             U06ExecutionResult f3,
                             U06SyntheticPostF3SafetyBarrier.Evaluation barrier,
                             U06SyntheticRevalidationAuthority.Result revalidation) {
        this.consultationId=consultationId;
        this.cdpId=cdpId;
        this.state=state;
        this.f3=f3;
        this.barrier=barrier;
        this.revalidation=revalidation;
    }

    @Override
    public U04A1U05AuthorityBridge.A1BarrierAuthority loadCurrent(
            String consultation, String cdp, int version) {
        if (!consultationId.equals(consultation) || !cdpId.equals(cdp)
                || state.readCurrent().getVersion()!=version
                || f3 == null || !U06ExecutionResult.MODE1_COMMITTED.equals(f3.getStatus())
                || !"COMMITTED".equals(f3.getCommitStatus())
                || f3.getEffectRef()==null
                || !f3.getEffectRef().equals(state.readCurrent().mapString(
                        "/patient_state/f3_gap_assessment", "f3_canonical_effect_id"))
                || barrier==null || !U06SyntheticPostF3SafetyBarrier.ALLOWED.equals(barrier.getStatus())
                || !barrier.getEvaluationId().equals(f3.getSafetyEvaluationId())
                || !f3.getEffectRef().equals(barrier.getF3EffectId())
                || barrier.getStateVersion()!=version
                || revalidation==null
                || !U06SyntheticDecisionBundle.REVALIDATED_CURRENT.equals(revalidation.getStatus())
                || revalidation.getFailureCode()!=null) {
            return null;
        }
        return new U04A1U05AuthorityBridge.A1BarrierAuthority(
                consultation, cdp, version, f3.getEffectRef(),
                barrier.getEvaluationId(), revalidation.getRevalidationId(),
                "synthetic-a1-f3-source-" + f3.getEffectRef(),
                "synthetic-a1-f3-route-" + revalidation.getRevalidationId(),
                "U07-NONPROD-POSTF3-V1", null, null, true, false);
    }
}
