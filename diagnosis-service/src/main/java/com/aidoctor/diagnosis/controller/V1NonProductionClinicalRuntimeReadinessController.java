package com.aidoctor.diagnosis.controller;

import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionGovernedRiskSafetyContinuation;
import com.aidoctor.diagnosis.runtime.u01.U01NaturalLanguageStartService;
import com.aidoctor.diagnosis.runtime.u02.U01U02NonProductionHandoffService;
import com.aidoctor.diagnosis.runtime.u03.U02U03GovernedSafetyBridge;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.LinkedHashMap;
import java.util.Map;

/** Nonproduction, opt-in, read-only wiring inventory. No clinical payloads or authority. */
@RestController
@Profile("!prod & !production")
@ConditionalOnProperty(prefix = "aidoctor.v1.clinical-e2e",
        name = "readiness-diagnostics-enabled", havingValue = "true", matchIfMissing = false)
@RequestMapping("/internal/nonproduction/v1/clinical")
public final class V1NonProductionClinicalRuntimeReadinessController {
    private final ApplicationContext context;

    public V1NonProductionClinicalRuntimeReadinessController(ApplicationContext context) {
        this.context = context;
    }

    @GetMapping("/runtime-readiness")
    public Map<String, Object> readiness() {
        Map<String, Boolean> dependencies = new LinkedHashMap<String, Boolean>();
        dependencies.put("u01", present(U01NaturalLanguageStartService.class));
        dependencies.put("u02TrustedHandoff", present(U01U02NonProductionHandoffService.class));
        dependencies.put("u03U04Bridge", present(U02U03GovernedSafetyBridge.class));
        dependencies.put("u03U04Executor", present(U07NonProductionGovernedRiskSafetyContinuation.class));
        dependencies.put("independentClinicalHost", present(
                V1NonProductionClinicalSafetyHandoffController.AuthorizedClinicalHost.class));
        dependencies.put("u01U04HttpEntry", present(V1NonProductionClinicalSafetyHandoffController.class));
        boolean ready = true;
        for (Boolean value : dependencies.values()) ready &= value.booleanValue();
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("status", ready ? "COMPONENTS_PRESENT_NOT_E2E_VERIFIED" : "MISSING_REQUIRED_COMPONENTS");
        response.put("components", dependencies);
        response.put("fullClinicalE2eVerified", false);
        response.put("patientDeliveryAllowed", false);
        return response;
    }

    private boolean present(Class<?> type) {
        return context.getBeanNamesForType(type, false, false).length > 0;
    }
}
