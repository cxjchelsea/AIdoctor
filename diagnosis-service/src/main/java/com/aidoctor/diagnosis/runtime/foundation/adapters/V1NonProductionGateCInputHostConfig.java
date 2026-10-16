package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.u03.U03CdpGateCClinicalInputPort;
import com.aidoctor.diagnosis.runtime.u03.U03GateCClinicalInputPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Bind accepted, persisted U02 evidence to U03 Gate-C.
 * A verified external scope authority is mandatory: no default age, setting,
 * region, negative clinical fact or clinical rule outcome is synthesized.
 */
@Configuration
@Profile("!prod & !production")
@ConditionalOnExpression("${aidoctor.v1.clinical-e2e.u01-u04-http-enabled:false} or " +
        "${aidoctor.v1.clinical-e2e.first-round-wait-http-enabled:false} or " +
        "${aidoctor.v1.clinical-e2e.answer-risk-http-enabled:false}")
public class V1NonProductionGateCInputHostConfig {
    @Bean
    @ConditionalOnMissingBean(U03GateCClinicalInputPort.class)
    @ConditionalOnBean({CDPRepository.class, U03CdpGateCClinicalInputPort.ScopeAuthority.class})
    public U03GateCClinicalInputPort v1NonProductionGateCInput(
            CDPRepository cdps,
            U03CdpGateCClinicalInputPort.ScopeAuthority scopeAuthority) {
        return new U03CdpGateCClinicalInputPort(cdps, scopeAuthority);
    }
}
