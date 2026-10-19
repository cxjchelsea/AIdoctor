package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.controller.V1NonProductionFirstRoundWaitController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Opt-in adapter wiring only; external host must supply both authorities. */
@Configuration
@Profile("!prod & !production")
@ConditionalOnProperty(prefix="aidoctor.v1.clinical-e2e",
        name="first-round-wait-http-enabled",havingValue="true",matchIfMissing=false)
public class V1NonProductionFirstRoundOwnerHostConfig {
    @Bean
    @ConditionalOnMissingBean(V1NonProductionFirstRoundWaitController.FirstRoundOwner.class)
    @ConditionalOnBean({
        V1NonProductionFirstRoundOwnerAdapter.AuthorizedStateOwner.class,
        V1NonProductionFirstRoundOwnerAdapter.DeliveryOwnerAuthority.class,
        V1NonProductionFirstRoundOwnerAdapter.AuthorizedQuestionContent.class,
        V1NonProductionDeliveredQuestionReadback.class
    })
    public V1NonProductionFirstRoundWaitController.FirstRoundOwner v1FirstRoundOwner(
            V1NonProductionFirstRoundOwnerAdapter.AuthorizedStateOwner state,
            V1NonProductionFirstRoundOwnerAdapter.DeliveryOwnerAuthority delivery,
            V1NonProductionDeliveredQuestionReadback readback,
            V1NonProductionFirstRoundOwnerAdapter.AuthorizedQuestionContent content) {
        return new V1NonProductionFirstRoundOwnerAdapter(state, delivery, readback, content);
    }
}
