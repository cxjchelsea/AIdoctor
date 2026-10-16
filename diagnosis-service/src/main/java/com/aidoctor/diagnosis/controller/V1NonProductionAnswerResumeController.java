package com.aidoctor.diagnosis.controller;

import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionAnswerResumeAdmission;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionDurableF8Consume;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionTrustedAnswerU02Bridge;
import com.aidoctor.diagnosis.runtime.u02.U02ClinicalFactApplicationService;
import com.aidoctor.diagnosis.runtime.u02.U02ExecutionCommand;
import com.aidoctor.diagnosis.runtime.u02.U02ExecutionResult;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Explicit nonproduction F8 -> U02 answer intake. Durable consumption, fresh
 * Run and P01 clinical fact commit are required. Does not claim second-round
 * risk re-evaluation, question delivery, or full clinical E2E.
 */
@RestController
@Profile("!prod & !production")
@ConditionalOnProperty(prefix="aidoctor.v1.clinical-e2e",
        name="answer-resume-http-enabled", havingValue="true", matchIfMissing=false)
@ConditionalOnBean({U07NonProductionAnswerResumeAdmission.class,
        U07NonProductionDurableF8Consume.class,
        U07NonProductionTrustedAnswerU02Bridge.class,
        U02ClinicalFactApplicationService.class,
        V1NonProductionAnswerResumeController.TrustedAnswerHost.class})
@RequestMapping("/internal/nonproduction/v1/clinical")
public final class V1NonProductionAnswerResumeController {
    /** Authenticates user/session and independently issues canonical event and answer digest. */
    public interface TrustedAnswerHost {
        AuthorizedAnswer requireAuthenticatedAnswer(AnswerRequest request);
    }
    public static final class AuthorizedAnswer {
        public final String consultationId, cdpId, threadId, waitingRunId;
        public final String checkpointId, questionId, deliveryId, userId;
        public final String eventId, correlationId, traceId, idempotencyKey, answerDigest;
        public AuthorizedAnswer(String consultationId, String cdpId, String threadId,
                String waitingRunId, String checkpointId, String questionId, String deliveryId,
                String userId, String eventId, String correlationId, String traceId,
                String idempotencyKey, String answerDigest) {
            this.consultationId=required(consultationId);
            this.cdpId=required(cdpId);
            this.threadId=required(threadId);
            this.waitingRunId=required(waitingRunId);
            this.checkpointId=required(checkpointId);
            this.questionId=required(questionId);
            this.deliveryId=required(deliveryId);
            this.userId=required(userId);
            this.eventId=required(eventId);
            this.correlationId=required(correlationId);
            this.traceId=required(traceId);
            this.idempotencyKey=required(idempotencyKey);
            this.answerDigest=required(answerDigest);
        }
        private static String required(String s) {
            if(s==null || s.trim().isEmpty()) throw new IllegalArgumentException("TRUSTED_ANSWER_IDENTITY_REQUIRED");
            return s;
        }
    }
    public static final class AnswerRequest {
        @NotBlank public String consultationId;
        @NotBlank public String checkpointId;
        @NotBlank public String answerText;
    }
    public static final class AnswerResponse {
        public final String consultationId, newRunId, status;
        public final boolean completeClinicalE2e=false, patientDeliveryAllowed=false;
        AnswerResponse(String consultationId, String runId) {
            this.consultationId=consultationId;
            this.newRunId=runId;
            this.status="F8_CONSUMED_U02_COMMITTED_RISK_REEVALUATION_PENDING";
        }
    }
    private final U07NonProductionAnswerResumeAdmission admission;
    private final U07NonProductionDurableF8Consume f8;
    private final U07NonProductionTrustedAnswerU02Bridge bridge;
    private final U02ClinicalFactApplicationService facts;
    private final TrustedAnswerHost host;
    public V1NonProductionAnswerResumeController(U07NonProductionAnswerResumeAdmission admission,
            U07NonProductionDurableF8Consume f8, U07NonProductionTrustedAnswerU02Bridge bridge,
            U02ClinicalFactApplicationService facts, TrustedAnswerHost host) {
        if(admission==null || f8==null || bridge==null || facts==null || host==null)
            throw new IllegalArgumentException("TRUSTED_ANSWER_HOST_REQUIRED");
        this.admission=admission;this.f8=f8;this.bridge=bridge;this.facts=facts;this.host=host;
    }
    @PostMapping("/answer-through-u02")
    public ResponseEntity<AnswerResponse> answer(@Valid @RequestBody AnswerRequest request) {
        CommittedAnswer completed=commitAnswer(request);
        return ResponseEntity.ok(new AnswerResponse(completed.authority.consultationId, completed.newRunId));
    }
    static final class CommittedAnswer {
        final AuthorizedAnswer authority;
        final String newRunId;
        final U02ExecutionResult clinicalFact;
        CommittedAnswer(AuthorizedAnswer authority, String newRunId, U02ExecutionResult clinicalFact) {
            this.authority=authority;this.newRunId=newRunId;this.clinicalFact=clinicalFact;
        }
    }
    CommittedAnswer commitAnswer(AnswerRequest request) {
        AuthorizedAnswer a=host.requireAuthenticatedAnswer(request);
        if(a==null || !request.consultationId.equals(a.consultationId)
                || !request.checkpointId.equals(a.checkpointId))
            throw new IllegalStateException("V1_ANSWER_AUTHORITY_MISMATCH");
        U02ExecutionCommand candidate=admission.requireWaitingAnswer(
                a.consultationId,a.cdpId,a.threadId,a.waitingRunId,a.checkpointId,
                a.questionId,a.deliveryId,a.userId,a.eventId,request.answerText,
                a.correlationId,a.traceId);
        String newRunId=f8.consume(candidate,a.checkpointId,a.idempotencyKey,a.answerDigest);
        U02ExecutionCommand authorized=bridge.requireAuthorizedNewRun(
                candidate,a.checkpointId,newRunId);
        U02ExecutionResult committed=facts.execute(authorized);
        if(committed==null || !"COMMITTED".equals(committed.status)
                || committed.commitResult==null
                || !"COMMITTED".equals(committed.commitResult.status))
            throw new IllegalStateException("V1_ANSWER_U02_COMMIT_REQUIRED");
        return new CommittedAnswer(a,newRunId,committed);
    }
}
