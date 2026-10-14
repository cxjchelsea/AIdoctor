package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.runtime.u02.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * No automatic clinical execution: host must explicitly install the governed
 * U02 application, P01 commit policy and independently trusted trace authority.
 */
@Configuration
@Profile("!prod & !production")
@ConditionalOnProperty(prefix = "aidoctor.v1.clinical-e2e",
        name = "u01-u02-handoff-enabled", havingValue = "true", matchIfMissing = false)
public class U01U02NonProductionHostConfig {
    @Bean
    @ConditionalOnBean({U01U02TrustedStartContext.class,
            U01U02NonProductionHandoffService.TrustedTraceAuthority.class,
            U02ClinicalFactApplicationService.class})
    public U01U02NonProductionHandoffService u01U02Handoff(
            U01U02TrustedStartContext resolver,
            U01U02NonProductionHandoffService.TrustedTraceAuthority traceAuthority,
            U02ClinicalFactApplicationService application) {
        return new U01U02NonProductionHandoffService(resolver, traceAuthority, application);
    }
}
