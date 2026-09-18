package com.aidoctor.diagnosis.runtime.u03;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * R5/N13 verification harness.
 *
 * <p>S14 is intentionally producer-only and the repository has no authorized U04
 * consumer wiring in CD-07. This harness models an attempted downstream owner
 * execution and proves authorization is checked before the U04 owner or any
 * additional clinical mutation/output can run.</p>
 */
class U03UnauthorizedU04AttemptTest {

    @Test
    void n13AttemptedU04OwnerExecutionWithoutAuthorizationFailsClosedWithNoOwnerCallOrOutput() {
        U03OutboundHandoff handoff = committedProducerHandoff();
        AtomicInteger u04OwnerCalls = new AtomicInteger();
        AtomicInteger clinicalMutationAttempts = new AtomicInteger();
        Object[] producedU04Output = new Object[1];

        UnauthorizedU04ExecutionHarness harness = new UnauthorizedU04ExecutionHarness(false);

        IllegalStateException failure = assertThrows(IllegalStateException.class, () ->
                producedU04Output[0] = harness.attempt(
                        handoff,
                        () -> {
                            u04OwnerCalls.incrementAndGet();
                            clinicalMutationAttempts.incrementAndGet();
                            return new Object();
                        }));

        assertEquals("U04_IMPLEMENTATION_AUTHORIZATION_NOT_GRANTED", failure.getMessage());
        assertEquals(0, u04OwnerCalls.get());
        assertEquals(0, clinicalMutationAttempts.get());
        assertNull(producedU04Output[0]);
        assertEquals("COMMITTED", handoff.getCommitStatus());
        assertEquals("CAUTION", handoff.getDispositionCode());
    }

    private static U03OutboundHandoff committedProducerHandoff() {
        return new U03OutboundHandoff(
                "consult-n13",
                "cdp-n13",
                7,
                Integer.valueOf(8),
                "thread-n13",
                "run-n13",
                "event-n13",
                "corr-n13",
                "trace-n13",
                "ci-nonprod-e2e",
                U03NonProductionExecutionContext.BINDING_MODE,
                U03RiskAssessmentCandidate.VALID,
                null,
                Collections.<String>emptyList(),
                "SYNTHETIC_UNCERTAINTY",
                "decision-n13",
                U03RiskAssessmentCandidate.VALID,
                "CAUTION",
                "SYNTHETIC_FROZEN_D09_DECISION",
                U03GovernedCandidateGateway.BINDING_ID,
                Arrays.asList(
                        U03GovernedCandidateGateway.BINDING_ID,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_KNOWLEDGE_RELEASE_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF),
                "acceptance-n13",
                Collections.singletonList("evidence-n13"),
                Collections.singletonList("source-n13"),
                Collections.singletonList("provenance-n13"),
                "proposal-n13",
                "COMMITTED",
                null,
                "audit-n13");
    }

    private interface U04OwnerAttempt {
        Object execute();
    }

    private static final class UnauthorizedU04ExecutionHarness {
        private final boolean u04ImplementationAuthorized;

        private UnauthorizedU04ExecutionHarness(boolean u04ImplementationAuthorized) {
            this.u04ImplementationAuthorized = u04ImplementationAuthorized;
        }

        private Object attempt(U03OutboundHandoff handoff, U04OwnerAttempt owner) {
            if (handoff == null) throw new IllegalArgumentException("handoff is required");
            if (owner == null) throw new IllegalArgumentException("owner is required");
            if (!u04ImplementationAuthorized) {
                throw new IllegalStateException("U04_IMPLEMENTATION_AUTHORIZATION_NOT_GRANTED");
            }
            return owner.execute();
        }
    }
}
