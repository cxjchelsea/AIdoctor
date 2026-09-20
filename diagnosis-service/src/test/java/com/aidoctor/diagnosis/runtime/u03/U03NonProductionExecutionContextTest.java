package com.aidoctor.diagnosis.runtime.u03;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class U03NonProductionExecutionContextTest {
    @Test
    void bindsExactExecutionIdentityToFrozenGateCReleaseSet() {
        U03ExecutionCommand command = command();
        U03ExplicitNonProductionReleaseRefs refs = U03ExplicitNonProductionReleaseRefs.gateCFrozenSet();

        U03NonProductionExecutionContext context =
                new U03NonProductionExecutionContext(command, refs, "ci-nonprod");

        assertSame(command, context.getCommand());
        assertSame(refs, context.getReleaseRefs());
        assertEquals("ci-nonprod", context.getEnvironmentId());
        assertEquals("EXPLICIT_NON_PRODUCTION_BINDING_ONLY", context.getBindingMode());
        assertEquals("thread-1", context.getCommand().threadId);
        assertEquals("run-1", context.getCommand().runId);
        assertEquals("event-1", context.getCommand().eventId);
        assertEquals(7, context.getCommand().clinicalStateVersion);
    }

    @Test
    void rejectsUnauthorizedExactReleaseSubstitution() {
        U03ExplicitNonProductionReleaseRefs substituted = new U03ExplicitNonProductionReleaseRefs(
                "KR-U03-SOURCE-001@0.1.1-candidate",
                U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF);

        assertThrows(IllegalStateException.class, () ->
                new U03NonProductionExecutionContext(command(), substituted, "test-nonprod"));
    }

    @Test
    void rejectsAliasReleaseSelection() {
        assertThrows(IllegalArgumentException.class, () ->
                new U03ExplicitNonProductionReleaseRefs(
                        "latest",
                        U03ExplicitNonProductionReleaseRefs.GATE_C_RULE_RELEASE_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_COVERAGE_CONTRACT_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_RELEASE_REF,
                        U03ExplicitNonProductionReleaseRefs.GATE_C_POLICY_PAIR_REF));
    }

    @Test
    void rejectsProductionEnvironment() {
        assertThrows(IllegalStateException.class, () ->
                new U03NonProductionExecutionContext(
                        command(),
                        U03ExplicitNonProductionReleaseRefs.gateCFrozenSet(),
                        "production"));
    }

    private static U03ExecutionCommand command() {
        return new U03ExecutionCommand(
                "consultation-1",
                "thread-1",
                "run-1",
                "event-1",
                "cdp-1",
                7,
                "correlation-1",
                "trace-1");
    }
}
