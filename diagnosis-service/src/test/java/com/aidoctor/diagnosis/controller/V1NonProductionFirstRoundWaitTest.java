package com.aidoctor.diagnosis.controller;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class V1NonProductionFirstRoundWaitTest {
    @Test
    void cannotConstructFirstRoundWithoutIndependentClinicalOwners() {
        assertThrows(IllegalArgumentException.class, () ->
                new V1NonProductionFirstRoundWaitController(
                        null, null, null, null, null, null));
    }
}
