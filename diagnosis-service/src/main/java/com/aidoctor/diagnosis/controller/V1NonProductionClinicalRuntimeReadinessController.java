package com.aidoctor.diagnosis.controller;

import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionGovernedRiskSafetyContinuation;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionPhysicalFollowupCoordinator;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionDurableF8Consume;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionAnswerResumeAdmission;
import com.aidoctor.diagnosis.runtime.foundation.adapters.U07NonProductionTrustedAnswerU02Bridge;
import com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery;
import com.aidoctor.diagnosis.runtime.u01.U01NaturalLanguageStartService;
import com.aidoctor.diagnosis.runtime.u02.U01U02NonProductionHandoffService;
import com.aidoctor.diagnosis.runtime.u03.U02U03GovernedSafetyBridge;
import com.aidoctor.diagnosis.runtime.u02.capability.C01U02CapabilityGateway;
import com.aidoctor.diagnosis.runtime.u03.U03CdpAcceptedEvidenceAuthority;
import com.aidoctor.diagnosis.runtime.u03.U03CdpGateCClinicalInputPort;
import com.aidoctor.diagnosis.runtime.u03.U03GateCClinicalInputPort;
import com.aidoctor.diagnosis.runtime.u03.U03GovernedCandidateGateway;
import com.aidoctor.diagnosis.state.committer.StateCommitter;
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
        dependencies.put("trustedTraceAuthority", present(
                U01U02NonProductionHandoffService.TrustedTraceAuthority.class));
        dependencies.put("governedC01Gateway", present(C01U02CapabilityGateway.class));
        dependencies.put("stateCommitter", present(StateCommitter.class));
        dependencies.put("independentClinicalEvidenceAttestation", present(
                U03CdpAcceptedEvidenceAuthority.ClinicalEvidenceAttestation.class));
        dependencies.put("verifiedGateCScopeAuthority", present(
                U03CdpGateCClinicalInputPort.ScopeAuthority.class));
        dependencies.put("governedGateCInput", present(U03GateCClinicalInputPort.class));
        dependencies.put("governedRiskCandidateGateway", present(U03GovernedCandidateGateway.class));
        dependencies.put("u02TrustedHandoff", present(U01U02NonProductionHandoffService.class));
        dependencies.put("u03U04Bridge", present(U02U03GovernedSafetyBridge.class));
        dependencies.put("u03U04Executor", present(U07NonProductionGovernedRiskSafetyContinuation.class));
        dependencies.put("independentClinicalHost", present(
                V1NonProductionClinicalSafetyHandoffController.AuthorizedClinicalHost.class));
        dependencies.put("u01U04HttpEntry", present(V1NonProductionClinicalSafetyHandoffController.class));
        dependencies.put("physicalFollowupCoordinator", present(U07NonProductionPhysicalFollowupCoordinator.class));
        dependencies.put("independentQuestionHost", present(U07NonProductionPhysicalFollowupCoordinator.AuthorizedQuestionHost.class));
        dependencies.put("confirmedDeliveryWaitRecovery", present(U07NonProductionDeliveryWaitRecovery.class));
        dependencies.put("firstRoundOwner", present(V1NonProductionFirstRoundWaitController.FirstRoundOwner.class));
        dependencies.put("firstRoundHttpEntry", present(V1NonProductionFirstRoundWaitController.class));
        dependencies.put("trustedAnswerHost", present(V1NonProductionAnswerResumeController.TrustedAnswerHost.class));
        dependencies.put("f8AnswerAdmission", present(U07NonProductionAnswerResumeAdmission.class));
        dependencies.put("durableF8Consumer", present(U07NonProductionDurableF8Consume.class));
        dependencies.put("trustedResumedRunBridge", present(U07NonProductionTrustedAnswerU02Bridge.class));
        dependencies.put("f8AnswerHttpEntry", present(V1NonProductionAnswerResumeController.class));
        dependencies.put("resumedRiskOwner", present(V1NonProductionAnswerRiskReevaluationController.ResumedRiskOwner.class));
        dependencies.put("resumedRiskHttpEntry", present(V1NonProductionAnswerRiskReevaluationController.class));
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
