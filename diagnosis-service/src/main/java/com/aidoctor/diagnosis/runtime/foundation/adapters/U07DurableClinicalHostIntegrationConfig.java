package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.u06.U07ExistingU06WaitAuthority;
import com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery;
import com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitTransitionService;
import com.aidoctor.diagnosis.runtime.u06.wait.U06WaitCoordinator;
import com.aidoctor.diagnosis.state.committer.StateCommitter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Explicit NON-PRODUCTION host composition of existing durable U06/U07 units.
 * Production profiles and default-off flags block all beans in this config.
 * No trusted verifier, durable repositories, medical issuer, or delivery
 * transport is manufactured by the application configuration.
 *
 * Candidate V9 schema must be installed via separately approved deployment;
 * this configuration never runs DDL or initiates patient dispatch.
 */
@Configuration
@Profile("!prod & !production")
@ConditionalOnProperty(prefix = "aidoctor.u07.host-integration",
        name = "nonproduction-enabled", havingValue = "true", matchIfMissing = false)
public class U07DurableClinicalHostIntegrationConfig {

    @Bean
    @ConditionalOnBean({ConsultationWaitTransitionService.class, U06WaitCoordinator.class})
    public U07ExistingU06WaitAuthority u07ExistingWaitAuthority(
            ConsultationWaitTransitionService consultation, U06WaitCoordinator runtime) {
        return new U07ExistingU06WaitAuthority(consultation, runtime);
    }

    @Bean
    @ConditionalOnBean({CDPRepository.class, JdbcTemplate.class,
            TransactionTemplate.class,
            U07TrustedF3RevalidationWriter.IndependentAuthority.class})
    public U07TrustedF3RevalidationWriter u07TrustedRevalidationWriter(
            CDPRepository repository, JdbcTemplate jdbc, TransactionTemplate tx,
            U07TrustedF3RevalidationWriter.IndependentAuthority authority) {
        return new U07TrustedF3RevalidationWriter(repository, jdbc, tx, authority);
    }

    @Bean
    @ConditionalOnBean({CDPRepository.class, JdbcTemplate.class,
            TransactionTemplate.class, StateCommitter.class,
            U07NonProductionDeliveryWaitRecovery.DeliveryVerifier.class,
            U07ExistingU06WaitAuthority.class})
    public U07NonProductionDeliveryWaitRecovery u07DurableDeliveryRecovery(
            CDPRepository repository, JdbcTemplate jdbc, TransactionTemplate tx,
            StateCommitter committer,
            U07NonProductionDeliveryWaitRecovery.DeliveryVerifier verifier,
            U07ExistingU06WaitAuthority waitAuthority) {
        return new U07NonProductionDeliveryWaitRecovery(
                repository, jdbc, tx, committer, verifier, waitAuthority);
    }
}
