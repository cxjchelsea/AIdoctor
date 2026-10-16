package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.governance.CapabilityCallTraceRepository;
import com.aidoctor.diagnosis.runtime.u03.*;
import com.aidoctor.diagnosis.runtime.u04.U04NonProductionApplicationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Explicit opt-in: no clinical acceptance authority or C02 executor is fabricated. */
@Configuration
@Profile("!prod & !production")
@ConditionalOnProperty(prefix="aidoctor.v1.clinical-e2e",
        name="u02-u03-safety-enabled", havingValue="true", matchIfMissing=false)
public class U02U03SafetyHostConfig {
    @Bean
    @ConditionalOnBean({CDPRepository.class, CapabilityCallTraceRepository.class,
            U02U03GovernedSafetyBridge.AcceptedEvidenceAuthority.class,
            U04NonProductionApplicationService.class})
    public U02U03GovernedSafetyBridge u02U03SafetyBridge(
            CDPRepository cdps,
            CapabilityCallTraceRepository traces,
            U02U03GovernedSafetyBridge.AcceptedEvidenceAuthority evidence,
            U04NonProductionApplicationService u04) {
        return new U02U03GovernedSafetyBridge(
                cdps, traces, evidence, new U03NonProductionAdmissionService(), u04);
    }
}
