package com.aidoctor.diagnosis.runtime.u02;

import com.aidoctor.diagnosis.controller.V1NonProductionAnswerRiskReevaluationController;
import com.aidoctor.diagnosis.controller.V1NonProductionFirstRoundWaitController;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Test classpath ONLY; reports the exact nonproduction synthetic manual host. */
@RestController
@Profile("clinical-e2e-test")
@RequestMapping("/internal/nonproduction/v1/clinical")
public final class V1SyntheticManualReadinessController {
    private final ApplicationContext context;
    public V1SyntheticManualReadinessController(ApplicationContext context) {
        this.context=context;
    }
    @GetMapping("/runtime-readiness")
    public Map<String,Object> readiness() {
        Map<String,Boolean> components=new LinkedHashMap<>();
        components.put("firstRoundHttpEntry",present(V1NonProductionFirstRoundWaitController.class));
        components.put("f8ResumedRiskHttpEntry",present(V1NonProductionAnswerRiskReevaluationController.class));
        components.put("durableWaitRecovery",present(U07NonProductionDeliveryWaitRecovery.class));
        components.put("physicalCdpRepository",present(CDPRepository.class));
        boolean all=true;
        for(Boolean value:components.values()) all &= value.booleanValue();
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("status",all ? "COMPONENTS_PRESENT_NOT_E2E_VERIFIED" : "MISSING_REQUIRED_COMPONENTS");
        result.put("components",components);
        result.put("fullClinicalE2eVerified",Boolean.FALSE);
        result.put("patientDeliveryAllowed",Boolean.FALSE);
        result.put("syntheticFixturesOnly",Boolean.TRUE);
        return result;
    }
    private boolean present(Class<?> clazz) {
        return context.getBeanNamesForType(clazz,false,false).length>0;
    }
}
