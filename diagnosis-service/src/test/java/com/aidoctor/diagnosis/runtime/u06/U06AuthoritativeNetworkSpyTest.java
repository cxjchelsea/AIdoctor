package com.aidoctor.diagnosis.runtime.u06;

import com.aidoctor.diagnosis.runtime.u06.delivery.U06DeliveryStore;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06SyntheticDeliveryService;
import com.aidoctor.diagnosis.runtime.u06.state.U06SyntheticP01Runtime;
import com.aidoctor.diagnosis.runtime.u06.state.U06SyntheticP01TestFactory;
import org.junit.jupiter.api.Test;

import java.security.Permission;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Runtime external-call interceptor for the bounded PROFILE-B path.
 *
 * This is deliberately test-only and does not alter production networking.
 * The SecurityManager records every socket connect attempt while representative
 * U06 synthetic decision/state/delivery code executes. Any attempt fails the
 * test immediately. The authoritative workflow only sets
 * external_call_spy_enabled=true when this exact test passes.
 */
class U06AuthoritativeNetworkSpyTest {
    private static final String AT="2026-09-24T00:00:00Z";

    @Test
    void profileBSyntheticCoreProducesZeroSocketConnectAttempts() {
        final AtomicInteger connectAttempts=new AtomicInteger();
        SecurityManager previous=System.getSecurityManager();
        SecurityManager spy=new SecurityManager(){
            @Override public void checkPermission(Permission perm) {
                // allow ordinary JVM operations; socket access is intercepted below
            }
            @Override public void checkPermission(Permission perm,Object context) {
                // allow ordinary JVM operations; socket access is intercepted below
            }
            @Override public void checkConnect(String host,int port) {
                connectAttempts.incrementAndGet();
                throw new SecurityException("U06_EXTERNAL_CALL_SPY_BLOCKED_CONNECT:"+host+":"+port);
            }
        };

        System.setSecurityManager(spy);
        try {
            Clock clock=Clock.fixed(Instant.parse(AT), ZoneOffset.UTC);
            U06SyntheticP01Runtime state=U06SyntheticP01TestFactory.create(
                    "synthetic-store-spy","consult-spy","cdp-spy",clock);

            U06ProfileBRequest request=new U06ProfileBRequest(
                    "request-spy","consult-spy","cdp-spy",
                    U06ProfileBRequest.QUESTION_SELECTION_DELIVERY,
                    U06ProfileBRequest.U05_QUESTION_ROUTING,
                    "synthetic-source-spy",0,0,
                    U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,
                    U06ProfileBRequest.SYNTHETIC_VERIFICATION_BINDING,
                    "synthetic-binding-spy",
                    "f3-policy-spy","question-policy-spy","d04-policy-spy",
                    "event-ref-spy","business-event-spy",
                    "thread-spy","run-spy",0L,
                    "corr-spy","trace-spy",AT);

            U06SyntheticDecisionBundle decision=new U06SyntheticDecisionEngine().decide(
                    request,
                    new U06SyntheticDecisionInput(
                            U06SyntheticDecisionInput.SUCCESS,
                            false,false,null,null,false,
                            U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE,
                            Collections.singletonList(
                                    new U06SyntheticDecisionInput.Candidate(
                                            "candidate-spy","question-spy","semantic-spy",
                                            "synthetic-content-ref-spy","content-fingerprint-spy",
                                            1,true))),
                    state.readCurrent());

            assertEquals(U06SyntheticDecisionBundle.SELECTED,decision.getQuestionSelectionStatus());

            U06DeliveryStore inMemory=new U06DeliveryStore(){
                @Override public Snapshot reconcileConfirmed(Command c) {
                    return new Snapshot(c.deliveryEffectId,c.deliveryId,"CONFIRMED",false);
                }
            };
            U06SyntheticDeliveryService service=new U06SyntheticDeliveryService(inMemory);
            U06SyntheticDeliveryService.ScopeAuthorization scope=
                    new U06SyntheticDeliveryService.ScopeAuthorization(
                            "scope-spy","fixture-auth-spy",
                            U06SyntheticDeliveryService.ScopeAuthorization.CURRENT,
                            "consult-spy",U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD,
                            "fixture-spy","fixture-review-spy","synthetic-store-spy",
                            "ci-nonprod-u06","synthetic-endpoint-spy",null,
                            false,false,false);

            U06SyntheticDeliveryService.Confirmation confirmation=service.confirm(
                    "consult-spy",decision.getQuestionSelectionEffectId(),
                    decision.getQuestionId(),decision.getQuestionContentFingerprint(),
                    scope,AT);
            assertNotNull(confirmation.getDeliveryId());

            assertEquals(0,connectAttempts.get(),
                    "bounded PROFILE-B U06 code attempted an external socket connection");
        } finally {
            System.setSecurityManager(previous);
        }
    }
}
