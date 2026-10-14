package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.runtime.u04.*;
import com.aidoctor.diagnosis.runtime.u05.*;
import com.aidoctor.diagnosis.state.committer.StateCommitter;
import com.aidoctor.diagnosis.repository.CDPRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.context.annotation.Profile;
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
     * Read-only durable F3 owner/barrier/revalidation intake. Never becomes a
     * default source: the host must opt in with a separately verified
     * non-production database and non-production A1 issuance.
     */
    @Bean
    @Profile("!prod & !production")
    @ConditionalOnProperty(prefix = "aidoctor.u07.clinical-host",
            name = "persisted-f3-evidence-nonproduction-enabled",
            havingValue = "true", matchIfMissing = false)
    @ConditionalOnBean(JdbcTemplate.class)
    public U07DurableNonProductionA1Issuer.VerifiedPostF3EvidencePort
            u07PersistedF3Evidence(JdbcTemplate jdbc) {
        return new U07PersistedF3OwnerBarrierRevalidationEvidence(jdbc);
    }

    /**
     * Non-production-only durable record source. An independent, owner-approved
     * evidence verifier is REQUIRED; no permissive default is provided.
     * The separate table is not a promoted production schema.
     */
    @Bean
    @Profile("!prod & !production")
    @ConditionalOnProperty(prefix = "aidoctor.u07.clinical-host",
            name = "durable-a1-nonproduction-enabled", havingValue = "true",
            matchIfMissing = false)
    @ConditionalOnBean({
        CDPRepository.class, JdbcTemplate.class, TransactionTemplate.class,
        U07DurableNonProductionA1Issuer.VerifiedPostF3EvidencePort.class
    })
    public U07DurableNonProductionA1Issuer u07DurableNonProductionA1Issuer(
            CDPRepository repository, JdbcTemplate jdbc, TransactionTemplate transactions,
            U07DurableNonProductionA1Issuer.VerifiedPostF3EvidencePort verifier) {
        return new U07DurableNonProductionA1Issuer(repository, jdbc, transactions, verifier);
    }

    /**
     * Explicitly opt-in only with an independently authenticated A1 source.
     * No default source, no synthetic issuer and no production traffic switch.
     */
    @Bean
    @ConditionalOnProperty(prefix = "aidoctor.u07.clinical-host",
            name = "attested-a1-enabled", havingValue = "true", matchIfMissing = false)
    @ConditionalOnBean({
        CDPRepository.class,
        ClinicalCdpF3BoundA1AuthorityPort.IndependentlyAttestedA1Source.class
    })
    public U04A1U05AuthorityBridge.A1BarrierAuthorityPort u07HostAttestedA1(
            CDPRepository repository,
            ClinicalCdpF3BoundA1AuthorityPort.IndependentlyAttestedA1Source source) {
        return new ClinicalCdpF3BoundA1AuthorityPort(repository, source);
    }

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
