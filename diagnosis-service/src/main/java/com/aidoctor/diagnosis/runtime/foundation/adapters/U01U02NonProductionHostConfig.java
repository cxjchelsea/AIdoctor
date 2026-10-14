package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.runtime.u02.*;
import com.aidoctor.diagnosis.runtime.u02.capability.C01U02CapabilityGateway;
import com.aidoctor.diagnosis.runtime.u02.d05.U02DependencyInvalidationHook;
import com.aidoctor.diagnosis.runtime.governance.CapabilityTraceService;
import com.aidoctor.diagnosis.state.committer.StateCommitter;
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
    /**
     * Assemble the existing U02 application only when the host supplies its
     * independently configured mechanical P01 committer and governed C01.
     * No default StateCommitter, policy port, model or medical approval.
     */
    @Bean
    @ConditionalOnBean({StateCommitter.class, C01U02CapabilityGateway.class,
            U02ClinicalFactBusinessOwner.class, U02ClinicalFactProposalFactory.class,
            CapabilityTraceService.class})
    public U02ClinicalFactApplicationService u02ClinicalFactApplication(
            StateCommitter committer,
            C01U02CapabilityGateway gateway,
            U02ClinicalFactBusinessOwner owner,
            U02ClinicalFactProposalFactory proposals,
            CapabilityTraceService trace) {
        return new U02ClinicalFactApplicationService(gateway, owner, proposals,
                new U02ClinicalFactCommitService(committer),
                new U02DependencyInvalidationHook(), trace);
    }

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
