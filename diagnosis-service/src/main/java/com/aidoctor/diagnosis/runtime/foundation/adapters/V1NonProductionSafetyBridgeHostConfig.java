package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.governance.CapabilityCallTraceRepository;
import com.aidoctor.diagnosis.runtime.u03.U02U03GovernedSafetyBridge;
import com.aidoctor.diagnosis.runtime.u03.U03NonProductionAdmissionService;
import com.aidoctor.diagnosis.runtime.u04.U04NonProductionApplicationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Makes the gated HTTP clinical entry use the existing governed safety bridge.
 * No acceptance authority, clinical decision, risk release or U04 policy is
 * created here. Those owners must be separately available in the host.
 */
@Configuration
@Profile("!prod & !production")
@ConditionalOnExpression("${aidoctor.v1.clinical-e2e.u01-u04-http-enabled:false} or " +
        "${aidoctor.v1.clinical-e2e.first-round-wait-http-enabled:false} or " +
        "${aidoctor.v1.clinical-e2e.answer-risk-http-enabled:false}")
public class V1NonProductionSafetyBridgeHostConfig {
    @Bean
    @ConditionalOnMissingBean(U02U03GovernedSafetyBridge.class)
    @ConditionalOnBean({
            CDPRepository.class,
            CapabilityCallTraceRepository.class,
            U02U03GovernedSafetyBridge.AcceptedEvidenceAuthority.class,
            U04NonProductionApplicationService.class
    })
    public U02U03GovernedSafetyBridge v1NonProductionHttpSafetyBridge(
            CDPRepository cdps,
            CapabilityCallTraceRepository traces,
            U02U03GovernedSafetyBridge.AcceptedEvidenceAuthority evidence,
            U04NonProductionApplicationService u04) {
        return new U02U03GovernedSafetyBridge(
                cdps, traces, evidence, new U03NonProductionAdmissionService(), u04);
    }
}
