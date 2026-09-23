package com.aidoctor.diagnosis.runtime.u05.verification;

import org.junit.jupiter.api.Test;

class U05EvidenceHarnessVerificationTest {
    @Test void hg001PolicyExpectationGapDetectorSelfTest() throws Exception {
        U05VerificationSupport.verifyHarnessGapDetector();
    }

    @Test void fixtureManifestIsSyntheticAndNonPhi() throws Exception {
        U05VerificationSupport.verifyStaticInputsSyntheticNonPhi();
    }
}
