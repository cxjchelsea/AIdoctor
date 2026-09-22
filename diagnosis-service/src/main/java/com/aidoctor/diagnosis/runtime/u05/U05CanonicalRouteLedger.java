package com.aidoctor.diagnosis.runtime.u05;

import com.aidoctor.diagnosis.runtime.effects.CanonicalEffectLedger;
import com.aidoctor.diagnosis.runtime.effects.CanonicalEffectLedgerDecision;

/**
 * Durable U05 routing/eligibility reconciliation over the generic effect ledger.
 *
 * <p>Routing currentness and permission are evaluated before this adapter is
 * called. The adapter does not authorize or execute downstream work.</p>
 */
public final class U05CanonicalRouteLedger implements U05RouteLedger {
    public static final String ROUTING_NAMESPACE = "U05_ROUTING_DECISION";
    public static final String ELIGIBILITY_NAMESPACE = "U05_ROUTE_ELIGIBILITY";

    private final CanonicalEffectLedger ledger;

    public U05CanonicalRouteLedger(CanonicalEffectLedger ledger) {
        if (ledger == null) throw new IllegalArgumentException("ledger is required");
        this.ledger = ledger;
    }

    @Override
    public Entry reconcile(
            String routingDecisionId,
            String fingerprint,
            U05DownstreamRoutingDecision candidate) {
        CanonicalEffectLedgerDecision routeDecision = ledger.createIfAbsent(
                ROUTING_NAMESPACE,
                routingDecisionId,
                fingerprint,
                U05CanonicalEffectPayloadCodec.ROUTING_SCHEMA,
                U05CanonicalEffectPayloadCodec.routing(candidate));

        requireSuccessful("U05_ROUTING_LEDGER", routeDecision);

        U05DownstreamEligibility eligibility = candidate.getEligibility();
        if (eligibility != null) {
            String eligibilityFingerprint =
                    U05CanonicalEffectPayloadCodec.eligibilityFingerprint(eligibility);
            CanonicalEffectLedgerDecision eligibilityDecision = ledger.createIfAbsent(
                    ELIGIBILITY_NAMESPACE,
                    eligibility.getEligibilityId(),
                    eligibilityFingerprint,
                    U05CanonicalEffectPayloadCodec.ELIGIBILITY_SCHEMA,
                    U05CanonicalEffectPayloadCodec.eligibility(eligibility));
            requireSuccessful("U05_ELIGIBILITY_LEDGER", eligibilityDecision);
        }

        boolean reattached =
                CanonicalEffectLedgerDecision.Status.REATTACHED.equals(routeDecision.getStatus());
        return new Entry(fingerprint, candidate, reattached);
    }

    private static void requireSuccessful(
            String prefix,
            CanonicalEffectLedgerDecision decision) {
        if (decision != null && decision.isSuccessfulRecord()) return;
        if (decision == null) {
            throw new IllegalStateException(prefix + "_NULL_DECISION");
        }
        throw new IllegalStateException(
                prefix + "_" + decision.getStatus()
                        + (decision.getReasonCode() == null ? "" : "_" + decision.getReasonCode()));
    }
}
