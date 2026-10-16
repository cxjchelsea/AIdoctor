package com.aidoctor.diagnosis.controller;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class V1NonProductionClinicalSafetyHandoffTest {
    @Test
    void rejectProductionIdentifiersBeforeRiskOrSafetyExecution() {
        for (String forbidden : new String[] {
                null, "", " ", "prod", "PRODUCTION", "prod-eu",
                "production-test", " PROD-us "}) {
            assertThrows(IllegalStateException.class,
                    () -> V1NonProductionClinicalSafetyHandoffController
                            .requireNonProductionEnvironment(forbidden));
        }
    }

    @Test
    void permitOnlyExplicitNonproductionExecutionEnvironment() {
        assertDoesNotThrow(() -> V1NonProductionClinicalSafetyHandoffController
                .requireNonProductionEnvironment("nonproduction-local"));
        assertDoesNotThrow(() -> V1NonProductionClinicalSafetyHandoffController
                .requireNonProductionEnvironment("synthetic-ci"));
    }

    @Test
    void failClosedWithoutTrustedClinicalDependencies() {
        assertThrows(IllegalArgumentException.class,
                () -> new V1NonProductionClinicalSafetyHandoffController(
                        null, null, null, null));
    }
    @Test
    void u03AuthorizationMustMatchCommittedConsultationRunAndClinicalVersion() {
        com.aidoctor.diagnosis.runtime.u01.U01Result started =
                org.mockito.Mockito.mock(com.aidoctor.diagnosis.runtime.u01.U01Result.class);
        org.mockito.Mockito.when(started.getConsultationId()).thenReturn("consult-A");
        org.mockito.Mockito.when(started.getRunId()).thenReturn("run-A");
        org.mockito.Mockito.when(started.getCdpId()).thenReturn("cdp-A");
        com.aidoctor.contracts.v1.StateTypes.CommitResult commit =
                new com.aidoctor.contracts.v1.StateTypes.CommitResult();
        commit.status = "COMMITTED";
        commit.committedVersion = Integer.valueOf(2);
        com.aidoctor.diagnosis.runtime.u02.U02ExecutionResult fact =
                new com.aidoctor.diagnosis.runtime.u02.U02ExecutionResult(
                        "COMMITTED", null, "synthetic-call", null, null, commit, null);
        com.aidoctor.diagnosis.runtime.u03.U03ExplicitNonProductionReleaseRefs releases =
                com.aidoctor.diagnosis.runtime.u03.U03ExplicitNonProductionReleaseRefs.gateCFrozenSet();
        com.aidoctor.diagnosis.runtime.u04.U04ScopeContext scope =
                com.aidoctor.diagnosis.runtime.u04.U04ScopeContext.current(true, true);
        V1NonProductionClinicalSafetyHandoffController.AuthorizedExecution accepted =
                new V1NonProductionClinicalSafetyHandoffController.AuthorizedExecution(
                        new com.aidoctor.diagnosis.runtime.u03.U03ExecutionCommand(
                                "consult-A", "thread-A", "run-A", "event-A", "cdp-A", 2,
                                "corr-A", "trace-A"),
                        releases, "synthetic-ci", scope);
        assertDoesNotThrow(() -> V1NonProductionClinicalSafetyHandoffController
                .requireBoundAuthorizedExecution(started, fact, accepted));
        for (String[] identity : new String[][] {
                {"consult-WRONG", "run-A", "cdp-A", "2"},
                {"consult-A", "run-WRONG", "cdp-A", "2"},
                {"consult-A", "run-A", "cdp-WRONG", "2"},
                {"consult-A", "run-A", "cdp-A", "3"}}) {
            V1NonProductionClinicalSafetyHandoffController.AuthorizedExecution wrong =
                    new V1NonProductionClinicalSafetyHandoffController.AuthorizedExecution(
                            new com.aidoctor.diagnosis.runtime.u03.U03ExecutionCommand(
                                    identity[0], "thread-A", identity[1], "event-A",
                                    identity[2], Integer.parseInt(identity[3]),
                                    "corr-A", "trace-A"),
                            releases, "synthetic-ci", scope);
            assertThrows(IllegalStateException.class, () ->
                    V1NonProductionClinicalSafetyHandoffController
                            .requireBoundAuthorizedExecution(started, fact, wrong));
        }
    }

    @Test
    void rejectNonFrozenClinicalReleaseOrUnestablishedScopeBeforeRiskExecution() {
        com.aidoctor.diagnosis.runtime.u03.U03ExecutionCommand command =
                new com.aidoctor.diagnosis.runtime.u03.U03ExecutionCommand(
                        "consult-A", "thread-A", "run-A", "event-A",
                        "cdp-A", 2, "corr-A", "trace-A");
        com.aidoctor.diagnosis.runtime.u03.U03ExplicitNonProductionReleaseRefs frozen =
                com.aidoctor.diagnosis.runtime.u03.U03ExplicitNonProductionReleaseRefs.gateCFrozenSet();
        com.aidoctor.diagnosis.runtime.u04.U04ScopeContext applicable =
                com.aidoctor.diagnosis.runtime.u04.U04ScopeContext.current(true, true);
        assertDoesNotThrow(() -> V1NonProductionClinicalSafetyHandoffController
                .requireFrozenSafetyRelease(
                        new V1NonProductionClinicalSafetyHandoffController.AuthorizedExecution(
                                command, frozen, "synthetic-ci", applicable)));
        com.aidoctor.diagnosis.runtime.u03.U03ExplicitNonProductionReleaseRefs wrong =
                new com.aidoctor.diagnosis.runtime.u03.U03ExplicitNonProductionReleaseRefs(
                        "not-approved", frozen.getRuleReleaseRef(),
                        frozen.getCoverageContractRef(), frozen.getPolicyReleaseRef(),
                        frozen.getPolicyPairRef());
        assertThrows(IllegalStateException.class,
                () -> V1NonProductionClinicalSafetyHandoffController.requireFrozenSafetyRelease(
                        new V1NonProductionClinicalSafetyHandoffController.AuthorizedExecution(
                                command, wrong, "synthetic-ci", applicable)));
        for (com.aidoctor.diagnosis.runtime.u04.U04ScopeContext scope :
                new com.aidoctor.diagnosis.runtime.u04.U04ScopeContext[] {
                        com.aidoctor.diagnosis.runtime.u04.U04ScopeContext.current(false, true),
                        com.aidoctor.diagnosis.runtime.u04.U04ScopeContext.current(true, false),
                        new com.aidoctor.diagnosis.runtime.u04.U04ScopeContext(
                                true, true, "not-approved")}) {
            assertThrows(IllegalStateException.class,
                    () -> V1NonProductionClinicalSafetyHandoffController.requireFrozenSafetyRelease(
                            new V1NonProductionClinicalSafetyHandoffController.AuthorizedExecution(
                                    command, frozen, "synthetic-ci", scope)));
        }
    }

}
