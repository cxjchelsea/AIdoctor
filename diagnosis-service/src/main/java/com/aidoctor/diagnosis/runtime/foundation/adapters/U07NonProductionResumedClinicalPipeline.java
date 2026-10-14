package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.runtime.u02.U02ClinicalFactApplicationService;
import com.aidoctor.diagnosis.runtime.u02.U02ExecutionCommand;
import com.aidoctor.diagnosis.runtime.u02.U02ExecutionResult;
import com.aidoctor.diagnosis.runtime.u03.U02U03GovernedSafetyBridge;
import com.aidoctor.diagnosis.runtime.u03.U03ExecutionCommand;
import com.aidoctor.diagnosis.runtime.u03.U03ExplicitNonProductionReleaseRefs;
import com.aidoctor.diagnosis.runtime.u03.U03NonProductionExecutionContext;
import java.util.Objects;

/**
 * Non-production business continuation only after a real durable F8 consumer
 * has authorized the fresh Run/Event. Runs existing U02 -> governed U03
 * admission; it cannot invent accepted evidence, a U03 risk result, or U04 gate.
 */
public final class U07NonProductionResumedClinicalPipeline {
    private final U07NonProductionTrustedAnswerU02Bridge freshRunAuthority;
    private final U02ClinicalFactApplicationService u02;
    private final U02U03GovernedSafetyBridge safety;

    public U07NonProductionResumedClinicalPipeline(
            U07NonProductionTrustedAnswerU02Bridge freshRunAuthority,
            U02ClinicalFactApplicationService u02,
            U02U03GovernedSafetyBridge safety) {
        this.freshRunAuthority = Objects.requireNonNull(freshRunAuthority);
        this.u02 = Objects.requireNonNull(u02);
        this.safety = Objects.requireNonNull(safety);
    }

    public Result commitAnswerAndAdmitRisk(U02ExecutionCommand waitingCandidate,
            String checkpointId, String newRunId,
            U03ExplicitNonProductionReleaseRefs releases, String environmentId) {
        if (releases == null || environmentId == null)
            throw new IllegalStateException("U07_F8_CLINICAL_RELEASE_REQUIRED");
        String env = environmentId.trim().toLowerCase(java.util.Locale.ROOT);
        if (env.isEmpty() || env.equals("prod") || env.equals("production")
                || env.startsWith("prod-") || env.startsWith("production-"))
            throw new IllegalStateException("U07_F8_CLINICAL_PRODUCTION_FORBIDDEN");
        // This gate must be backed by an independently persisted F8 consumption
        // authority; there is no default/boolean success implementation here.
        U02ExecutionCommand fresh = freshRunAuthority.requireAuthorizedNewRun(
                waitingCandidate, checkpointId, newRunId);
        U02ExecutionResult fact = u02.execute(fresh);
        if (fact == null || !"COMMITTED".equals(fact.status)
                || fact.commitResult == null
                || !"COMMITTED".equals(fact.commitResult.status)
                || fact.commitResult.committedVersion == null)
            throw new IllegalStateException("U07_F8_U02_FACT_NOT_COMMITTED");
        U03ExecutionCommand risk = new U03ExecutionCommand(
                fresh.consultationId, fresh.threadId, fresh.runId, fresh.eventId,
                fresh.cdpId, fact.commitResult.committedVersion.intValue(),
                fresh.correlationId, fresh.traceId);
        U03NonProductionExecutionContext accepted =
                safety.requireU03Admission(fact, risk, releases, environmentId);
        if (accepted == null)
            throw new IllegalStateException("U07_F8_U03_ADMISSION_MISSING");
        return new Result(fact, risk, accepted);
    }

    public static final class Result {
        public final U02ExecutionResult fact;
        public final U03ExecutionCommand riskCommand;
        public final U03NonProductionExecutionContext admittedRisk;
        private Result(U02ExecutionResult fact, U03ExecutionCommand riskCommand,
                U03NonProductionExecutionContext admittedRisk) {
            this.fact = fact;
            this.riskCommand = riskCommand;
            this.admittedRisk = admittedRisk;
        }
    }
}
