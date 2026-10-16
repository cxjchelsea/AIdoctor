package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.runtime.governance.CapabilityTraceService;
import com.aidoctor.diagnosis.runtime.u02.*;
import com.aidoctor.diagnosis.runtime.u02.capability.C01U02CapabilityGateway;
import com.aidoctor.diagnosis.runtime.u02.d05.U02DependencyInvalidationHook;
import com.aidoctor.diagnosis.state.committer.StateCommitter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Allows a nonproduction HTTP V1 host to assemble the canonical U02 chain
 * without requiring the older, independent u01-u02-handoff-enabled toggle.
 * Every governed owner must be separately provided. Never fabricates C01,
 * P01 commit authority, patient trace evidence, or clinical decisions.
 */
@Configuration
@Profile("!prod & !production")
@ConditionalOnExpression("${aidoctor.v1.clinical-e2e.u01-u04-http-enabled:false} or " +
        "${aidoctor.v1.clinical-e2e.first-round-wait-http-enabled:false} or " +
        "${aidoctor.v1.clinical-e2e.u01-u02-http-enabled:false}")
public class V1NonProductionU02HttpHostConfig {
    @Bean
    @ConditionalOnMissingBean(U02ClinicalFactApplicationService.class)
    @ConditionalOnBean({StateCommitter.class, C01U02CapabilityGateway.class,
            U02ClinicalFactBusinessOwner.class, U02ClinicalFactProposalFactory.class,
            CapabilityTraceService.class})
    public U02ClinicalFactApplicationService v1HttpClinicalFactApplication(
            StateCommitter committer, C01U02CapabilityGateway gateway,
            U02ClinicalFactBusinessOwner owner, U02ClinicalFactProposalFactory proposals,
            CapabilityTraceService trace) {
        return new U02ClinicalFactApplicationService(gateway, owner, proposals,
                new U02ClinicalFactCommitService(committer),
                new U02DependencyInvalidationHook(), trace);
    }

    @Bean
    @ConditionalOnMissingBean(U01U02NonProductionHandoffService.class)
    @ConditionalOnBean({U01U02TrustedStartContext.class,
            U01U02NonProductionHandoffService.TrustedTraceAuthority.class,
            U02ClinicalFactApplicationService.class, TransactionTemplate.class})
    public U01U02NonProductionHandoffService v1HttpTrustedFactHandoff(
            U01U02TrustedStartContext resolver,
            U01U02NonProductionHandoffService.TrustedTraceAuthority traceAuthority,
            U02ClinicalFactApplicationService application,
            TransactionTemplate transactions) {
        return new U01U02NonProductionHandoffService(resolver, traceAuthority, application, transactions);
    }
}
