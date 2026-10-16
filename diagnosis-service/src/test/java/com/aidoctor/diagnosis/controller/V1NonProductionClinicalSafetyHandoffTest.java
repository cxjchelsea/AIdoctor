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
}
