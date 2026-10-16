package com.aidoctor.diagnosis.controller;

import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionGovernedRiskSafetyContinuation;
import com.aidoctor.diagnosis.runtime.u03.U03ExecutionCommand;
import com.aidoctor.diagnosis.runtime.u04.U04ExecutionResult;
import com.aidoctor.diagnosis.runtime.u02.U02ExecutionResult;
import javax.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Nonproduction resumed answer: durable F8 -> U02 -> governed U03 replacement
 * -> U04 safety re-evaluation. Does not deliver another question or claim E2E.
 */
@RestController
@Profile("!prod & !production")
@ConditionalOnProperty(prefix="aidoctor.v1.clinical-e2e",
        name="answer-risk-http-enabled", havingValue="true", matchIfMissing=false)
@ConditionalOnBean({V1NonProductionAnswerResumeController.class,
        U07NonProductionGovernedRiskSafetyContinuation.class,
        V1NonProductionAnswerRiskReevaluationController.ResumedRiskOwner.class})
@RequestMapping("/internal/nonproduction/v1/clinical")
public final class V1NonProductionAnswerRiskReevaluationController {
    /** Reads already issued authority for the newly committed resumed Run. */
    public interface ResumedRiskOwner {
        V1NonProductionClinicalSafetyHandoffController.AuthorizedExecution
                requireResumedRiskAuthority(String consultationId, String newRunId,
                        U02ExecutionResult committedFact);
    }
    public static final class Result {
        public final String consultationId,newRunId,status;
        public final boolean completeClinicalE2e=false,patientDeliveryAllowed=false;
        Result(String consultationId,String newRunId) {
            this.consultationId=consultationId;
            this.newRunId=newRunId;
            this.status="F8_U02_U03_U04_COMMITTED_NEXT_QUESTION_NOT_DELIVERED";
        }
    }
    private final V1NonProductionAnswerResumeController answer;
    private final U07NonProductionGovernedRiskSafetyContinuation clinical;
    private final ResumedRiskOwner owner;
    public V1NonProductionAnswerRiskReevaluationController(
            V1NonProductionAnswerResumeController answer,
            U07NonProductionGovernedRiskSafetyContinuation clinical, ResumedRiskOwner owner) {
        if(answer==null || clinical==null || owner==null)
            throw new IllegalArgumentException("V1_RESUMED_RISK_OWNER_REQUIRED");
        this.answer=answer;this.clinical=clinical;this.owner=owner;
    }
    @PostMapping("/answer-through-u04")
    public ResponseEntity<Result> resume(@Valid @RequestBody
            V1NonProductionAnswerResumeController.AnswerRequest request) {
        V1NonProductionAnswerResumeController.CommittedAnswer completed=answer.commitAnswer(request);
        V1NonProductionClinicalSafetyHandoffController.AuthorizedExecution approved=
                owner.requireResumedRiskAuthority(completed.authority.consultationId,
                        completed.newRunId,completed.clinicalFact);
        requireBoundResumedAuthority(completed,approved);
        V1NonProductionClinicalSafetyHandoffController.requireNonProductionEnvironment(
                approved.environmentId);
        V1NonProductionClinicalSafetyHandoffController.requireFrozenSafetyRelease(approved);
        U07NonProductionGovernedRiskSafetyContinuation.Result evaluated=
                clinical.executeFromCommittedU02ReplacingExistingRisk(
                        completed.clinicalFact,approved.command,approved.releases,
                        approved.environmentId,approved.scope);
        U04ExecutionResult gate=evaluated==null?null:evaluated.gate;
        if(gate==null || !U04ExecutionResult.COMMIT_COMPLETE.equals(gate.getStatus())
                || gate.getCommitResult()==null
                || !"COMMITTED".equals(gate.getCommitResult().status)
                || !completed.authority.cdpId.equals(gate.getCommitResult().cdpId))
            throw new IllegalStateException("V1_RESUMED_U04_COMMIT_REQUIRED");
        return ResponseEntity.ok(new Result(completed.authority.consultationId,completed.newRunId));
    }
    static void requireBoundResumedAuthority(
            V1NonProductionAnswerResumeController.CommittedAnswer c,
            V1NonProductionClinicalSafetyHandoffController.AuthorizedExecution a) {
        if(c==null || c.clinicalFact==null || c.clinicalFact.commitResult==null
                || a==null || a.command==null)
            throw new IllegalStateException("V1_RESUMED_RISK_AUTHORITY_REQUIRED");
        U03ExecutionCommand x=a.command;
        if(!c.authority.consultationId.equals(x.consultationId)
                || !c.authority.cdpId.equals(x.cdpId)
                || !c.authority.threadId.equals(x.threadId)
                || !c.newRunId.equals(x.runId)
                || !c.authority.eventId.equals(x.eventId)
                || c.clinicalFact.commitResult.committedVersion==null
                || c.clinicalFact.commitResult.committedVersion.intValue()
                    !=x.clinicalStateVersion)
            throw new IllegalStateException("V1_RESUMED_RISK_DURABLE_AUTHORITY_MISMATCH");
    }
}
