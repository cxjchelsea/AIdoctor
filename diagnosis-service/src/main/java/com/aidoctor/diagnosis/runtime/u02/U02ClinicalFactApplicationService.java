package com.aidoctor.diagnosis.runtime.u02;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.dto.capability.c01.C01U02CapabilityResponse;
import com.aidoctor.diagnosis.runtime.governance.CapabilityTraceService;
import com.aidoctor.diagnosis.runtime.u02.capability.C01U02CapabilityGateway;
import com.aidoctor.diagnosis.runtime.u02.d05.U02DependencyInvalidationHook;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * U02 component application path:
 * governed capability candidate -> business decision -> typed proposal -> P01 commit -> minimum D05.
 * This class is deliberately not an external HTTP cutover or production activation point.
 */
public final class U02ClinicalFactApplicationService {
    private final C01U02CapabilityGateway capabilityGateway;
    private final U02ClinicalFactBusinessOwner businessOwner;
    private final U02ClinicalFactProposalFactory proposalFactory;
    private final U02ClinicalFactCommitService commitService;
    private final U02DependencyInvalidationHook invalidationHook;
    private final CapabilityTraceService traceService;

    public U02ClinicalFactApplicationService(
            C01U02CapabilityGateway capabilityGateway,
            U02ClinicalFactBusinessOwner businessOwner,
            U02ClinicalFactProposalFactory proposalFactory,
            U02ClinicalFactCommitService commitService,
            U02DependencyInvalidationHook invalidationHook,
            CapabilityTraceService traceService) {
        this.capabilityGateway = require(capabilityGateway, "capabilityGateway");
        this.businessOwner = require(businessOwner, "businessOwner");
        this.proposalFactory = require(proposalFactory, "proposalFactory");
        this.commitService = require(commitService, "commitService");
        this.invalidationHook = require(invalidationHook, "invalidationHook");
        this.traceService = require(traceService, "traceService");
    }

    public U02ExecutionResult execute(U02ExecutionCommand command) {
        if (command == null) throw new IllegalArgumentException("command is required");
        String capabilityCallId = "c01-u02-call-" + UUID.randomUUID().toString();
        traceService.start(
                capabilityCallId,
                command.consultationId,
                command.threadId,
                command.runId,
                command.eventId,
                "U02",
                C01U02CapabilityGateway.CAPABILITY_ID,
                C01U02CapabilityGateway.BINDING_ID,
                Integer.valueOf(command.baseClinicalStateVersion));

        final C01U02CapabilityGateway.GovernedResult governed;
        try {
            governed = capabilityGateway.interpret(
                    command.userId,
                    command.rawClinicalText,
                    command.consultationId,
                    command.eventId,
                    command.sourceType);
        } catch (RuntimeException failure) {
            traceService.fail(capabilityCallId, "C01_U02_INVOCATION_FAILED");
            throw failure;
        }

        C01U02CapabilityResponse capabilityResult = governed.getResponse();
        String capabilityResultRef = "c01-u02-result-" + command.eventId;
        U02ClinicalFactDecision decision = businessOwner.decide(
                command.consultationId, command.eventId, capabilityResult);

        if (decision.isClarificationRequired()) {
            traceService.succeed(
                    capabilityCallId,
                    capabilityResultRef,
                    decision.getDecisionId(),
                    null,
                    null,
                    Integer.valueOf(command.baseClinicalStateVersion));
            return new U02ExecutionResult(
                    "CLARIFICATION_REQUIRED",
                    decision.getReasonCode(),
                    capabilityCallId,
                    decision,
                    null,
                    null,
                    Collections.<U02DependencyInvalidationHook.InvalidationRecord>emptyList());
        }

        U02ClinicalFactProposal proposal = proposalFactory.create(
                command.cdpId,
                command.baseClinicalStateVersion,
                command.traceId,
                command.correlationId,
                decision,
                governed.getAuthorizedBinding());
        StateTypes.CommitResult commitResult = commitService.commit(proposal);
        Integer versionAfter = commitResult == null ? null : commitResult.committedVersion;

        traceService.succeed(
                capabilityCallId,
                capabilityResultRef,
                decision.getDecisionId(),
                proposal.getProposalId(),
                commitResult == null ? null : commitResult.patchId,
                versionAfter);

        if (commitResult == null || !"COMMITTED".equals(commitResult.status)) {
            return new U02ExecutionResult(
                    "COMMIT_NOT_APPLIED",
                    commitResult == null ? "P01_COMMIT_RESULT_MISSING" : commitResult.reasonCode,
                    capabilityCallId,
                    decision,
                    proposal,
                    commitResult,
                    Collections.<U02DependencyInvalidationHook.InvalidationRecord>emptyList());
        }

        int committedVersion = commitResult.committedVersion == null
                ? command.baseClinicalStateVersion : commitResult.committedVersion.intValue();
        String sourceFactRef = decision.getAcceptedObservations().get(0).getObservationId();
        List<U02DependencyInvalidationHook.InvalidationRecord> invalidations = invalidationHook.invalidate(
                command.consultationId,
                command.eventId,
                sourceFactRef,
                committedVersion,
                command.registeredDependencies);

        return new U02ExecutionResult(
                "COMMITTED",
                commitResult.reasonCode,
                capabilityCallId,
                decision,
                proposal,
                commitResult,
                invalidations);
    }

    private static <T> T require(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
