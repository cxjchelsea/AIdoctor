package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.foundation.ClinicalRunRepository;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeBindingService;
import com.aidoctor.diagnosis.runtime.u06.U05QuestionRouteAdmissionAdapter;
import com.aidoctor.diagnosis.runtime.u06.U06NonProductionPhysicalMode23Service;
import com.aidoctor.diagnosis.runtime.u06.U07NonProductionDeliveryWaitRecovery;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * No default activation. Requires independently wired clinical A1/F3, U05,
 * physical U06, authoritative runtime identities, verified delivery and
 * durable WAIT owners; this configuration installs none of those authorities.
 */
@Configuration
@Profile("!prod & !production")
@ConditionalOnProperty(prefix="aidoctor.v1.clinical-e2e",
        name="physical-followup-enabled", havingValue="true", matchIfMissing=false)
public class U07NonProductionPhysicalFollowupHostConfig {
    @Bean
    @ConditionalOnBean({
            U07NonProductionF3A1U05Flow.class,
            U05QuestionRouteAdmissionAdapter.class,
            U06NonProductionPhysicalMode23Service.class,
            U07NonProductionDeliveryWaitRecovery.class,
            CDPRepository.class, RuntimeBindingService.class, ClinicalRunRepository.class,
            TransactionTemplate.class,
            U07NonProductionPhysicalFollowupCoordinator.AuthorizedQuestionHost.class
    })
    public U07NonProductionPhysicalFollowupCoordinator physicalFollowupCoordinator(
            U07NonProductionF3A1U05Flow f3A1U05,
            U05QuestionRouteAdmissionAdapter routedAdmission,
            U06NonProductionPhysicalMode23Service physicalSelection,
            U07NonProductionDeliveryWaitRecovery deliveryWait,
            CDPRepository cdps, RuntimeBindingService binding,
            ClinicalRunRepository runs, TransactionTemplate selectionTransactions,
            U07NonProductionPhysicalFollowupCoordinator.AuthorizedQuestionHost host) {
        return new U07NonProductionPhysicalFollowupCoordinator(
                f3A1U05, routedAdmission, physicalSelection, deliveryWait,
                cdps, binding, runs, selectionTransactions, host);
    }
}
