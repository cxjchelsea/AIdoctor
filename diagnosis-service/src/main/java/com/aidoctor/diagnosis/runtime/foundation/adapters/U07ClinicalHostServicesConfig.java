package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.runtime.u04.*;
import com.aidoctor.diagnosis.runtime.u05.*;
import com.aidoctor.diagnosis.state.committer.StateCommitter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Explicit opt-in assembly of EXISTING non-production clinical services.
 *
 * <p>StateCommitter and U05 evidence/permission/ledger ports must be supplied
 * separately by an independently authorized host. No synthetic policy, ledger,
 * F3/A1 issuer, or outbound sender is installed by this configuration.
 * Both services retain their own non-production admission restrictions.</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "aidoctor.u07.clinical-host",
        name = "nonproduction-enabled", havingValue = "true", matchIfMissing = false)
public class U07ClinicalHostServicesConfig {
    /**
     * An independent host-owned A1 attestation MUST be provided; the CDP gate
     * alone cannot issue F3/A1 routing permission. No default/fake A1 bean.
     */
    @Bean
    @ConditionalOnBean({
        U04A1U05AuthorityBridge.GateReadbackPort.class,
        U04A1U05AuthorityBridge.A1BarrierAuthorityPort.class
    })
    public U04A1U05AuthorityBridge u07HostAuthorityBridge(
            U04A1U05AuthorityBridge.GateReadbackPort gate,
            U04A1U05AuthorityBridge.A1BarrierAuthorityPort a1) {
        return new U04A1U05AuthorityBridge(gate, a1);
    }

    @Bean
    @ConditionalOnBean(StateCommitter.class)
    public U04NonProductionApplicationService u07HostU04(StateCommitter committer) {
        return new U04NonProductionApplicationService(
                new U04AdmissionService(),
                new U04SafetyGatePolicy(),
                new U04StateProposalFactory(),
                new U04CommitService(committer));
    }

    @Bean
    @ConditionalOnBean({
        StateCommitter.class,
        U05ClinicalReadinessSnapshotPort.class,
        U05RoutingCurrentnessPort.class,
        U05DownstreamPermissionPort.class,
        U05AdmissionLedger.class,
        U05RouteLedger.class
    })
    public U05NonProductionApplicationService u07HostU05(
            StateCommitter committer,
            U05ClinicalReadinessSnapshotPort readback,
            U05RoutingCurrentnessPort currentness,
            U05DownstreamPermissionPort permission,
            U05AdmissionLedger admissionLedger,
            U05RouteLedger routeLedger) {
        return new U05NonProductionApplicationService(
                new U05AdmissionService(admissionLedger),
                new U05ClinicalReadinessPolicy(),
                new U05ReadinessStateProposalFactory(),
                new U05CommitService(committer, readback),
                currentness,
                new U05RoutingService(routeLedger, permission));
    }
}
