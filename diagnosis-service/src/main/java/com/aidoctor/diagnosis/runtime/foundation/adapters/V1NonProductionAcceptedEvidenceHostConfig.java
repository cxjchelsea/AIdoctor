package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.u03.U02U03GovernedSafetyBridge;
import com.aidoctor.diagnosis.runtime.u03.U03CdpAcceptedEvidenceAuthority;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Reuse persisted U02 fact readback for U03, without manufacturing acceptance.
 * Independent host attestation remains a mandatory dependency, so disabling or
 * omitting it cannot accidentally admit clinical evidence.
 */
@Configuration
@Profile("!prod & !production")
@ConditionalOnProperty(prefix = "aidoctor.v1.clinical-e2e",
        name = "u01-u04-http-enabled", havingValue = "true", matchIfMissing = false)
public class V1NonProductionAcceptedEvidenceHostConfig {
    @Bean
    @ConditionalOnMissingBean(U02U03GovernedSafetyBridge.AcceptedEvidenceAuthority.class)
    @ConditionalOnBean({CDPRepository.class,
            U03CdpAcceptedEvidenceAuthority.ClinicalEvidenceAttestation.class})
    public U02U03GovernedSafetyBridge.AcceptedEvidenceAuthority
            v1NonProductionAcceptedClinicalEvidence(
                    CDPRepository cdps,
                    U03CdpAcceptedEvidenceAuthority.ClinicalEvidenceAttestation attestation) {
        return new U03CdpAcceptedEvidenceAuthority(cdps, attestation);
    }
}
