package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.u03.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Explicit opt-in for real CDP-backed Gate-C evidence input. No implicit
 * evidence acceptance, patient scope, measurement authority or C02 permission
 * is issued by this host configuration.
 */
@Configuration
@Profile("!prod & !production")
@ConditionalOnProperty(prefix="aidoctor.v1.clinical-e2e",
        name="cdp-gatec-input-enabled", havingValue="true", matchIfMissing=false)
public class U03CdpGateCNonProductionHostConfig {
    @Bean
    @ConditionalOnBean({CDPRepository.class,
            U03CdpAcceptedEvidenceAuthority.ClinicalEvidenceAttestation.class})
    public U02U03GovernedSafetyBridge.AcceptedEvidenceAuthority cdpAcceptedEvidence(
            CDPRepository cdps,
            U03CdpAcceptedEvidenceAuthority.ClinicalEvidenceAttestation evidenceAttestation) {
        return new U03CdpAcceptedEvidenceAuthority(cdps, evidenceAttestation);
    }

    @Bean
    @ConditionalOnBean({CDPRepository.class,
            U03CdpGateCClinicalInputPort.ScopeAuthority.class})
    public U03GateCClinicalInputPort cdpGateCClinicalInput(
            CDPRepository cdps, U03CdpGateCClinicalInputPort.ScopeAuthority scopeAuthority) {
        return new U03CdpGateCClinicalInputPort(cdps, scopeAuthority);
    }
}
