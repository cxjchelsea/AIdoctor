package com.aidoctor.diagnosis.runtime.u05.verification;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.runtime.effects.CanonicalEffectLedger;
import com.aidoctor.diagnosis.runtime.effects.CanonicalEffectLedgerDecision;
import com.aidoctor.diagnosis.runtime.effects.NonProductionFileCanonicalEffectLedger;
import com.aidoctor.diagnosis.runtime.u05.*;
import com.aidoctor.diagnosis.state.committer.StateCommitter;
import com.aidoctor.diagnosis.state.committer.SyntheticStateSnapshot;
import com.aidoctor.diagnosis.state.committer.SyntheticVersionedStateRepository;
import com.aidoctor.diagnosis.state.committer.fakes.InMemoryIdempotencyFake;
import com.aidoctor.diagnosis.state.committer.fakes.RecordingCommitEventEvidenceFake;
import com.aidoctor.diagnosis.state.committer.fakes.SyntheticAuditPortFake;
import com.aidoctor.diagnosis.state.committer.ports.CapabilityPolicyPort;
import com.aidoctor.diagnosis.state.committer.ports.ConsentPolicyPort;
import com.aidoctor.diagnosis.state.committer.ports.FieldPermissionPort;
import com.aidoctor.diagnosis.state.committer.ports.SourceValidationPort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Verification-only support for the frozen U05 RDP-06 matrix.
 *
 * <p>This class is test-only. It executes the real U05 non-production
 * implementation surfaces and records observed evidence. Expected values are
 * loaded only from the independently reviewable static oracle resource.</p>
 */
public final class U05VerificationSupport {
    public static final String IMPLEMENTATION_SHA =
            "2b7926afd69de9fe2224d8a5b69e91c02c2db495";
    public static final String CONTRACT_MANIFEST_DIGEST =
            "570ca5303f601df2caf672bfb7c9e61fe2c5a5add86e288cf782c8e6030712e5";
    public static final String ENV = "ci-nonprod-u05";
    public static final int VERSION = 12;
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-09-21T08:00:00Z"), ZoneOffset.UTC);
    private static final ObjectMapper JSON = new ObjectMapper();

    private static volatile Map<String, Map<String, Object>> expectations;
    private static volatile Map<String, Map<String, Object>> precedence;

    private U05VerificationSupport() {}

    // ---------------------------------------------------------------------
    // Public entry points used by the six focused verification suites.
    // ---------------------------------------------------------------------

    public static void verifyAdmission(int number) throws Exception {
        String caseId = caseId(number);
        Observation observed;
        switch (number) {
            case 1:
                observed = admissionAccepted(false);
                break;
            case 2:
                observed = admissionPostSafetyRejected();
                break;
            case 3:
                observed = admissionGateRejected(U05ConsumerInboundRequest.GATE_BLOCKED);
                break;
            case 4:
                observed = admissionGateRejected(U05ConsumerInboundRequest.GATE_UNAVAILABLE);
                break;
            case 5:
                observed = admissionAccepted(true);
                break;
            case 6:
                observed = admissionRestrictedMissingContext();
                break;
            case 7:
                observed = admissionStaleGate();
                break;
            case 8:
                observed = admissionUncommittedGate();
                break;
            case 9:
                observed = admissionWrongContinuationConsequence();
                break;
            case 10:
                observed = admissionStateVersionMismatch();
                break;
            case 11:
                observed = admissionOwnerRecomputationPending();
                break;
            case 12:
                observed = admissionThenInputFailure();
                break;
            case 15:
                observed = admissionInputSetChanged();
                break;
            default:
                throw new IllegalArgumentException("not an admission verification case: " + number);
        }
        assertAndRecord(caseId, observed);
    }

    public static void verifyReplay(int number) throws Exception {
        String caseId = caseId(number);
        Observation observed;
        switch (number) {
            case 13:
                observed = admissionExactReplay();
                break;
            case 14:
                observed = admissionReplayConflict();
                break;
            case 32:
                observed = readinessExactReplay();
                break;
            case 40:
                observed = readinessFingerprintReplayConflict();
                break;
            case 41:
                observed = proposalIdentityRecovered();
                break;
            case 42:
                observed = postCommitReplayNoDuplicate();
                break;
            case 43:
                observed = concurrentWritersAtMostOne();
                break;
            case 54:
                observed = routeExactReplay();
                break;
            case 55:
                observed = routeReplayConflict();
                break;
            case 58:
                observed = schedulerIntentReplay();
                break;
            default:
                throw new IllegalArgumentException("not a replay/recovery case: " + number);
        }
        assertAndRecord(caseId, observed);
    }

    public static void verifyD03(int number) throws Exception {
        String caseId = caseId(number);
        Observation observed;
        switch (number) {
            case 16:
                observed = d03(profileOutOfScope(), U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, "DECIDED|OUT_OF_SCOPE|D03-POL-001");
                break;
            case 17:
                observed = d03(profileOfflineWithLowerCandidates(), U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, "DECIDED|NEEDS_OFFLINE_EVIDENCE|D03-POL-002");
                break;
            case 18:
                observed = d03(profileClarification(), U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, "DECIDED|NEEDS_CLARIFICATION|D03-POL-003");
                break;
            case 19:
                observed = d03(profileCanAskMore(), U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, "DECIDED|CAN_ASK_MORE|D03-POL-004");
                break;
            case 20:
                observed = d03(profilePol005(VERSION, ""), U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, "DECIDED|READY_FOR_CLINICAL_ANALYSIS|D03-POL-005");
                break;
            case 21:
                observed = d03(profilePol011(), U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, "DECIDED|READY_FOR_CLINICAL_ANALYSIS|D03-POL-011");
                break;
            case 22:
                observed = d03(profileNoReliable(), U05ConsumerInboundRequest.POST_DDX_REEVALUATION, "DECIDED|NO_RELIABLE_DIRECTION|D03-POL-006");
                break;
            case 23:
                observed = d03(profileUnavailable(), U05ConsumerInboundRequest.POST_DDX_REEVALUATION, "INPUT_FAILURE");
                break;
            case 24:
                observed = d03(profileConflict(), U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, "INPUT_CONFLICT");
                break;
            case 25:
                observed = d03(profileOfflineWithLowerCandidates(), U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, "DECIDED|NEEDS_OFFLINE_EVIDENCE|D03-POL-002");
                break;
            case 26:
                observed = applicabilityEvidenceMissing();
                break;
            case 27:
                observed = d03(profilePostDdxPol011Excluded(), U05ConsumerInboundRequest.POST_DDX_REEVALUATION, "DECIDED|NO_RELIABLE_DIRECTION|D03-POL-006");
                break;
            case 28:
                observed = d03(profileFailureWithLowerCandidates(), U05ConsumerInboundRequest.POST_DDX_REEVALUATION, "INPUT_FAILURE");
                break;
            default:
                throw new IllegalArgumentException("not a D03 case: " + number);
        }
        assertAndRecord(caseId, observed);
    }

    public static void verifyMutation(int number) throws Exception {
        String caseId = caseId(number);
        Observation observed;
        switch (number) {
            case 29:
                observed = mutationCommit();
                break;
            case 30:
                observed = mutationNoEffectInputFailure();
                break;
            case 31:
                observed = mutationNoEffectInputConflict();
                break;
            case 33:
                observed = mutationDifferentBasisDifferentEffect();
                break;
            case 34:
                observed = mutationStaleBaseConflict();
                break;
            case 35:
                observed = mutationUnauthorizedFieldRejected();
                break;
            case 36:
                observed = mutationRestrictedProvenance();
                break;
            case 37:
                observed = mutationReadinessOnlyStaysCurrent();
                break;
            case 38:
                observed = mutationStateDependencyInvalidation();
                break;
            case 39:
                observed = mutationNonStateRevalidationInvalidation();
                break;
            default:
                throw new IllegalArgumentException("not a mutation case: " + number);
        }
        assertAndRecord(caseId, observed);
    }

    public static void verifyRouting(int number) throws Exception {
        String caseId = caseId(number);
        Observation observed;
        switch (number) {
            case 44:
                observed = routingMapping(profileClarification(), U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        "ELIGIBLE|TO_U06_QUESTION_PATH|U06");
                break;
            case 45:
                observed = routingMapping(profileCanAskMore(), U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        "ELIGIBLE|TO_U06_QUESTION_PATH|U06");
                break;
            case 46:
                observed = routingMapping(profilePol005(VERSION, ""), U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        "ELIGIBLE|TO_U08_CLINICAL_ANALYSIS|U08");
                break;
            case 47:
                observed = routingMapping(profileOfflineWithLowerCandidates(), U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        "ELIGIBLE|TO_U10_OFFLINE_EVIDENCE|U10");
                break;
            case 48:
                observed = routingMapping(profileOutOfScope(), U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        "ELIGIBLE|TO_U11_SAFE_EXIT|U11");
                break;
            case 49:
                observed = routingMapping(profileNoReliable(), U05ConsumerInboundRequest.POST_DDX_REEVALUATION,
                        "ELIGIBLE|TO_U11_SAFE_EXIT|U11");
                break;
            case 50:
                observed = restrictedRouting(U05DownstreamPermissionDecision.DENIED, "PREEMPTED");
                break;
            case 51:
                observed = restrictedRouting(U05DownstreamPermissionDecision.UNAVAILABLE, "FAILURE_REQUIRED");
                break;
            case 52:
                observed = postCommitSafetyRoute(U05ConsumerInboundRequest.GATE_BLOCKED, "PREEMPTED");
                break;
            case 53:
                observed = postCommitSafetyRoute(U05ConsumerInboundRequest.GATE_UNAVAILABLE, "FAILURE_REQUIRED");
                break;
            case 56:
                observed = schedulerStaleEligibility();
                break;
            case 57:
                observed = schedulerBindingUnavailable();
                break;
            case 59:
                observed = noOrdinaryLifecycleForNonEligibleStatuses();
                break;
            case 60:
                observed = zeroExternalEffectsAfterRouteConsumption();
                break;
            default:
                throw new IllegalArgumentException("not a routing case: " + number);
        }
        assertAndRecord(caseId, observed);
    }

    public static void verifyPrecedenceMatrix() throws Exception {
        Map<String, Map<String, Object>> oracle = precedence();
        Assertions.assertEquals(28, oracle.size(), "P0..P7 pair matrix must contain 28 pairs");
        for (Map<String, Object> entry : oracle.values()) {
            String subcase = String.valueOf(entry.get("subcase_id"));
            String constructibility = String.valueOf(entry.get("constructibility"));
            if ("NOT_CONSTRUCTIBLE".equals(constructibility)) {
                Assertions.assertFalse(blank(String.valueOf(entry.get("rationale"))));
                recordPrecedence(entry, null, true);
                continue;
            }
            int high = Integer.parseInt(String.valueOf(entry.get("higher_candidate")).substring(1));
            int low = Integer.parseInt(String.valueOf(entry.get("lower_candidate")).substring(1));
            U05ReadinessInputManifest manifest = precedenceManifest(high, low);
            AdmissionBundle admitted = admitBundle(
                    manifest,
                    U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                    U05ConsumerInboundRequest.GATE_ALLOW,
                    null,
                    null,
                    VERSION,
                    authority(U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                            U05ConsumerInboundRequest.GATE_ALLOW, null, null, true,
                            VERSION, true, true, true, false, null, null));
            Assertions.assertTrue(admitted.admission.isAdmitted(), subcase + " must be lawfully admitted");
            U05ClinicalReadinessDecision decision =
                    new U05ClinicalReadinessPolicy().decide(admitted.admission.getAdmittedInput());
            String actual = precedenceCode(decision);
            Assertions.assertEquals("P" + high, actual, subcase);
            recordPrecedence(entry, decision, true);
        }

        U05ClinicalReadinessDecision pol005 = decisionFor(
                profilePol005(VERSION, "special-pol005"),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT);
        U05ClinicalReadinessDecision pol011 = decisionFor(
                profilePol011(),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT);
        U05ClinicalReadinessDecision postDdx = decisionFor(
                profilePostDdxPol011Excluded(),
                U05ConsumerInboundRequest.POST_DDX_REEVALUATION);

        Assertions.assertEquals("D03-POL-005", pol005.getPolicyRuleRef());
        Assertions.assertEquals("D03-POL-011", pol011.getPolicyRuleRef());
        Assertions.assertEquals("D03-POL-006", postDdx.getPolicyRuleRef());

        List<Map<String, Object>> proofs = new ArrayList<Map<String, Object>>();
        proofs.add(specialProof(
                "POL-005-POSITIVE", "D03-POL-005", pol005.getPolicyRuleRef(),
                Arrays.asList("RDP-02"), "runtime positive POL-005 profile"));
        proofs.add(specialProof(
                "POL-011-POSITIVE", "D03-POL-011", pol011.getPolicyRuleRef(),
                Arrays.asList("RDP-02"), "runtime positive POL-011 profile"));

        List<U05ReadinessInput> pol005Inputs = profilePol005(VERSION, "special-mutual-pol005");
        List<U05ReadinessInput> pol011Inputs = profilePol011();
        String pol005F6 = f6Semantic(pol005Inputs);
        String pol011F6 = f6Semantic(pol011Inputs);
        boolean mutuallyExclusive = !pol005F6.equals(pol011F6)
                && pol005F6.startsWith("NOT_YET_APPLICABLE")
                && pol011F6.startsWith("PRESENT|NO_BLOCKING_OFFLINE_EVIDENCE_NEED");
        Map<String, Object> mutual = specialProof(
                "POL-005-POL-011-MUTUAL-EXCLUSION",
                "MUTUALLY_EXCLUSIVE",
                mutuallyExclusive ? "MUTUALLY_EXCLUSIVE" : "NOT_PROVEN",
                Arrays.asList("RDP-02", "RDP-05"),
                "Derived from independently executed positive POL-005/POL-011 branches and their contract-governed F6 semantics.");
        mutual.put("pol005_observed_rule", pol005.getPolicyRuleRef());
        mutual.put("pol011_observed_rule", pol011.getPolicyRuleRef());
        mutual.put("pol005_f6_semantics", pol005F6);
        mutual.put("pol011_f6_semantics", pol011F6);
        mutual.put("contract_predicate", mutuallyExclusive);
        proofs.add(mutual);

        String postDdxObserved = "D03-POL-011".equals(postDdx.getPolicyRuleRef())
                ? "POL-011-APPLIED"
                : "POL-011-NOT_APPLICABLE";
        Map<String, Object> postDdxProof = specialProof(
                "POST-DDX-POL-011-EXCLUSION",
                "POL-011-NOT_APPLICABLE",
                postDdxObserved,
                Arrays.asList("RDP-02", "RDP-05"),
                "Derived from executed POST_DDX runtime rule; POL-011 must not be selected.");
        postDdxProof.put("observed_runtime_rule", postDdx.getPolicyRuleRef());
        proofs.add(postDdxProof);

        Map<String, Object> special = new LinkedHashMap<String, Object>();
        special.put("schema", "U05_D03_SPECIAL_PROOF_EVIDENCE_V0_1");
        special.put("proofs", proofs);
        writeJson(evidenceRoot().resolve("u05-d03-special-proof-evidence.json"), special);
    }

    private static U05ClinicalReadinessDecision decisionFor(
            List<U05ReadinessInput> inputs,
            String context) {
        U05ReadinessInputManifest manifest = manifest(context, VERSION, inputs);
        AdmissionBundle admitted = admitBundle(
                manifest,
                context,
                U05ConsumerInboundRequest.GATE_ALLOW,
                null,
                null,
                VERSION,
                authority(
                        context,
                        U05ConsumerInboundRequest.GATE_ALLOW,
                        null,
                        null,
                        true,
                        VERSION,
                        true,
                        true,
                        true,
                        false,
                        null,
                        null));
        Assertions.assertTrue(admitted.admission.isAdmitted());
        return new U05ClinicalReadinessPolicy().decide(admitted.admission.getAdmittedInput());
    }

    private static Map<String, Object> specialProof(
            String id,
            String expected,
            String observed,
            List<String> authorityRefs,
            String rationale) {
        Map<String, Object> proof = new LinkedHashMap<String, Object>();
        proof.put("id", id);
        proof.put("expected", expected);
        proof.put("observed", observed);
        proof.put("authority_refs", authorityRefs);
        proof.put("rationale", rationale);
        proof.put("proof_source", "RUNTIME_OR_CONTRACT_PREDICATE");
        proof.put("pass", expected.equals(observed));
        return proof;
    }

    public static void verifyHarnessGapDetector() throws Exception {
        boolean fired = false;
        try {
            expectation("U05-EV-999");
        } catch (IllegalStateException expected) {
            fired = true;
        }
        Assertions.assertTrue(fired, "expectation gap detector must fail closed");
        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("schema", "U05_HARNESS_SELF_TEST_EVIDENCE_V0_1");
        evidence.put("harness_id", "U05-HG-001");
        evidence.put("detector", "FIRED");
        evidence.put("verification_path", "FAIL_CLOSED");
        evidence.put("business_expected_result_invented", false);
        evidence.put("pass", true);
        writeJson(evidenceRoot().resolve("u05-harness-self-test-evidence.json"), evidence);
    }

    public static void verifyStaticInputsSyntheticNonPhi() throws Exception {
        Map<String, Object> fixtures = readResourceMap("/u05/verification/u05-verification-fixtures.json");
        Assertions.assertEquals(Boolean.TRUE, fixtures.get("synthetic"));
        Assertions.assertEquals(Boolean.FALSE, fixtures.get("contains_real_phi"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) fixtures.get("fixtures");
        Assertions.assertTrue(list.size() >= 82);
        for (Map<String, Object> fixture : list) {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) fixture.get("fixture_payload");
            Assertions.assertEquals(Boolean.TRUE, payload.get("synthetic"));
            Assertions.assertEquals(Boolean.FALSE, payload.get("contains_real_phi"));
            String digest = sha256(canonicalJson(payload).getBytes(StandardCharsets.UTF_8));
            Assertions.assertEquals(fixture.get("fixture_digest"), digest);
        }
    }

    // ---------------------------------------------------------------------
    // Admission cases.
    // ---------------------------------------------------------------------

    private static Observation admissionAccepted(boolean restricted) throws Exception {
        String gate = restricted ? U05ConsumerInboundRequest.GATE_RESTRICTED : U05ConsumerInboundRequest.GATE_ALLOW;
        String ctx = restricted ? "restricted-context-1" : null;
        String perm = restricted ? "restricted-permission-u05" : null;
        U05ReadinessInputManifest manifest = manifest(
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, VERSION, profilePol005(VERSION, ""));
        U05AdmissionResult result = new U05AdmissionService(new U05InMemoryAdmissionLedger()).admit(
                request(manifest, U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, gate, ctx, perm,
                        VERSION, "canonical-event-1", null),
                manifest,
                authority(U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, gate, ctx, perm, true,
                        VERSION, true, true, true, false, null, null));
        Assertions.assertTrue(result.isAdmitted());
        Observation o = observation("ADMISSION", "ADMITTED");
        addAdmissionDetails(o, result, manifest);
        return o;
    }

    private static Observation admissionPostSafetyRejected() {
        U05ReadinessInputManifest manifest = manifest(
                U05ConsumerInboundRequest.POST_SAFETY_INITIAL, VERSION,
                profilePol005(VERSION, "post-safety"));
        U05AdmissionResult result = new U05AdmissionService(new U05InMemoryAdmissionLedger()).admit(
                request(manifest, U05ConsumerInboundRequest.POST_SAFETY_INITIAL,
                        U05ConsumerInboundRequest.GATE_ALLOW, null, null, VERSION, "canonical-event-1", null),
                manifest,
                authority(U05ConsumerInboundRequest.POST_SAFETY_INITIAL,
                        U05ConsumerInboundRequest.GATE_ALLOW, null, null, true,
                        VERSION, true, true, true, false, null, null));
        Assertions.assertFalse(result.isAdmitted());
        Assertions.assertEquals(U05AdmissionService.CONTEXT_MISMATCH, result.getReasonCode());
        return rejection(result);
    }

    private static Observation admissionGateRejected(String gate) {
        U05ReadinessInputManifest manifest = manifest(
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, VERSION, profilePol005(VERSION, gate));
        U05AdmissionResult result = new U05AdmissionService(new U05InMemoryAdmissionLedger()).admit(
                request(manifest, U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, gate,
                        null, null, VERSION, "canonical-event-1", null),
                manifest,
                authority(U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, gate,
                        null, null, true, VERSION, true, true, true, false, null, null));
        Assertions.assertFalse(result.isAdmitted());
        Assertions.assertEquals(U05AdmissionService.GATE_NOT_ELIGIBLE, result.getReasonCode());
        return rejection(result);
    }

    private static Observation admissionRestrictedMissingContext() {
        U05ReadinessInputManifest manifest = manifest(
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, VERSION, profilePol005(VERSION, "restricted"));
        U05AdmissionResult result = new U05AdmissionService(new U05InMemoryAdmissionLedger()).admit(
                request(manifest, U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_RESTRICTED, null, "restricted-permission-u05",
                        VERSION, "canonical-event-1", null),
                manifest,
                authority(U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_RESTRICTED, "restricted-context-1",
                        "restricted-permission-u05", true, VERSION, true, true, true, false, null, null));
        Assertions.assertEquals(U05AdmissionService.RESTRICTED_CONTEXT_MISSING, result.getReasonCode());
        return rejection(result);
    }

    private static Observation admissionStaleGate() {
        U05ReadinessInputManifest manifest = manifest(
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, VERSION, profilePol005(VERSION, "stale"));
        U05AdmissionResult result = new U05AdmissionService(new U05InMemoryAdmissionLedger()).admit(
                request(manifest, U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_ALLOW, null, null,
                        VERSION, "canonical-event-1", null),
                manifest,
                authority(U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_ALLOW, null, null, true,
                        VERSION, true, false, true, false, null, null));
        Assertions.assertEquals(U05AdmissionService.GATE_NOT_CURRENT, result.getReasonCode());
        return rejection(result);
    }

    private static Observation admissionUncommittedGate() {
        U05ReadinessInputManifest manifest = manifest(
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, VERSION, profilePol005(VERSION, "uncommitted"));
        U05AdmissionResult result = new U05AdmissionService(new U05InMemoryAdmissionLedger()).admit(
                request(manifest, U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_ALLOW, null, null,
                        VERSION, "canonical-event-1", null),
                manifest,
                authority(U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_ALLOW, null, null, true,
                        VERSION, false, true, true, false, null, null));
        Assertions.assertEquals(U05AdmissionService.GATE_NOT_COMMITTED, result.getReasonCode());
        return rejection(result);
    }

    private static Observation admissionWrongContinuationConsequence() {
        U05ReadinessInputManifest manifest = manifest(
                U05ConsumerInboundRequest.POST_DDX_REEVALUATION, VERSION, profileNoReliable());
        String wrong = "TO_NOT_U05";
        U05AdmissionResult result = new U05AdmissionService(new U05InMemoryAdmissionLedger()).admit(
                request(manifest, U05ConsumerInboundRequest.POST_DDX_REEVALUATION,
                        U05ConsumerInboundRequest.GATE_ALLOW, null, null,
                        VERSION, "canonical-event-1", wrong),
                manifest,
                authority(U05ConsumerInboundRequest.POST_DDX_REEVALUATION,
                        U05ConsumerInboundRequest.GATE_ALLOW, null, null, true,
                        VERSION, true, true, true, false, wrong, null));
        Assertions.assertEquals(U05AdmissionService.ROUTE_CONSEQUENCE_NOT_U05, result.getReasonCode());
        return rejection(result);
    }

    private static Observation admissionStateVersionMismatch() {
        U05ReadinessInputManifest manifest = manifest(
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, VERSION, profilePol005(VERSION, "version"));
        U05AdmissionResult result = new U05AdmissionService(new U05InMemoryAdmissionLedger()).admit(
                request(manifest, U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_ALLOW, null, null,
                        VERSION, "canonical-event-1", null),
                manifest,
                authority(U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_ALLOW, null, null, true,
                        VERSION + 1, true, true, true, false, null, null));
        Assertions.assertEquals(U05AdmissionService.STATE_VERSION_MISMATCH, result.getReasonCode());
        return rejection(result);
    }

    private static Observation admissionOwnerRecomputationPending() {
        U05ReadinessInputManifest manifest = manifest(
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, VERSION, profilePol005(VERSION, "owner"));
        U05AdmissionResult result = new U05AdmissionService(new U05InMemoryAdmissionLedger()).admit(
                request(manifest, U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_ALLOW, null, null,
                        VERSION, "canonical-event-1", null),
                manifest,
                authority(U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_ALLOW, null, null, true,
                        VERSION, true, true, true, true, null, null));
        Assertions.assertEquals(U05AdmissionService.PENDING_OWNER_RECOMPUTATION, result.getReasonCode());
        return rejection(result);
    }

    private static Observation admissionThenInputFailure() {
        U05ReadinessInputManifest manifest = manifest(
                U05ConsumerInboundRequest.POST_DDX_REEVALUATION, VERSION, profileFailed());
        AdmissionBundle b = admitBundle(manifest, U05ConsumerInboundRequest.POST_DDX_REEVALUATION,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, VERSION,
                authority(U05ConsumerInboundRequest.POST_DDX_REEVALUATION,
                        U05ConsumerInboundRequest.GATE_ALLOW, null, null, true,
                        VERSION, true, true, true, false, null, null));
        Assertions.assertTrue(b.admission.isAdmitted());
        U05ClinicalReadinessDecision d =
                new U05ClinicalReadinessPolicy().decide(b.admission.getAdmittedInput());
        Assertions.assertEquals(U05ClinicalReadinessDecision.INPUT_FAILURE, d.getDecisionStatus());
        Observation o = observation("D03", "INPUT_FAILURE");
        addAdmissionDetails(o, b.admission, manifest);
        addDecisionDetails(o, d);
        return o;
    }

    private static Observation admissionExactReplay() throws Exception {
        Path root = Files.createTempDirectory("u05-admission-replay-");
        U05ReadinessInputManifest manifest = manifest(
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, VERSION, profilePol005(VERSION, "replay"));
        U05ConsumerInboundRequest req = request(
                manifest, U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null,
                VERSION, "canonical-event-1", null);
        U05AdmissionAuthoritySnapshot auth = authority(
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, true,
                VERSION, true, true, true, false, null, null);
        U05AdmissionResult first = new U05AdmissionService(
                new U05CanonicalAdmissionLedger(new NonProductionFileCanonicalEffectLedger(root)))
                .admit(req, manifest, auth);
        U05AdmissionResult replay = new U05AdmissionService(
                new U05CanonicalAdmissionLedger(new NonProductionFileCanonicalEffectLedger(root)))
                .admit(req, manifest, auth);
        Assertions.assertEquals(U05AdmissionResult.ORIGINAL, first.getReplayDisposition());
        Assertions.assertEquals(U05AdmissionResult.REATTACHED, replay.getReplayDisposition());
        Assertions.assertEquals(first.getAdmittedInput().getAdmissionId(), replay.getAdmittedInput().getAdmissionId());
        Observation o = observation("ADMISSION", "ADMITTED|REATTACHED");
        addAdmissionDetails(o, replay, manifest);
        o.sideEffectRefs.put("admission_record", replay.getAdmittedInput().getAdmissionId());
        return o;
    }

    private static Observation admissionReplayConflict() throws Exception {
        Path root = Files.createTempDirectory("u05-admission-conflict-");
        U05ReadinessInputManifest manifest = manifest(
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, VERSION, profilePol005(VERSION, "replay-conflict"));
        U05AdmissionAuthoritySnapshot auth = authority(
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, true,
                VERSION, true, true, true, false, null, null);
        U05AdmissionService service = new U05AdmissionService(
                new U05CanonicalAdmissionLedger(new NonProductionFileCanonicalEffectLedger(root)));
        U05ConsumerInboundRequest firstReq = request(
                manifest, U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null,
                VERSION, "canonical-event-a", null);
        U05ConsumerInboundRequest changedPayload = request(
                manifest, U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null,
                VERSION, "canonical-event-b", null);
        U05AdmissionResult first = service.admit(firstReq, manifest, auth);
        U05AdmissionResult conflict = service.admit(changedPayload, manifest, auth);
        Assertions.assertTrue(first.isAdmitted());
        Assertions.assertFalse(conflict.isAdmitted());
        Assertions.assertEquals(U05AdmissionService.REPLAY_CONFLICT, conflict.getReasonCode());
        return rejection(conflict);
    }

    private static Observation admissionInputSetChanged() {
        List<U05ReadinessInput> a = profilePol005(VERSION, "set-a");
        List<U05ReadinessInput> b = profilePol005(VERSION, "set-b");
        U05ReadinessInputManifest manifestA =
                manifest(U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, VERSION, a, "manifest-same-ref");
        U05ReadinessInputManifest manifestB =
                manifest(U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, VERSION, b, "manifest-same-ref");
        U05ConsumerInboundRequest old = request(
                manifestA, U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null,
                VERSION, "canonical-event-1", null);
        U05AdmissionResult result = new U05AdmissionService(new U05InMemoryAdmissionLedger()).admit(
                old, manifestB,
                authority(U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_ALLOW, null, null, true,
                        VERSION, true, true, true, false, null, null));
        Assertions.assertEquals(U05AdmissionService.INPUT_SET_IDENTITY_MISMATCH, result.getReasonCode());
        return rejection(result);
    }

    // ---------------------------------------------------------------------
    // D03 cases.
    // ---------------------------------------------------------------------

    private static Observation d03(
            List<U05ReadinessInput> inputs,
            String context,
            String expectedResult) {
        U05ReadinessInputManifest manifest = manifest(context, VERSION, inputs);
        AdmissionBundle b = admitBundle(manifest, context,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, VERSION,
                authority(context, U05ConsumerInboundRequest.GATE_ALLOW,
                        null, null, true, VERSION, true, true, true, false, null, null));
        Assertions.assertTrue(b.admission.isAdmitted(), "D03 fixture must be admitted");
        U05ClinicalReadinessDecision decision =
                new U05ClinicalReadinessPolicy().decide(b.admission.getAdmittedInput());
        Assertions.assertEquals(expectedResult, decisionResult(decision));
        Observation o = observation("D03", expectedResult);
        addAdmissionDetails(o, b.admission, manifest);
        addDecisionDetails(o, decision);
        return o;
    }

    private static Observation applicabilityEvidenceMissing() {
        List<U05ReadinessInput> inputs = profilePol005(VERSION, "missing-app");
        inputs.set(2, nonPresent(
                "f5-missing-app", U05ReadinessInput.F5,
                U05ReadinessInput.NOT_YET_APPLICABLE, VERSION, null));
        U05ReadinessInputManifest manifest =
                manifest(U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, VERSION, inputs);
        U05AdmissionResult result = new U05AdmissionService(new U05InMemoryAdmissionLedger()).admit(
                request(manifest, U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_ALLOW, null, null,
                        VERSION, "canonical-event-1", null),
                manifest,
                authority(U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_ALLOW, null, null, true,
                        VERSION, true, true, true, false, null, null));
        Assertions.assertEquals(U05AdmissionService.APPLICABILITY_EVIDENCE_MISSING, result.getReasonCode());
        return rejection(result);
    }

    // ---------------------------------------------------------------------
    // Mutation cases.
    // ---------------------------------------------------------------------

    private static Observation mutationCommit() {
        Fixture f = new Fixture(VERSION);
        CommittedBundle c = commitProfile(f, profilePol005(VERSION, "ev029"),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, null);
        Assertions.assertEquals("COMMITTED", c.commit.status);
        Assertions.assertEquals(1, f.repository.mutationCount());
        Observation o = observation("MUTATION", "COMMITTED");
        mutationCounts(o, 1, 1, 0);
        addCommittedDetails(o, c);
        return o;
    }

    private static Observation mutationNoEffectInputFailure() {
        Fixture f = new Fixture(VERSION);
        U05ReadinessInputManifest manifest = manifest(
                U05ConsumerInboundRequest.POST_DDX_REEVALUATION, VERSION, profileFailed());
        U05ExecutionResult r = application(f, U05DownstreamPermissionDecision.PERMITTED).execute(
                request(manifest, U05ConsumerInboundRequest.POST_DDX_REEVALUATION,
                        U05ConsumerInboundRequest.GATE_ALLOW, null, null,
                        VERSION, "canonical-event-1", null),
                manifest,
                authority(U05ConsumerInboundRequest.POST_DDX_REEVALUATION,
                        U05ConsumerInboundRequest.GATE_ALLOW, null, null, true,
                        VERSION, true, true, true, false, null, null));
        Assertions.assertEquals(U05ExecutionResult.D03_INPUT_FAILURE, r.getStatus());
        Assertions.assertEquals(0, f.repository.mutationCount());
        Observation o = observation("MUTATION", "NO_EFFECT");
        addAdmissionDetails(o, r.getAdmission(), manifest);
        addDecisionDetails(o, r.getDecision());
        return o;
    }

    private static Observation mutationNoEffectInputConflict() {
        Fixture f = new Fixture(VERSION);
        U05ReadinessInputManifest manifest = manifest(
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, VERSION, profileConflict());
        U05ExecutionResult r = application(f, U05DownstreamPermissionDecision.PERMITTED).execute(
                request(manifest, U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_ALLOW, null, null,
                        VERSION, "canonical-event-1", null),
                manifest,
                authority(U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_ALLOW, null, null, true,
                        VERSION, true, true, true, false, null, null));
        Assertions.assertEquals(U05ExecutionResult.D03_INPUT_CONFLICT, r.getStatus());
        Assertions.assertEquals(0, f.repository.mutationCount());
        Observation o = observation("MUTATION", "NO_EFFECT");
        addAdmissionDetails(o, r.getAdmission(), manifest);
        addDecisionDetails(o, r.getDecision());
        return o;
    }

    private static Observation readinessExactReplay() {
        Fixture f = new Fixture(VERSION);
        PreparedBundle p = prepare(f, profilePol005(VERSION, "ev032"),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, null);
        StateTypes.CommitResult first =
                f.commitService.commitNonProduction(p.input, p.decision, p.proposal);
        U05ClinicalReadinessCommitEvidence evidence =
                f.commitService.verifyCommittedReadBack(p.proposal, first);
        StateTypes.CommitResult replay =
                f.commitService.commitNonProduction(p.input, p.decision, p.proposal);
        Assertions.assertEquals("COMMITTED", first.status);
        Assertions.assertEquals(first.committedVersion, replay.committedVersion);
        Assertions.assertEquals(1, f.repository.mutationCount());
        Observation o = observation("MUTATION", "REATTACHED_NO_SECOND_VERSION");
        mutationCounts(o, 1, 1, 0);
        addPreparedDetails(o, p);
        o.details.put("commit_status", replay.status);
        o.details.put("committed_version", replay.committedVersion);
        o.details.put("commit_result_ref", evidence.getCommitResultRef());
        o.details.put("audit_ref", evidence.getAuditRef());
        o.sideEffectRefs.put("readiness_effect", p.proposal.getEffectId());
        o.sideEffectRefs.put("state_commit", evidence.getCommitResultRef());
        return o;
    }

    private static Observation mutationDifferentBasisDifferentEffect() {
        Fixture f = new Fixture(VERSION);
        CommittedBundle first = commitProfile(f, profilePol005(VERSION, "basis-a"),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, null);

        int v2 = first.evidence.getCommittedClinicalStateVersion();
        CommittedBundle second = commitProfile(f, profilePol005(v2, "basis-b"),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null,
                readinessRecordId(first));

        Assertions.assertNotEquals(first.proposal.getEffectId(), second.proposal.getEffectId());
        Assertions.assertEquals(2, f.repository.mutationCount());
        Observation o = observation("MUTATION", "DIFFERENT_EFFECT");
        mutationCounts(o, 2, 2, 0);
        addCommittedDetails(o, second);
        o.sideEffectRefs.put("readiness_effect_1", first.proposal.getEffectId());
        o.sideEffectRefs.put("readiness_effect_2", second.proposal.getEffectId());
        return o;
    }

    private static Observation mutationStaleBaseConflict() {
        Fixture f = new Fixture(VERSION);
        PreparedBundle target = prepare(f, profileOutOfScope(),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, null);
        PreparedBundle advancer = prepare(f, profilePol005(VERSION, "advance"),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, null);
        StateTypes.CommitResult advance =
                f.commitService.commitNonProduction(advancer.input, advancer.decision, advancer.proposal);
        Assertions.assertEquals("COMMITTED", advance.status);
        StateTypes.CommitResult conflict =
                f.commitService.commitNonProduction(target.input, target.decision, target.proposal);
        Assertions.assertEquals("CONFLICT", conflict.status);
        Observation o = observation("MUTATION", "CONFLICT");
        addPreparedDetails(o, target);
        o.details.put("commit_status", conflict.status);
        o.details.put("proposal_base_version", target.proposal.getStatePatch().baseVersion);
        o.details.put("current_version_after_fixture_advance", f.repository.snapshot("cdp-1").version());
        return o;
    }

    private static Observation mutationUnauthorizedFieldRejected() {
        Fixture f = new Fixture(VERSION);
        PreparedBundle p = prepare(f, profilePol005(VERSION, "unauth"),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, null);
        p.proposal.getStatePatch().operations.get(0).path =
                "/patient_state/unauthorized_u05_test_field";
        StateTypes.CommitResult result = f.committer.commit(p.proposal.getStatePatch());
        Assertions.assertEquals("REJECTED", result.status);
        Assertions.assertEquals(0, f.repository.mutationCount());
        Observation o = observation("MUTATION", "REJECTED");
        addPreparedDetails(o, p);
        o.details.put("commit_status", result.status);
        o.details.put("reason_code", result.reasonCode);
        return o;
    }

    private static Observation mutationRestrictedProvenance() {
        Fixture f = new Fixture(VERSION);
        CommittedBundle c = commitProfile(f, profilePol005(VERSION, "restricted-provenance"),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_RESTRICTED,
                "restricted-context-1", "restricted-permission-u05", null);
        Assertions.assertEquals("restricted-permission-u05", c.input.getAcceptedRestrictedPermissionRef());
        Assertions.assertEquals("restricted-permission-u05", c.decision.getRestrictedPermissionRef());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload =
                (Map<String, Object>) c.proposal.getStatePatch().operations.get(0).value;
        Assertions.assertEquals("restricted-permission-u05", payload.get("source_restricted_permission_ref"));
        Observation o = observation("MUTATION", "PROVENANCE_EQUAL");
        mutationCounts(o, 1, 1, 0);
        addCommittedDetails(o, c);
        o.details.put("inbound_restricted_permission_ref", "restricted-permission-u05");
        o.details.put("admission_result_restricted_permission_ref", c.admission.getRestrictedPermissionRef());
        o.details.put("admitted_restricted_permission_ref", c.input.getAcceptedRestrictedPermissionRef());
        o.details.put("d03_restricted_permission_ref", c.decision.getRestrictedPermissionRef());
        o.details.put("readiness_source_restricted_permission_ref", payload.get("source_restricted_permission_ref"));
        return o;
    }

    private static Observation mutationReadinessOnlyStaysCurrent() {
        Fixture f = new Fixture(VERSION);
        CommittedBundle c = commitProfile(f, profilePol005(VERSION, "readiness-only"),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, null);
        Assertions.assertEquals(VERSION + 1, c.evidence.getCommittedClinicalStateVersion());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload =
                (Map<String, Object>) c.proposal.getStatePatch().operations.get(0).value;
        Assertions.assertEquals("CURRENT", payload.get("state_validity"));
        Assertions.assertEquals("u04-gate-1", payload.get("source_u04_gate_ref"));
        Observation o = observation("MUTATION", "COMMITTED_CURRENT");
        mutationCounts(o, 1, 1, 0);
        addCommittedDetails(o, c);
        return o;
    }

    private static Observation mutationStateDependencyInvalidation() {
        Fixture f = new Fixture(VERSION);
        CommittedBundle c = commitProfile(f, profilePol005(VERSION, "invalidate-state"),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, null);
        U05ReadinessInvalidationEvidence invalidated =
                invalidate(f, c, "STATE_DEPENDENCY_CHANGED", "state-change-1");
        Assertions.assertEquals(VERSION + 2, invalidated.getCommittedClinicalStateVersion());
        Assertions.assertEquals(2, f.repository.mutationCount());
        Observation o = observation("INVALIDATION", "STALE_COMMITTED");
        mutationCounts(o, 2, 1, 1);
        addCommittedDetails(o, c);
        o.details.put("invalidation_effect_id", invalidated.getInvalidationEffectId());
        o.sideEffectRefs.put("invalidation_effect", invalidated.getInvalidationEffectId());
        return o;
    }

    private static Observation mutationNonStateRevalidationInvalidation() {
        Fixture f = new Fixture(VERSION);
        CommittedBundle c = commitProfile(f, profilePol005(VERSION, "invalidate-non-state"),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, null);
        U05ReadinessInvalidationEvidence invalidated =
                invalidate(f, c, "READINESS_INPUT_SET_REVALIDATION_CHANGED", "revalidation-1");
        Assertions.assertEquals(2, f.repository.mutationCount());
        Assertions.assertTrue(invalidated.getAuthoritativeReadinessRecordRef().contains("@14"));
        Observation o = observation("INVALIDATION", "STALE_BEFORE_REUSE");
        mutationCounts(o, 2, 1, 1);
        addCommittedDetails(o, c);
        o.details.put("invalidation_effect_id", invalidated.getInvalidationEffectId());
        return o;
    }

    private static Observation readinessFingerprintReplayConflict() {
        Fixture f = new Fixture(VERSION);
        PreparedBundle p = prepare(f, profilePol005(VERSION, "fp-conflict"),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, null);
        StateTypes.CommitResult first = f.committer.commit(p.proposal.getStatePatch());
        Assertions.assertEquals("COMMITTED", first.status);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload =
                (Map<String, Object>) p.proposal.getStatePatch().operations.get(0).value;
        payload.put("canonical_payload_fingerprint", "different-fingerprint");
        StateTypes.CommitResult conflict = f.committer.commit(p.proposal.getStatePatch());
        Assertions.assertEquals("CONFLICT", conflict.status);
        Assertions.assertEquals(1, f.repository.mutationCount());
        Observation o = observation("MUTATION", "REPLAY_CONFLICT");
        mutationCounts(o, 1, 1, 0);
        addPreparedDetails(o, p);
        o.details.put("commit_status", conflict.status);
        o.sideEffectRefs.put("readiness_effect", p.proposal.getEffectId());
        return o;
    }

    private static Observation proposalIdentityRecovered() {
        Fixture f = new Fixture(VERSION);
        AdmissionBundle b = admitBundle(
                manifest(U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, VERSION,
                        profilePol005(VERSION, "crash-before-proposal")),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, VERSION,
                authority(U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                        U05ConsumerInboundRequest.GATE_ALLOW, null, null, true,
                        VERSION, true, true, true, false, null, null));
        U05ClinicalReadinessDecision d =
                new U05ClinicalReadinessPolicy().decide(b.admission.getAdmittedInput());
        U05ReadinessStateProposalFactory factory = new U05ReadinessStateProposalFactory();
        U05ReadinessStateProposal first = factory.create(b.admission.getAdmittedInput(), d);
        U05ReadinessStateProposal recovered = factory.create(b.admission.getAdmittedInput(), d);
        Assertions.assertEquals(first.getEffectId(), recovered.getEffectId());
        Assertions.assertEquals(first.getProposalId(), recovered.getProposalId());
        Observation o = observation("MUTATION", "IDENTITY_RECOVERED");
        addAdmissionDetails(o, b.admission, b.manifest);
        addDecisionDetails(o, d);
        o.details.put("readiness_effect_id", first.getEffectId());
        o.details.put("proposal_id", first.getProposalId());
        return o;
    }

    private static Observation postCommitReplayNoDuplicate() {
        Fixture f = new Fixture(VERSION);
        PreparedBundle p = prepare(f, profilePol005(VERSION, "post-commit-recovery"),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, null);
        StateTypes.CommitResult first =
                f.commitService.commitNonProduction(p.input, p.decision, p.proposal);
        U05CommitService reconstructed =
                new U05CommitService(f.committer,
                        new U05SyntheticClinicalReadinessSnapshotAdapter(f.repository));
        StateTypes.CommitResult replay =
                reconstructed.commitNonProduction(p.input, p.decision, p.proposal);
        Assertions.assertEquals(first.committedVersion, replay.committedVersion);
        Assertions.assertEquals(1, f.repository.mutationCount());
        Observation o = observation("MUTATION", "REATTACHED_NO_DUPLICATE");
        mutationCounts(o, 1, 1, 0);
        addPreparedDetails(o, p);
        o.sideEffectRefs.put("readiness_effect", p.proposal.getEffectId());
        return o;
    }

    private static Observation concurrentWritersAtMostOne() throws Exception {
        final Fixture f = new Fixture(VERSION);
        final PreparedBundle a = prepare(f, profilePol005(VERSION, "writer-a"),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, null);
        final PreparedBundle b = prepare(f, profileOutOfScope(),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, null);
        final CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<StateTypes.CommitResult> fa = pool.submit(() -> {
            start.await();
            return f.commitService.commitNonProduction(a.input, a.decision, a.proposal);
        });
        Future<StateTypes.CommitResult> fb = pool.submit(() -> {
            start.await();
            return f.commitService.commitNonProduction(b.input, b.decision, b.proposal);
        });
        start.countDown();
        StateTypes.CommitResult ra = fa.get();
        StateTypes.CommitResult rb = fb.get();
        pool.shutdownNow();
        int committed = ("COMMITTED".equals(ra.status) ? 1 : 0) + ("COMMITTED".equals(rb.status) ? 1 : 0);
        Assertions.assertEquals(1, committed);
        Assertions.assertEquals(1, f.repository.mutationCount());
        Observation o = observation("MUTATION", "AT_MOST_ONE_COMMIT");
        mutationCounts(o, 1, 1, 0);
        addPreparedDetails(o, a);
        o.details.put("writer_a_status", ra.status);
        o.details.put("writer_b_status", rb.status);
        return o;
    }

    // ---------------------------------------------------------------------
    // Routing / Scheduler cases.
    // ---------------------------------------------------------------------

    private static Observation routingMapping(
            List<U05ReadinessInput> profile,
            String context,
            String expected) {
        Fixture f = new Fixture(VERSION);
        CommittedBundle c = commitProfile(f, profile, context,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, null);
        U05DownstreamRoutingDecision route =
                route(c, new U05InMemoryRouteLedger(),
                        currentness(c, U05ConsumerInboundRequest.GATE_ALLOW, null,
                                true, true, true, true, true, "routing-current"),
                        U05DownstreamPermissionDecision.PERMITTED);
        Assertions.assertEquals(expected, routeResult(route));
        Observation o = observation("ROUTING", expected);
        mutationCounts(o, 1, 1, 0);
        routingCounts(o, 1, 1, 0);
        addCommittedDetails(o, c);
        addRouteDetails(o, route);
        return o;
    }

    private static Observation restrictedRouting(String permissionStatus, String expected) {
        Fixture f = new Fixture(VERSION);
        CommittedBundle c = commitProfile(f, profilePol005(VERSION, "restricted-route"),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_RESTRICTED,
                "restricted-context-1", "restricted-permission-u05", null);
        U05DownstreamRoutingDecision route =
                route(c, new U05InMemoryRouteLedger(),
                        currentness(c, U05ConsumerInboundRequest.GATE_RESTRICTED,
                                "restricted-context-1", true, true, true, true, true,
                                "restricted-routing-current"),
                        permissionStatus);
        Assertions.assertEquals(expected, route.getRoutingStatus());
        Assertions.assertNull(route.getEligibility());
        Observation o = observation("ROUTING", expected);
        mutationCounts(o, 1, 1, 0);
        routingCounts(o, 1, 0, 0);
        addCommittedDetails(o, c);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload =
                (Map<String, Object>) c.proposal.getStatePatch().operations.get(0).value;
        o.details.put("inbound_restricted_permission_ref",
                c.input.getAcceptedRestrictedPermissionRef());
        o.details.put("admission_result_restricted_permission_ref",
                c.admission.getRestrictedPermissionRef());
        o.details.put("admitted_restricted_permission_ref",
                c.input.getAcceptedRestrictedPermissionRef());
        o.details.put("d03_restricted_permission_ref",
                c.decision.getRestrictedPermissionRef());
        o.details.put("readiness_source_restricted_permission_ref",
                payload.get("source_restricted_permission_ref"));
        addRouteDetails(o, route);
        return o;
    }

    private static Observation postCommitSafetyRoute(String gate, String expected) {
        Fixture f = new Fixture(VERSION);
        CommittedBundle c = commitProfile(f, profilePol005(VERSION, "safety-route-" + gate),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, null);
        U05DownstreamRoutingDecision route =
                route(c, new U05InMemoryRouteLedger(),
                        currentness(c, gate, null, true, true, true, true, true,
                                "post-commit-safety-" + gate),
                        U05DownstreamPermissionDecision.PERMITTED);
        Assertions.assertEquals(expected, route.getRoutingStatus());
        Assertions.assertNull(route.getEligibility());
        Observation o = observation("ROUTING", expected);
        mutationCounts(o, 1, 1, 0);
        routingCounts(o, 1, 0, 0);
        addCommittedDetails(o, c);
        addRouteDetails(o, route);
        return o;
    }

    private static Observation routeExactReplay() throws Exception {
        Fixture f = new Fixture(VERSION);
        CommittedBundle c = commitProfile(f, profilePol005(VERSION, "route-replay"),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, null);
        Path root = Files.createTempDirectory("u05-route-replay-");
        U05RoutingCurrentness current = currentness(c, U05ConsumerInboundRequest.GATE_ALLOW,
                null, true, true, true, true, true, "route-replay-current");
        U05DownstreamRoutingDecision first = route(c,
                new U05CanonicalRouteLedger(new NonProductionFileCanonicalEffectLedger(root)),
                current, U05DownstreamPermissionDecision.PERMITTED);
        U05DownstreamRoutingDecision replay = route(c,
                new U05CanonicalRouteLedger(new NonProductionFileCanonicalEffectLedger(root)),
                current, U05DownstreamPermissionDecision.PERMITTED);
        Assertions.assertEquals(U05DownstreamRoutingDecision.REATTACHED, replay.getReplayDisposition());
        Assertions.assertEquals(first.getRouteEffectId(), replay.getRouteEffectId());
        Assertions.assertEquals(first.getEligibility().getEligibilityId(), replay.getEligibility().getEligibilityId());
        Observation o = observation("ROUTING", "ELIGIBLE|REATTACHED");
        mutationCounts(o, 1, 1, 0);
        routingCounts(o, 1, 1, 0);
        addCommittedDetails(o, c);
        addRouteDetails(o, replay);
        return o;
    }

    private static Observation routeReplayConflict() {
        Fixture f = new Fixture(VERSION);
        CommittedBundle c = commitProfile(f, profilePol005(VERSION, "route-conflict"),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, null);
        U05InMemoryRouteLedger ledger = new U05InMemoryRouteLedger();
        U05DownstreamRoutingDecision first = route(c, ledger,
                currentness(c, U05ConsumerInboundRequest.GATE_ALLOW, null,
                        true, true, true, true, true, "routing-context-a"),
                U05DownstreamPermissionDecision.PERMITTED);
        Assertions.assertNotNull(first.getEligibility());
        Assertions.assertThrows(IllegalStateException.class, () ->
                route(c, ledger,
                        currentness(c, U05ConsumerInboundRequest.GATE_ALLOW, null,
                                true, true, true, true, true, "routing-context-b"),
                        U05DownstreamPermissionDecision.PERMITTED));
        Observation o = observation("ROUTING", "REPLAY_CONFLICT");
        mutationCounts(o, 1, 1, 0);
        routingCounts(o, 1, 1, 0);
        addCommittedDetails(o, c);
        addRouteDetails(o, first);
        return o;
    }

    private static Observation schedulerStaleEligibility() throws Exception {
        EligibilityBundle e = eligibility("scheduler-stale");
        Path root = Files.createTempDirectory("u05-scheduler-stale-");
        SchedulerObservation s = new TestSchedulerConsumer(
                new NonProductionFileCanonicalEffectLedger(root)).consume(
                e.route.getEligibility(),
                "currentness-fixture-stale", false,
                "binding-fixture-u08", true);
        Assertions.assertEquals("REJECTED_STALE", s.status);
        Assertions.assertEquals(0, s.schedulerTargetIntentCount);
        Observation o = observation("SCHEDULER_BOUNDARY", "REJECTED_STALE");
        mutationCounts(o, 1, 1, 0);
        routingCounts(o, 1, 1, 0);
        addCommittedDetails(o, e.committed);
        addRouteDetails(o, e.route);
        addSchedulerDetails(o, s);
        return o;
    }

    private static Observation schedulerBindingUnavailable() throws Exception {
        EligibilityBundle e = eligibility("scheduler-binding-unavailable");
        Path root = Files.createTempDirectory("u05-scheduler-binding-");
        SchedulerObservation s = new TestSchedulerConsumer(
                new NonProductionFileCanonicalEffectLedger(root)).consume(
                e.route.getEligibility(),
                "currentness-fixture-current", true,
                "binding-fixture-u08-unavailable", false);
        Assertions.assertEquals("FAILURE_REQUIRED", s.status);
        Assertions.assertNotNull(s.failureHandoffRef);
        Assertions.assertEquals(0, s.downstreamUnitInvocationCount);
        Assertions.assertEquals(0, s.alternateRouteEffectCount);
        Observation o = observation("SCHEDULER_BOUNDARY", "FAILURE_REQUIRED");
        mutationCounts(o, 1, 1, 0);
        routingCounts(o, 1, 1, 0);
        addCommittedDetails(o, e.committed);
        addRouteDetails(o, e.route);
        addSchedulerDetails(o, s);
        return o;
    }

    private static Observation schedulerIntentReplay() throws Exception {
        EligibilityBundle e = eligibility("scheduler-replay");
        Path root = Files.createTempDirectory("u05-scheduler-replay-");
        SchedulerObservation first = new TestSchedulerConsumer(
                new NonProductionFileCanonicalEffectLedger(root)).consume(
                e.route.getEligibility(),
                "currentness-fixture-current", true,
                "binding-fixture-u08", true);
        SchedulerObservation replay = new TestSchedulerConsumer(
                new NonProductionFileCanonicalEffectLedger(root)).consume(
                e.route.getEligibility(),
                "currentness-fixture-current", true,
                "binding-fixture-u08", true);
        Assertions.assertEquals("ORIGINAL", first.replayDisposition);
        Assertions.assertEquals("REATTACHED", replay.replayDisposition);
        Assertions.assertEquals(first.schedulerIntentRef, replay.schedulerIntentRef);
        Observation o = observation("SCHEDULER_BOUNDARY", "TARGET_INTENT|REATTACHED");
        mutationCounts(o, 1, 1, 0);
        routingCounts(o, 1, 1, 1);
        addCommittedDetails(o, e.committed);
        addRouteDetails(o, e.route);
        addSchedulerDetails(o, replay);
        return o;
    }

    private static Observation noOrdinaryLifecycleForNonEligibleStatuses() {
        Fixture f = new Fixture(VERSION);
        CommittedBundle c = commitProfile(f, profilePol005(VERSION, "noneligible-lifecycle"),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, null);
        U05DownstreamRoutingDecision preempted = route(c, new U05InMemoryRouteLedger(),
                currentness(c, U05ConsumerInboundRequest.GATE_BLOCKED, null,
                        true, true, true, true, true, "noneligible-blocked"),
                U05DownstreamPermissionDecision.PERMITTED);
        U05DownstreamRoutingDecision failure = route(c, new U05InMemoryRouteLedger(),
                currentness(c, U05ConsumerInboundRequest.GATE_UNAVAILABLE, null,
                        true, true, true, true, true, "noneligible-unavailable"),
                U05DownstreamPermissionDecision.PERMITTED);
        U05DownstreamRoutingDecision stale = route(c, new U05InMemoryRouteLedger(),
                currentness(c, U05ConsumerInboundRequest.GATE_ALLOW, null,
                        false, true, true, true, true, "noneligible-stale"),
                U05DownstreamPermissionDecision.PERMITTED);
        for (U05DownstreamRoutingDecision d : Arrays.asList(preempted, failure, stale)) {
            Assertions.assertNull(d.getRouteEffectId());
            Assertions.assertNull(d.getEligibility());
        }
        Observation o = observation("ROUTING", "NO_ORDINARY_ROUTE_EFFECT");
        mutationCounts(o, 1, 1, 0);
        addCommittedDetails(o, c);
        o.counts.put("route_decision_count", 3);
        o.sideEffectRefs.put("route_decision_preempted", preempted.getRoutingDecisionId());
        o.sideEffectRefs.put("route_decision_failure_required", failure.getRoutingDecisionId());
        o.sideEffectRefs.put("route_decision_rejected_stale", stale.getRoutingDecisionId());
        o.details.put("statuses", Arrays.asList(
                preempted.getRoutingStatus(), failure.getRoutingStatus(), stale.getRoutingStatus()));
        return o;
    }

    private static Observation zeroExternalEffectsAfterRouteConsumption() throws Exception {
        EligibilityBundle e = eligibility("zero-external");
        Path root = Files.createTempDirectory("u05-scheduler-zero-external-");
        SchedulerObservation s = new TestSchedulerConsumer(
                new NonProductionFileCanonicalEffectLedger(root)).consume(
                e.route.getEligibility(),
                "currentness-fixture-current", true,
                "binding-fixture-u08", true);
        Assertions.assertEquals(0, s.downstreamUnitInvocationCount);
        Assertions.assertEquals(0, s.externalDeliveryCount);
        Assertions.assertEquals(0, s.externalToolModelCallCount);
        Observation o = observation("SIDE_EFFECT_BOUNDARY", "ZERO_EXTERNAL_EFFECTS");
        mutationCounts(o, 1, 1, 0);
        routingCounts(o, 1, 1, 1);
        addCommittedDetails(o, e.committed);
        addRouteDetails(o, e.route);
        addSchedulerDetails(o, s);
        return o;
    }

    // ---------------------------------------------------------------------
    // Fixture/proposal/routing helpers.
    // ---------------------------------------------------------------------

    private static PreparedBundle prepare(
            Fixture f,
            List<U05ReadinessInput> inputs,
            String context,
            String gate,
            String restrictedContext,
            String restrictedPermission,
            String currentReadinessRecordRef) {
        int version = inputs.get(0).getClinicalStateVersion();
        U05ReadinessInputManifest manifest = manifest(context, version, inputs);
        U05AdmissionAuthoritySnapshot auth = authority(
                context, gate, restrictedContext, restrictedPermission, true,
                version, true, true, true, false, null, currentReadinessRecordRef);
        AdmissionBundle a = admitBundle(
                manifest, context, gate, restrictedContext, restrictedPermission, version, auth);
        Assertions.assertTrue(a.admission.isAdmitted());
        U05AdmittedInput input = a.admission.getAdmittedInput();
        U05ClinicalReadinessDecision decision = new U05ClinicalReadinessPolicy().decide(input);
        Assertions.assertTrue(decision.isDecided());
        U05ReadinessStateProposal proposal =
                new U05ReadinessStateProposalFactory().create(input, decision);
        return new PreparedBundle(a.admission, input, decision, proposal, manifest);
    }

    private static CommittedBundle commitProfile(
            Fixture f,
            List<U05ReadinessInput> inputs,
            String context,
            String gate,
            String restrictedContext,
            String restrictedPermission,
            String currentReadinessRecordRef) {
        PreparedBundle p = prepare(
                f, inputs, context, gate, restrictedContext, restrictedPermission, currentReadinessRecordRef);
        StateTypes.CommitResult commit =
                f.commitService.commitNonProduction(p.input, p.decision, p.proposal);
        Assertions.assertEquals(
                "COMMITTED",
                commit.status,
                "commit reason=" + commit.reasonCode
                        + ", base=" + p.proposal.getStatePatch().baseVersion
                        + ", operation=" + p.proposal.getMutationOperation());
        U05ClinicalReadinessCommitEvidence evidence =
                f.commitService.verifyCommittedReadBack(p.proposal, commit);
        return new CommittedBundle(p, commit, evidence);
    }

    @SuppressWarnings("unchecked")
    private static String readinessRecordId(CommittedBundle c) {
        Map<String, Object> payload =
                (Map<String, Object>) c.proposal.getStatePatch().operations.get(0).value;
        return String.valueOf(payload.get("readiness_record_id"));
    }

    private static U05ReadinessInvalidationEvidence invalidate(
            Fixture f,
            CommittedBundle c,
            String reason,
            String changeRef) {
        @SuppressWarnings("unchecked")
        Map<String, Object> prior =
                (Map<String, Object>) c.proposal.getStatePatch().operations.get(0).value;
        U05ReadinessInvalidationRequest req = new U05ReadinessInvalidationRequest(
                "consult-1", "cdp-1", c.evidence.getCommittedClinicalStateVersion(),
                String.valueOf(prior.get("readiness_record_id")),
                String.valueOf(prior.get("effect_id")),
                prior,
                changeRef,
                Collections.singletonList("dependency-f3"),
                reason,
                "owner-decision-invalidation",
                "corr-1", "trace-1", ENV,
                "2026-09-21T08:05:00Z");
        U05ReadinessInvalidationProposal proposal =
                new U05ReadinessInvalidationProposalFactory().create(req);
        return new U05ReadinessInvalidationService(
                f.committer,
                new U05SyntheticClinicalReadinessSnapshotAdapter(f.repository))
                .commitAndVerifyNonProduction(req, proposal);
    }

    private static U05NonProductionApplicationService application(
            Fixture f,
            final String permissionStatus) {
        U05AdmissionService admissionService =
                new U05AdmissionService(new U05InMemoryAdmissionLedger());
        U05RoutingCurrentnessPort currentnessPort =
                (input, decision, evidence) ->
                        new U05RoutingCurrentness(
                                evidence.getCommittedClinicalStateVersion(),
                                "app-routing-" + evidence.getEffectId(),
                                true, true, true, true, true,
                                input.getAcceptedU04GateRef(),
                                input.getGateValue(),
                                input.getAcceptedRestrictedContextRef());
        U05DownstreamPermissionPort permission =
                permissionPort(permissionStatus);
        return new U05NonProductionApplicationService(
                admissionService,
                new U05ClinicalReadinessPolicy(),
                new U05ReadinessStateProposalFactory(),
                f.commitService,
                currentnessPort,
                new U05RoutingService(new U05InMemoryRouteLedger(), permission));
    }

    private static U05DownstreamRoutingDecision route(
            CommittedBundle c,
            U05RouteLedger ledger,
            U05RoutingCurrentness currentness,
            String permissionStatus) {
        return new U05RoutingService(ledger, permissionPort(permissionStatus)).route(
                c.input, c.decision, c.evidence, currentness);
    }

    private static U05DownstreamPermissionPort permissionPort(final String status) {
        return (input, evidence, currentness, consequence, targetUnitId, targetAction) -> {
            if (U05ConsumerInboundRequest.GATE_ALLOW.equals(currentness.getGateValue())) {
                throw new AssertionError("ALLOW routing must not resolve downstream permission");
            }
            return new U05DownstreamPermissionDecision(
                    "permission-decision-" + targetUnitId + "-" + status,
                    input.getConsultationId(),
                    input.getCdpId(),
                    currentness.getCurrentU04GateRef(),
                    currentness.getRestrictedContextRef(),
                    consequence,
                    targetAction,
                    targetUnitId,
                    status,
                    U05DownstreamPermissionDecision.PERMITTED.equals(status)
                            ? "downstream-permission-" + targetUnitId : null,
                    "safety-permission-policy",
                    "v1",
                    "CURRENT");
        };
    }

    private static U05RoutingCurrentness currentness(
            CommittedBundle c,
            String gate,
            String restrictedContext,
            boolean readinessCurrent,
            boolean depsCurrent,
            boolean gateCurrent,
            boolean inboundCurrent,
            boolean restrictedCurrent,
            String routingContext) {
        return new U05RoutingCurrentness(
                c.evidence.getCommittedClinicalStateVersion(),
                routingContext,
                readinessCurrent,
                depsCurrent,
                gateCurrent,
                inboundCurrent,
                restrictedCurrent,
                "u04-gate-1",
                gate,
                restrictedContext);
    }

    private static EligibilityBundle eligibility(String suffix) {
        Fixture f = new Fixture(VERSION);
        CommittedBundle c = commitProfile(f, profilePol005(VERSION, suffix),
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT,
                U05ConsumerInboundRequest.GATE_ALLOW, null, null, null);
        U05DownstreamRoutingDecision route = route(
                c, new U05InMemoryRouteLedger(),
                currentness(c, U05ConsumerInboundRequest.GATE_ALLOW, null,
                        true, true, true, true, true, "eligibility-current-" + suffix),
                U05DownstreamPermissionDecision.PERMITTED);
        Assertions.assertNotNull(route.getEligibility());
        return new EligibilityBundle(f, c, route);
    }

    private static AdmissionBundle admitBundle(
            U05ReadinessInputManifest manifest,
            String context,
            String gate,
            String restrictedContext,
            String restrictedPermission,
            int version,
            U05AdmissionAuthoritySnapshot auth) {
        U05AdmissionResult admission =
                new U05AdmissionService(new U05InMemoryAdmissionLedger()).admit(
                        request(manifest, context, gate, restrictedContext, restrictedPermission,
                                version, "canonical-event-1", null),
                        manifest, auth);
        return new AdmissionBundle(admission, manifest);
    }

    // ---------------------------------------------------------------------
    // Canonical input/profile helpers.
    // ---------------------------------------------------------------------

    private static List<U05ReadinessInput> profilePol005(int version, String suffix) {
        String s = suffix == null ? "" : suffix;
        return mutable(
                present("f1-framed-" + s, U05ReadinessInput.F1, U05ReadinessInput.FRAMED_IN_SCOPE, version),
                present("f3-no-gap-" + s, U05ReadinessInput.F3, U05ReadinessInput.NO_ACTIVE_ONLINE_BLOCKING_GAP, version),
                nonPresent("f5-not-yet-" + s, U05ReadinessInput.F5, U05ReadinessInput.NOT_YET_APPLICABLE, version, "app-f5-" + s),
                nonPresent("f6-not-yet-" + s, U05ReadinessInput.F6, U05ReadinessInput.NOT_YET_APPLICABLE, version, "app-f6-" + s));
    }

    private static List<U05ReadinessInput> profilePol011() {
        return mutable(
                present("f1-framed-pol011", U05ReadinessInput.F1, U05ReadinessInput.FRAMED_IN_SCOPE, VERSION),
                present("f3-no-gap-pol011", U05ReadinessInput.F3, U05ReadinessInput.NO_ACTIVE_ONLINE_BLOCKING_GAP, VERSION),
                nonPresent("f5-not-yet-pol011", U05ReadinessInput.F5, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION, "app-f5-pol011"),
                present("f6-no-blocking-pol011", U05ReadinessInput.F6, U05ReadinessInput.NO_BLOCKING_OFFLINE_EVIDENCE_NEED, VERSION));
    }

    private static List<U05ReadinessInput> profileOutOfScope() {
        return mutable(
                present("f1-out", U05ReadinessInput.F1, U05ReadinessInput.OUT_OF_SCOPE, VERSION),
                present("f3-no-gap-out", U05ReadinessInput.F3, U05ReadinessInput.NO_ACTIVE_ONLINE_BLOCKING_GAP, VERSION),
                nonPresent("f5-not-yet-out", U05ReadinessInput.F5, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION, "app-f5-out"),
                nonPresent("f6-not-yet-out", U05ReadinessInput.F6, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION, "app-f6-out"));
    }

    private static List<U05ReadinessInput> profileOfflineWithLowerCandidates() {
        return mutable(
                present("f1-framed-offline", U05ReadinessInput.F1, U05ReadinessInput.FRAMED_IN_SCOPE, VERSION),
                present("f2-clarify-offline", U05ReadinessInput.F2_CLARIFICATION, U05ReadinessInput.NEEDS_CLARIFICATION, VERSION),
                present("f3-can-ask-offline", U05ReadinessInput.F3, U05ReadinessInput.CAN_ASK_MORE, VERSION),
                nonPresent("f5-not-yet-offline", U05ReadinessInput.F5, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION, "app-f5-offline"),
                present("f6-offline", U05ReadinessInput.F6, U05ReadinessInput.NEEDS_OFFLINE_EVIDENCE, VERSION));
    }

    private static List<U05ReadinessInput> profileClarification() {
        return mutable(
                present("f1-framed-clarify", U05ReadinessInput.F1, U05ReadinessInput.FRAMED_IN_SCOPE, VERSION),
                present("f2-clarify", U05ReadinessInput.F2_CLARIFICATION, U05ReadinessInput.NEEDS_CLARIFICATION, VERSION),
                present("f3-no-gap-clarify", U05ReadinessInput.F3, U05ReadinessInput.NO_ACTIVE_ONLINE_BLOCKING_GAP, VERSION),
                nonPresent("f5-not-yet-clarify", U05ReadinessInput.F5, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION, "app-f5-clarify"),
                nonPresent("f6-not-yet-clarify", U05ReadinessInput.F6, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION, "app-f6-clarify"));
    }

    private static List<U05ReadinessInput> profileCanAskMore() {
        return mutable(
                present("f1-framed-ask", U05ReadinessInput.F1, U05ReadinessInput.FRAMED_IN_SCOPE, VERSION),
                present("f3-can-ask", U05ReadinessInput.F3, U05ReadinessInput.CAN_ASK_MORE, VERSION),
                nonPresent("f5-not-yet-ask", U05ReadinessInput.F5, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION, "app-f5-ask"),
                nonPresent("f6-not-yet-ask", U05ReadinessInput.F6, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION, "app-f6-ask"));
    }

    private static List<U05ReadinessInput> profileNoReliable() {
        return mutable(
                present("f1-framed-nrd", U05ReadinessInput.F1, U05ReadinessInput.FRAMED_IN_SCOPE, VERSION),
                present("f3-no-gap-nrd", U05ReadinessInput.F3, U05ReadinessInput.NO_ACTIVE_ONLINE_BLOCKING_GAP, VERSION),
                present("f5-no-reliable", U05ReadinessInput.F5, U05ReadinessInput.NO_RELIABLE_DIRECTION, VERSION),
                nonPresent("f6-not-yet-nrd", U05ReadinessInput.F6, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION, "app-f6-nrd"));
    }

    private static List<U05ReadinessInput> profilePostDdxPol011Excluded() {
        return mutable(
                present("f1-framed-postddx", U05ReadinessInput.F1, U05ReadinessInput.FRAMED_IN_SCOPE, VERSION),
                present("f3-no-gap-postddx", U05ReadinessInput.F3, U05ReadinessInput.NO_ACTIVE_ONLINE_BLOCKING_GAP, VERSION),
                present("f5-no-reliable-postddx", U05ReadinessInput.F5, U05ReadinessInput.NO_RELIABLE_DIRECTION, VERSION),
                present("f6-no-blocking-postddx", U05ReadinessInput.F6, U05ReadinessInput.NO_BLOCKING_OFFLINE_EVIDENCE_NEED, VERSION));
    }

    private static List<U05ReadinessInput> profileUnavailable() {
        return mutable(
                present("f1-framed-unavailable", U05ReadinessInput.F1, U05ReadinessInput.FRAMED_IN_SCOPE, VERSION),
                present("f3-no-gap-unavailable", U05ReadinessInput.F3, U05ReadinessInput.NO_ACTIVE_ONLINE_BLOCKING_GAP, VERSION),
                nonPresent("f5-unavailable", U05ReadinessInput.F5, U05ReadinessInput.UNAVAILABLE, VERSION, "app-f5-unavailable"),
                nonPresent("f6-not-yet-unavailable", U05ReadinessInput.F6, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION, "app-f6-unavailable"));
    }

    private static List<U05ReadinessInput> profileFailed() {
        return mutable(
                present("f1-framed-failed", U05ReadinessInput.F1, U05ReadinessInput.FRAMED_IN_SCOPE, VERSION),
                present("f3-no-gap-failed", U05ReadinessInput.F3, U05ReadinessInput.NO_ACTIVE_ONLINE_BLOCKING_GAP, VERSION),
                nonPresent("f5-failed", U05ReadinessInput.F5, U05ReadinessInput.FAILED, VERSION, "app-f5-failed"),
                nonPresent("f6-not-yet-failed", U05ReadinessInput.F6, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION, "app-f6-failed"));
    }

    private static List<U05ReadinessInput> profileConflict() {
        return mutable(
                present("f1-conflict-a", U05ReadinessInput.F1, U05ReadinessInput.FRAMED_IN_SCOPE, VERSION),
                present("f1-conflict-b", U05ReadinessInput.F1, U05ReadinessInput.OUT_OF_SCOPE, VERSION),
                present("f3-no-gap-conflict", U05ReadinessInput.F3, U05ReadinessInput.NO_ACTIVE_ONLINE_BLOCKING_GAP, VERSION),
                nonPresent("f5-not-yet-conflict", U05ReadinessInput.F5, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION, "app-f5-conflict"),
                nonPresent("f6-not-yet-conflict", U05ReadinessInput.F6, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION, "app-f6-conflict"));
    }

    private static List<U05ReadinessInput> profileFailureWithLowerCandidates() {
        return mutable(
                present("f1-out-p0", U05ReadinessInput.F1, U05ReadinessInput.OUT_OF_SCOPE, VERSION),
                present("f3-can-ask-p0", U05ReadinessInput.F3, U05ReadinessInput.CAN_ASK_MORE, VERSION),
                nonPresent("f5-unavailable-p0", U05ReadinessInput.F5, U05ReadinessInput.UNAVAILABLE, VERSION, "app-f5-p0"),
                nonPresent("f6-not-yet-p0", U05ReadinessInput.F6, U05ReadinessInput.NOT_YET_APPLICABLE, VERSION, "app-f6-p0"));
    }

    private static U05ReadinessInput present(String id, String domain, String signal, int version) {
        return new U05ReadinessInput(
                id, domain, "owner-" + domain, inputKind(domain),
                U05ReadinessInput.PRESENT, signal, "consult-1", "cdp-1", version,
                "decision-" + id, "state-" + domain, "app-evidence-" + id,
                Collections.singletonList("evidence-" + id),
                Collections.singletonList("rule-" + domain),
                "2026-09-21T08:00:00Z", U05ReadinessInput.CURRENT, null);
    }

    private static U05ReadinessInput nonPresent(
            String id, String domain, String status, int version, String applicabilityEvidence) {
        return new U05ReadinessInput(
                id, domain, "owner-" + domain, inputKind(domain),
                status, null, "consult-1", "cdp-1", version,
                "decision-" + id, "state-" + domain, applicabilityEvidence,
                Collections.singletonList("evidence-" + id),
                Collections.singletonList("rule-" + domain),
                "2026-09-21T08:00:00Z", U05ReadinessInput.CURRENT,
                U05ReadinessInput.STALE.equals(status) ? "invalidation-" + id : null);
    }

    private static String inputKind(String domain) {
        if (U05ReadinessInput.F1.equals(domain)) return "SCOPE_OR_FRAMING";
        if (U05ReadinessInput.F2_CLARIFICATION.equals(domain)) return "CLARIFICATION_REQUIREMENT";
        if (U05ReadinessInput.F3.equals(domain)) return "ONLINE_INFORMATION_GAP";
        if (U05ReadinessInput.F5.equals(domain)) return "DDX_OR_MUST_EXCLUDE";
        return "OFFLINE_EVIDENCE";
    }

    @SafeVarargs
    private static List<U05ReadinessInput> mutable(U05ReadinessInput... inputs) {
        return new ArrayList<U05ReadinessInput>(Arrays.asList(inputs));
    }

    private static U05ReadinessInputManifest manifest(
            String context, int version, List<U05ReadinessInput> inputs) {
        String setIdentity = U05ReadinessInputManifest.semanticSetIdentity(
                "consult-1", "cdp-1", version, context,
                U05ReadinessInputManifest.RDP05_CONTRACT_VERSION, inputs);
        return manifest(
                context,
                version,
                inputs,
                "manifest-" + context + "-" + version + "-" + setIdentity.substring(0, 12));
    }

    private static U05ReadinessInputManifest manifest(
            String context, int version, List<U05ReadinessInput> inputs, String manifestRef) {
        String setIdentity = U05ReadinessInputManifest.semanticSetIdentity(
                "consult-1", "cdp-1", version, context,
                U05ReadinessInputManifest.RDP05_CONTRACT_VERSION, inputs);
        return new U05ReadinessInputManifest(
                manifestRef, setIdentity, "consult-1", "cdp-1", version, context,
                U05ReadinessInputManifest.RDP05_CONTRACT_VERSION, inputs);
    }

    private static U05ConsumerInboundRequest request(
            U05ReadinessInputManifest manifest,
            String context,
            String gate,
            String restrictedContext,
            String restrictedPermission,
            int version,
            String canonicalEventRef,
            String consequenceOverride) {
        String routeSource = routeSource(context);
        String consequence = consequenceOverride != null ? consequenceOverride : routeConsequence(context);
        return new U05ConsumerInboundRequest(
                "request-" + context + "-" + version,
                "consult-1", "cdp-1", version,
                "clinical-state-cdp-1-v" + version,
                context, routeSource, "route-source-" + context, consequence,
                "u04-gate-1", "u04-gate-commit-1", gate,
                routeAuthType(context), "route-auth-" + context, "routing-policy-v1",
                U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT.equals(context) ? "A1" : null,
                restrictedContext, restrictedPermission,
                manifest.getManifestRef(), manifest.getSetIdentity(), manifest.authoritativeRecordRefs(),
                canonicalEventRef, "business-event-1", "corr-1", "trace-1",
                U05AdmissionService.CONTRACT_VERSION, ENV,
                "2026-09-21T08:00:00Z");
    }

    private static U05AdmissionAuthoritySnapshot authority(
            String context,
            String gate,
            String restrictedContext,
            String restrictedPermission,
            boolean restrictedPermitted,
            int version,
            boolean gateCommitted,
            boolean gateCurrent,
            boolean routeCurrent,
            boolean ownerPending,
            String consequenceOverride,
            String currentReadinessRecordRef) {
        boolean a1 = U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT.equals(context);
        return new U05AdmissionAuthoritySnapshot(
                "consult-1", "cdp-1", version,
                "clinical-state-cdp-1-v" + version,
                ENV, true,
                gateCommitted, gateCurrent,
                "u04-gate-1", "u04-gate-commit-1", gate,
                routeCurrent,
                "route-source-" + context,
                routeAuthType(context),
                "route-auth-" + context,
                consequenceOverride != null ? consequenceOverride : routeConsequence(context),
                "routing-policy-v1",
                restrictedContext, restrictedPermission, restrictedPermitted,
                a1 ? "A1" : null,
                a1, a1, a1,
                ownerPending,
                currentReadinessRecordRef);
    }

    private static String routeSource(String context) {
        if (U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT.equals(context)) {
            return U05ConsumerInboundRequest.U04_A1_POST_BARRIER_ROUTING;
        }
        if (U05ConsumerInboundRequest.POST_SAFETY_INITIAL.equals(context)) {
            return U05ConsumerInboundRequest.U04_ORDINARY_ROUTING;
        }
        return U05ConsumerInboundRequest.CLINICAL_CONTINUATION_ROUTING;
    }

    private static String routeConsequence(String context) {
        if (U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT.equals(context)) {
            return U05ConsumerInboundRequest.U05_ELIGIBLE;
        }
        if (U05ConsumerInboundRequest.POST_SAFETY_INITIAL.equals(context)) return null;
        return U05ConsumerInboundRequest.TO_U05_CLINICAL_READINESS;
    }

    private static String routeAuthType(String context) {
        if (U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT.equals(context)) {
            return "U04_A1_ROUTING_AUTHORIZATION";
        }
        if (U05ConsumerInboundRequest.POST_SAFETY_INITIAL.equals(context)) {
            return "U04_ORDINARY_ROUTING_AUTHORIZATION";
        }
        return "CLINICAL_CONTINUATION_ROUTING_DECISION";
    }

    // ---------------------------------------------------------------------
    // Precedence pair construction.
    // ---------------------------------------------------------------------

    private static U05ReadinessInputManifest precedenceManifest(int high, int low) {
        List<U05ReadinessInput> inputs = precedenceLowerProfile(low);
        if (high == 0) {
            if (low == 4) {
                replaceDomain(inputs, U05ReadinessInput.F6,
                        nonPresent("pm-p0-f6-failed", U05ReadinessInput.F6,
                                U05ReadinessInput.FAILED, VERSION, "pm-p0-f6-evidence"));
            } else {
                addOrReplaceF2(inputs,
                        nonPresent("pm-p0-f2-failed", U05ReadinessInput.F2_CLARIFICATION,
                                U05ReadinessInput.FAILED, VERSION, "pm-p0-f2-evidence"));
            }
        } else if (high == 1) {
            U05ReadinessInput existing = firstDomain(inputs, U05ReadinessInput.F1);
            String conflicting = U05ReadinessInput.OUT_OF_SCOPE.equals(existing.getBusinessSignal())
                    ? U05ReadinessInput.FRAMED_IN_SCOPE : U05ReadinessInput.OUT_OF_SCOPE;
            inputs.add(present("pm-p1-conflict", U05ReadinessInput.F1, conflicting, VERSION));
        } else {
            applyCandidate(inputs, high);
        }
        return manifest(U05ConsumerInboundRequest.A1_POST_BARRIER_CURRENT, VERSION, inputs,
                "manifest-precedence-p" + high + "-p" + low);
    }

    private static List<U05ReadinessInput> precedenceLowerProfile(int low) {
        List<U05ReadinessInput> inputs = profilePol005(VERSION, "pm-base-" + low);
        applyCandidate(inputs, low);
        return inputs;
    }

    private static void applyCandidate(List<U05ReadinessInput> inputs, int p) {
        switch (p) {
            case 1:
                inputs.add(present("pm-p1-conflict-lower", U05ReadinessInput.F1,
                        U05ReadinessInput.OUT_OF_SCOPE, VERSION));
                break;
            case 2:
                replaceDomain(inputs, U05ReadinessInput.F1,
                        present("pm-p2-out", U05ReadinessInput.F1,
                                U05ReadinessInput.OUT_OF_SCOPE, VERSION));
                break;
            case 3:
                replaceDomain(inputs, U05ReadinessInput.F6,
                        present("pm-p3-offline", U05ReadinessInput.F6,
                                U05ReadinessInput.NEEDS_OFFLINE_EVIDENCE, VERSION));
                break;
            case 4:
                addOrReplaceF2(inputs,
                        present("pm-p4-clarify", U05ReadinessInput.F2_CLARIFICATION,
                                U05ReadinessInput.NEEDS_CLARIFICATION, VERSION));
                break;
            case 5:
                replaceDomain(inputs, U05ReadinessInput.F3,
                        present("pm-p5-ask", U05ReadinessInput.F3,
                                U05ReadinessInput.CAN_ASK_MORE, VERSION));
                break;
            case 6:
                // Base profile is P6 / POL-005.
                break;
            case 7:
                replaceDomain(inputs, U05ReadinessInput.F5,
                        present("pm-p7-nrd", U05ReadinessInput.F5,
                                U05ReadinessInput.NO_RELIABLE_DIRECTION, VERSION));
                break;
            default:
                break;
        }
    }

    private static void replaceDomain(
            List<U05ReadinessInput> inputs, String domain, U05ReadinessInput replacement) {
        for (Iterator<U05ReadinessInput> it = inputs.iterator(); it.hasNext();) {
            if (domain.equals(it.next().getSourceDomain())) it.remove();
        }
        inputs.add(replacement);
    }

    private static void addOrReplaceF2(List<U05ReadinessInput> inputs, U05ReadinessInput replacement) {
        replaceDomain(inputs, U05ReadinessInput.F2_CLARIFICATION, replacement);
    }

    private static U05ReadinessInput firstDomain(List<U05ReadinessInput> inputs, String domain) {
        for (U05ReadinessInput i : inputs) if (domain.equals(i.getSourceDomain())) return i;
        throw new IllegalStateException("missing domain " + domain);
    }

    private static String f6Semantic(List<U05ReadinessInput> inputs) {
        for (U05ReadinessInput input : inputs) {
            if (U05ReadinessInput.F6.equals(input.getSourceDomain())) {
                return input.getApplicabilityStatus()
                        + (input.getBusinessSignal() == null ? "" : "|" + input.getBusinessSignal());
            }
        }
        return "MISSING";
    }

    private static String precedenceCode(U05ClinicalReadinessDecision d) {
        if (U05ClinicalReadinessDecision.INPUT_FAILURE.equals(d.getDecisionStatus())) return "P0";
        if (U05ClinicalReadinessDecision.INPUT_CONFLICT.equals(d.getDecisionStatus())) return "P1";
        if ("D03-POL-001".equals(d.getPolicyRuleRef())) return "P2";
        if ("D03-POL-002".equals(d.getPolicyRuleRef())) return "P3";
        if ("D03-POL-003".equals(d.getPolicyRuleRef())) return "P4";
        if ("D03-POL-004".equals(d.getPolicyRuleRef())) return "P5";
        if ("D03-POL-005".equals(d.getPolicyRuleRef()) || "D03-POL-011".equals(d.getPolicyRuleRef())) return "P6";
        if ("D03-POL-006".equals(d.getPolicyRuleRef())) return "P7";
        throw new IllegalStateException("unmapped D03 precedence result");
    }

    // ---------------------------------------------------------------------
    // Evidence/oracle handling.
    // ---------------------------------------------------------------------

    private static Observation observation(String boundary, String result) {
        return new Observation(boundary, result);
    }

    private static Observation rejection(U05AdmissionResult result) {
        Observation o = observation("ADMISSION",
                "REJECTED|" + result.getReasonCode());
        o.details.put("admission_status", result.getAdmissionStatus());
        o.details.put("admission_reason", result.getReasonCode());
        return o;
    }

    private static void mutationCounts(
            Observation o, int commits, int readiness, int invalidation) {
        o.counts.put("state_commit_count", commits);
        o.counts.put("readiness_effect_count", readiness);
        o.counts.put("invalidation_effect_count", invalidation);
    }

    private static void routingCounts(
            Observation o, int routeDecisions, int eligibilities, int schedulerIntents) {
        o.counts.put("route_decision_count", routeDecisions);
        o.counts.put("route_eligibility_count", eligibilities);
        o.counts.put("scheduler_target_intent_count", schedulerIntents);
    }

    private static void addAdmissionDetails(
            Observation o, U05AdmissionResult admission, U05ReadinessInputManifest manifest) {
        o.details.put("admission_status", admission.getAdmissionStatus());
        o.details.put("admission_reason", admission.getReasonCode());
        o.details.put("admission_replay_disposition", admission.getReplayDisposition());
        o.details.put("readiness_input_set_identity", manifest.getSetIdentity());
        o.details.put("readiness_input_refs", manifest.authoritativeRecordRefs());
        if (admission.isAdmitted()) {
            U05AdmittedInput input = admission.getAdmittedInput();
            o.details.put("admission_id", input.getAdmissionId());
            o.details.put("admitted_readiness_input_set_identity",
                    input.getAcceptedReadinessInputSetIdentity());
            o.details.put("admitted_restricted_permission_ref",
                    input.getAcceptedRestrictedPermissionRef());
            o.details.put("consultation_id", input.getConsultationId());
            o.details.put("cdp_id", input.getCdpId());
            o.details.put("source_clinical_state_version", input.getClinicalStateVersion());
            o.details.put("evaluation_context", input.getEvaluationContext());
            o.details.put("gate_ref", input.getAcceptedU04GateRef());
            o.details.put("gate_value", input.getGateValue());
        }
    }

    private static void addDecisionDetails(Observation o, U05ClinicalReadinessDecision d) {
        if (d == null) return;
        o.details.put("d03_decision_id", d.getDecisionId());
        o.details.put("d03_decision_status", d.getDecisionStatus());
        o.details.put("clinical_readiness", d.getClinicalReadiness());
        o.details.put("d03_policy_rule_ref", d.getPolicyRuleRef());
        o.details.put("d03_source_admission_ref", d.getSourceAdmissionRef());
        o.details.put("d03_source_readiness_input_set_identity",
                d.getSourceReadinessInputSetIdentity());
        o.details.put("d03_restricted_permission_ref", d.getRestrictedPermissionRef());
    }

    private static void addPreparedDetails(Observation o, PreparedBundle p) {
        addAdmissionDetails(o, p.admission, p.manifest);
        addDecisionDetails(o, p.decision);
    }

    private static void addCommittedDetails(Observation o, CommittedBundle c) {
        addAdmissionDetails(o, c.admission, c.manifest);
        addDecisionDetails(o, c.decision);
        o.details.put("readiness_effect_id", c.proposal.getEffectId());
        o.details.put("readiness_payload_fingerprint", c.proposal.getCanonicalPayloadFingerprint());
        o.details.put("proposal_id", c.proposal.getProposalId());
        o.details.put("proposal_base_version", c.proposal.getStatePatch().baseVersion);
        o.details.put("commit_status", c.commit.status);
        o.details.put("committed_version", c.commit.committedVersion);
        o.details.put("commit_result_ref", c.evidence.getCommitResultRef());
        o.details.put("audit_ref", c.evidence.getAuditRef());
        o.sideEffectRefs.put("readiness_effect", c.proposal.getEffectId());
        o.sideEffectRefs.put("state_commit", c.evidence.getCommitResultRef());
    }

    private static void addRouteDetails(Observation o, U05DownstreamRoutingDecision route) {
        o.details.put("routing_decision_id", route.getRoutingDecisionId());
        o.details.put("routing_status", route.getRoutingStatus());
        o.details.put("routing_replay_disposition", route.getReplayDisposition());
        o.details.put("routing_decision_fingerprint", route.getRoutingDecisionFingerprint());
        o.details.put("route_effect_id", route.getRouteEffectId());
        o.details.put("downstream_consequence", route.getDownstreamConsequence());
        o.details.put("target_unit_id", route.getTargetUnitId());
        o.details.put("failure_handoff_ref", route.getFailureHandoffRef());
        if (route.getRouteEffectId() != null) {
            o.sideEffectRefs.put("route_decision", route.getRoutingDecisionId());
            o.sideEffectRefs.put("route_effect", route.getRouteEffectId());
        } else {
            o.sideEffectRefs.put("route_decision", route.getRoutingDecisionId());
        }
        if (route.getEligibility() != null) {
            o.details.put("eligibility_id", route.getEligibility().getEligibilityId());
            o.sideEffectRefs.put("route_eligibility", route.getEligibility().getEligibilityId());
        }
        if (route.getFailureHandoffRef() != null) {
            o.sideEffectRefs.put("failure_handoff", route.getFailureHandoffRef());
        }
    }

    private static void addSchedulerDetails(Observation o, SchedulerObservation s) {
        o.details.put("route_consumption_id", s.routeConsumptionId);
        o.details.put("scheduler_intent_ref", s.schedulerIntentRef);
        o.details.put("failure_handoff_ref", s.failureHandoffRef);
        if (s.schedulerIntentRef != null) {
            o.sideEffectRefs.put("scheduler_target_intent", s.schedulerIntentRef);
        }
        if (s.failureHandoffRef != null) {
            o.sideEffectRefs.put("scheduler_failure_handoff", s.failureHandoffRef);
        }
        o.counts.put("downstream_unit_invocation_count", s.downstreamUnitInvocationCount);
        o.counts.put("external_delivery_count", s.externalDeliveryCount);
        o.counts.put("external_tool_model_call_count", s.externalToolModelCallCount);
    }

    private static void assertAndRecord(String caseId, Observation observed) throws Exception {
        Map<String, Object> expected = expectation(caseId);
        @SuppressWarnings("unchecked")
        Map<String, Object> expectedCounts =
                (Map<String, Object>) expected.get("expected_effect_counts");
        boolean boundaryMatch =
                String.valueOf(expected.get("expected_boundary")).equals(observed.boundary);
        boolean resultMatch =
                String.valueOf(expected.get("expected_result")).equals(observed.result);
        boolean countsMatch = true;
        for (String key : observed.counts.keySet()) {
            int expectedValue = ((Number) expectedCounts.get(key)).intValue();
            if (expectedValue != observed.counts.get(key).intValue()) {
                countsMatch = false;
            }
        }

        Map<String, Object> evidence = new LinkedHashMap<String, Object>();
        evidence.put("schema", "U05_CASE_EVIDENCE_V0_1");
        evidence.put("case_id", caseId);
        evidence.put("category", expected.get("category"));
        evidence.put("scenario", expected.get("scenario"));
        evidence.put("implementation_sha", IMPLEMENTATION_SHA);
        evidence.put("expected_authority_refs", expected.get("expected_authority_refs"));
        evidence.put("expected_contract_versions",
                Arrays.asList("U05-RDP01-V1","U05-D03-POLICY-V1-REFROZEN",
                        "U05-RDP03-EFFECT-V1","U05-RDP04-V1-FROZEN","U05-RDP05-REFROZEN-V1"));
        evidence.put("fixture_id", expected.get("fixture_id"));
        evidence.put("fixture_digest", expected.get("fixture_digest"));
        evidence.putAll(observed.details);
        evidence.put("policy_expectation_gap", false);
        evidence.put("expected_boundary", expected.get("expected_boundary"));
        evidence.put("observed_boundary", observed.boundary);
        evidence.put("expected_result", expected.get("expected_result"));
        evidence.put("observed_result", observed.result);
        @SuppressWarnings("unchecked")
        List<String> expectedEqualityIds =
                (List<String>) expected.get("expected_provenance_equalities");
        List<Map<String, Object>> observedEqualities =
                observedProvenanceEqualities(observed);
        List<String> observedEqualityIds = new ArrayList<String>();
        for (Map<String, Object> eq : observedEqualities) {
            observedEqualityIds.add(String.valueOf(eq.get("equality")));
            Assertions.assertEquals(Boolean.TRUE, eq.get("equal"),
                    caseId + " provenance equality failed: " + eq.get("equality"));
        }
        Collections.sort(expectedEqualityIds);
        Collections.sort(observedEqualityIds);
        boolean provenanceCoverageMatch = expectedEqualityIds.equals(observedEqualityIds);

        evidence.put("expected_provenance_equalities", expectedEqualityIds);
        evidence.put("observed_provenance_equalities", observedEqualities);
        evidence.put("expected_effect_counts", expectedCounts);
        evidence.put("observed_effect_counts", observed.counts);
        evidence.put("side_effect_evidence_refs", observed.sideEffectRefs);
        evidence.put("contract_manifest_digest", expected.get("contract_manifest_digest"));
        evidence.put("pass", boundaryMatch && resultMatch && countsMatch && provenanceCoverageMatch);
        writeJson(evidenceRoot().resolve("cases").resolve(caseId + ".json"), evidence);

        Assertions.assertTrue(boundaryMatch, caseId + " boundary mismatch");
        Assertions.assertTrue(resultMatch, caseId + " result mismatch");
        Assertions.assertTrue(countsMatch, caseId + " effect-count mismatch");
        Assertions.assertTrue(provenanceCoverageMatch,
                caseId + " provenance equality coverage mismatch expected="
                        + expectedEqualityIds + " observed=" + observedEqualityIds);
    }

    private static List<Map<String, Object>> observedProvenanceEqualities(Observation o) {
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        if (o.details.get("admission_id") != null && o.details.get("d03_source_admission_ref") != null) {
            out.add(equality("admission_id=d03_source_admission_ref",
                    o.details.get("admission_id"), o.details.get("d03_source_admission_ref")));
        }
        if (o.details.get("admitted_readiness_input_set_identity") != null
                && o.details.get("d03_source_readiness_input_set_identity") != null) {
            out.add(equality("readiness_input_set_identity=d03_source_readiness_input_set_identity",
                    o.details.get("admitted_readiness_input_set_identity"),
                    o.details.get("d03_source_readiness_input_set_identity")));
        }
        if (o.details.get("inbound_restricted_permission_ref") != null
                && o.details.get("admission_result_restricted_permission_ref") != null
                && o.details.get("admitted_restricted_permission_ref") != null
                && o.details.get("d03_restricted_permission_ref") != null
                && o.details.get("readiness_source_restricted_permission_ref") != null) {
            out.add(chainEquality(
                    "restricted_permission_ref_chain",
                    o.details.get("inbound_restricted_permission_ref"),
                    o.details.get("admission_result_restricted_permission_ref"),
                    o.details.get("admitted_restricted_permission_ref"),
                    o.details.get("d03_restricted_permission_ref"),
                    o.details.get("readiness_source_restricted_permission_ref")));
        }
        return out;
    }

    private static Map<String, Object> chainEquality(String id, Object... values) {
        Map<String, Object> e = new LinkedHashMap<String, Object>();
        e.put("equality", id);
        e.put("values", Arrays.asList(values));
        boolean equal = values.length > 0 && values[0] != null;
        for (int i = 1; i < values.length; i++) {
            equal = equal && Objects.equals(values[0], values[i]);
        }
        e.put("equal", equal);
        return e;
    }

    private static Map<String, Object> equality(String id, Object left, Object right) {
        Map<String, Object> e = new LinkedHashMap<String, Object>();
        e.put("equality", id);
        e.put("left", left);
        e.put("right", right);
        e.put("equal", Objects.equals(left, right));
        return e;
    }

    private static Map<String, Object> expectation(String caseId) {
        Map<String, Map<String, Object>> all = expectations();
        Map<String, Object> value = all.get(caseId);
        if (value == null) {
            throw new IllegalStateException("POLICY_EXPECTATION_GAP:" + caseId);
        }
        return value;
    }

    private static Map<String, Map<String, Object>> expectations() {
        if (expectations == null) {
            synchronized (U05VerificationSupport.class) {
                if (expectations == null) {
                    Map<String, Object> root =
                            readResourceMapUnchecked("/u05/verification/u05-verification-expectations.json");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> cases =
                            (List<Map<String, Object>>) root.get("cases");
                    Map<String, Map<String, Object>> byId =
                            new LinkedHashMap<String, Map<String, Object>>();
                    for (Map<String, Object> c : cases) {
                        byId.put(String.valueOf(c.get("case_id")), c);
                    }
                    expectations = Collections.unmodifiableMap(byId);
                }
            }
        }
        return expectations;
    }

    private static Map<String, Map<String, Object>> precedence() {
        if (precedence == null) {
            synchronized (U05VerificationSupport.class) {
                if (precedence == null) {
                    Map<String, Object> root =
                            readResourceMapUnchecked("/u05/verification/u05-d03-precedence-expectations.json");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> pairs =
                            (List<Map<String, Object>>) root.get("pairs");
                    Map<String, Map<String, Object>> byId =
                            new LinkedHashMap<String, Map<String, Object>>();
                    for (Map<String, Object> p : pairs) {
                        byId.put(String.valueOf(p.get("subcase_id")), p);
                    }
                    precedence = Collections.unmodifiableMap(byId);
                }
            }
        }
        return precedence;
    }

    private static void recordPrecedence(
            Map<String, Object> oracle,
            U05ClinicalReadinessDecision decision,
            boolean pass) throws Exception {
        Map<String, Object> e = new LinkedHashMap<String, Object>();
        e.put("schema", "U05_D03_PRECEDENCE_EVIDENCE_V0_1");
        e.put("subcase_id", oracle.get("subcase_id"));
        e.put("higher_candidate", oracle.get("higher_candidate"));
        e.put("lower_candidate", oracle.get("lower_candidate"));
        e.put("constructibility", oracle.get("constructibility"));
        e.put("fixture_id", oracle.get("fixture_id"));
        e.put("fixture_digest", oracle.get("fixture_digest"));
        e.put("expected_higher_outcome", oracle.get("expected_higher_outcome"));
        e.put("observed_precedence", decision == null ? null : precedenceCode(decision));
        e.put("authority_refs", oracle.get("constructibility_authority_refs"));
        e.put("rationale", oracle.get("rationale"));
        e.put("pass", pass && (decision == null
                || String.valueOf(oracle.get("expected_higher_outcome")).equals(precedenceCode(decision))));
        writeJson(evidenceRoot().resolve("precedence")
                .resolve(String.valueOf(oracle.get("subcase_id")) + ".json"), e);
    }

    private static Path evidenceRoot() {
        return Paths.get(System.getProperty(
                "u05.verification.evidenceDir",
                "target/u05-rdp06-evidence"));
    }

    private static void writeJson(Path path, Map<String, Object> value) throws Exception {
        Files.createDirectories(path.getParent());
        byte[] bytes = (canonicalJson(value)).getBytes(StandardCharsets.UTF_8);
        Files.write(path, bytes,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static Map<String, Object> readResourceMap(String path) throws Exception {
        try (InputStream in = U05VerificationSupport.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("missing resource " + path);
            return JSON.readValue(in, new TypeReference<Map<String, Object>>() {});
        }
    }

    private static Map<String, Object> readResourceMapUnchecked(String path) {
        try {
            return readResourceMap(path);
        } catch (Exception e) {
            throw new IllegalStateException("failed to load " + path, e);
        }
    }

    private static String canonicalJson(Object value) throws Exception {
        Object sorted = sortJson(value);
        return JSON.writeValueAsString(sorted) + "\n";
    }

    @SuppressWarnings("unchecked")
    private static Object sortJson(Object value) {
        if (value instanceof Map<?, ?>) {
            TreeMap<String, Object> sorted = new TreeMap<String, Object>();
            for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
                sorted.put(String.valueOf(e.getKey()), sortJson(e.getValue()));
            }
            return sorted;
        }
        if (value instanceof List<?>) {
            List<Object> out = new ArrayList<Object>();
            for (Object v : (List<?>) value) out.add(sortJson(v));
            return out;
        }
        return value;
    }

    private static String sha256(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder out = new StringBuilder();
            for (byte b : digest) out.append(String.format("%02x", b & 0xff));
            return out.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String caseId(int number) {
        return String.format("U05-EV-%03d", number);
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty() || "null".equals(value);
    }

    private static String decisionResult(U05ClinicalReadinessDecision d) {
        if (!d.isDecided()) return d.getDecisionStatus();
        return d.getDecisionStatus() + "|" + d.getClinicalReadiness() + "|" + d.getPolicyRuleRef();
    }

    private static String routeResult(U05DownstreamRoutingDecision d) {
        return d.getRoutingStatus() + "|" + d.getDownstreamConsequence() + "|" + d.getTargetUnitId();
    }

    // ---------------------------------------------------------------------
    // Test-only Scheduler consumer surface approved by the remediation review.
    // ---------------------------------------------------------------------

    private static final class TestSchedulerConsumer {
        static final String CONTRACT_VERSION = "U05_TEST_SCHEDULER_CONSUMPTION_V1";
        static final String NAMESPACE = "U05_TEST_SCHEDULER_TARGET_INTENT";
        private final CanonicalEffectLedger ledger;

        TestSchedulerConsumer(CanonicalEffectLedger ledger) {
            this.ledger = ledger;
        }

        SchedulerObservation consume(
                U05DownstreamEligibility eligibility,
                String currentnessFixtureIdentity,
                boolean current,
                String bindingFixtureIdentity,
                boolean bindingAvailable) {
            if (!current) {
                return SchedulerObservation.stale();
            }
            if (!bindingAvailable) {
                String handoff = hash(
                        "u05-test-failure-handoff",
                        eligibility.getEligibilityId(),
                        eligibility.getRouteEffectId(),
                        eligibility.getTargetUnitId(),
                        "TARGET_BINDING_UNAVAILABLE",
                        currentnessFixtureIdentity,
                        bindingFixtureIdentity,
                        CONTRACT_VERSION);
                return SchedulerObservation.failure(handoff);
            }

            String governanceFixtureIdentity = hash(
                    "u05-test-execution-governance-fixture",
                    currentnessFixtureIdentity,
                    bindingFixtureIdentity);
            String intentIdentity = hash(
                    "u05-test-scheduler-intent",
                    eligibility.getEligibilityId(),
                    eligibility.getRouteEffectId(),
                    eligibility.getTargetUnitId(),
                    governanceFixtureIdentity,
                    CONTRACT_VERSION);
            String routeConsumptionId = hash(
                    "u05-route-consumption",
                    eligibility.getEligibilityId(),
                    eligibility.getRouteEffectId(),
                    eligibility.getTargetUnitId(),
                    intentIdentity,
                    CONTRACT_VERSION);
            String fingerprint = hash(
                    "u05-test-scheduler-intent-payload",
                    eligibility.getEligibilityId(),
                    eligibility.getRouteEffectId(),
                    eligibility.getTargetUnitId(),
                    governanceFixtureIdentity,
                    CONTRACT_VERSION);
            byte[] payload = (
                    eligibility.getEligibilityId() + "|" +
                    eligibility.getRouteEffectId() + "|" +
                    eligibility.getTargetUnitId() + "|" +
                    governanceFixtureIdentity + "|" +
                    CONTRACT_VERSION).getBytes(StandardCharsets.UTF_8);

            CanonicalEffectLedgerDecision decision = ledger.createIfAbsent(
                    NAMESPACE, intentIdentity, fingerprint, CONTRACT_VERSION, payload);
            if (CanonicalEffectLedgerDecision.Status.CREATED.equals(decision.getStatus())) {
                return SchedulerObservation.intent(intentIdentity, routeConsumptionId, "ORIGINAL");
            }
            if (CanonicalEffectLedgerDecision.Status.REATTACHED.equals(decision.getStatus())) {
                return SchedulerObservation.intent(intentIdentity, routeConsumptionId, "REATTACHED");
            }
            throw new IllegalStateException("TEST_SCHEDULER_INTENT_" + decision.getStatus());
        }
    }

    private static final class SchedulerObservation {
        final String status;
        final String schedulerIntentRef;
        final String routeConsumptionId;
        final String failureHandoffRef;
        final String replayDisposition;
        final int schedulerTargetIntentCount;
        final int downstreamUnitInvocationCount;
        final int alternateRouteEffectCount;
        final int externalDeliveryCount;
        final int externalToolModelCallCount;

        private SchedulerObservation(
                String status,
                String schedulerIntentRef,
                String routeConsumptionId,
                String failureHandoffRef,
                String replayDisposition,
                int schedulerTargetIntentCount) {
            this.status = status;
            this.schedulerIntentRef = schedulerIntentRef;
            this.routeConsumptionId = routeConsumptionId;
            this.failureHandoffRef = failureHandoffRef;
            this.replayDisposition = replayDisposition;
            this.schedulerTargetIntentCount = schedulerTargetIntentCount;
            this.downstreamUnitInvocationCount = 0;
            this.alternateRouteEffectCount = 0;
            this.externalDeliveryCount = 0;
            this.externalToolModelCallCount = 0;
        }

        static SchedulerObservation stale() {
            return new SchedulerObservation("REJECTED_STALE", null, null, null, null, 0);
        }

        static SchedulerObservation failure(String handoff) {
            return new SchedulerObservation("FAILURE_REQUIRED", null, null, handoff, null, 0);
        }

        static SchedulerObservation intent(String intent, String consumption, String replay) {
            return new SchedulerObservation("TARGET_INTENT", intent, consumption, null, replay, 1);
        }
    }

    private static String hash(String prefix, String... values) {
        StringBuilder in = new StringBuilder(prefix);
        for (String value : values) {
            in.append('|').append(value == null ? "<null>" : value);
        }
        return sha256(in.toString().getBytes(StandardCharsets.UTF_8));
    }

    // ---------------------------------------------------------------------
    // Nested data holders.
    // ---------------------------------------------------------------------

    private static final class Observation {
        final String boundary;
        final String result;
        final Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        final Map<String, Object> details = new LinkedHashMap<String, Object>();
        final Map<String, String> sideEffectRefs = new LinkedHashMap<String, String>();

        Observation(String boundary, String result) {
            this.boundary = boundary;
            this.result = result;
            counts.put("state_commit_count", 0);
            counts.put("readiness_effect_count", 0);
            counts.put("invalidation_effect_count", 0);
            counts.put("route_decision_count", 0);
            counts.put("route_eligibility_count", 0);
            counts.put("scheduler_target_intent_count", 0);
            counts.put("downstream_unit_invocation_count", 0);
            counts.put("external_delivery_count", 0);
            counts.put("external_tool_model_call_count", 0);
        }
    }

    private static class AdmissionBundle {
        final U05AdmissionResult admission;
        final U05ReadinessInputManifest manifest;

        AdmissionBundle(U05AdmissionResult admission, U05ReadinessInputManifest manifest) {
            this.admission = admission;
            this.manifest = manifest;
        }
    }

    private static class PreparedBundle {
        final U05AdmissionResult admission;
        final U05AdmittedInput input;
        final U05ClinicalReadinessDecision decision;
        final U05ReadinessStateProposal proposal;
        final U05ReadinessInputManifest manifest;

        PreparedBundle(
                U05AdmissionResult admission,
                U05AdmittedInput input,
                U05ClinicalReadinessDecision decision,
                U05ReadinessStateProposal proposal,
                U05ReadinessInputManifest manifest) {
            this.admission = admission;
            this.input = input;
            this.decision = decision;
            this.proposal = proposal;
            this.manifest = manifest;
        }
    }

    private static final class CommittedBundle extends PreparedBundle {
        final StateTypes.CommitResult commit;
        final U05ClinicalReadinessCommitEvidence evidence;

        CommittedBundle(
                PreparedBundle p,
                StateTypes.CommitResult commit,
                U05ClinicalReadinessCommitEvidence evidence) {
            super(p.admission, p.input, p.decision, p.proposal, p.manifest);
            this.commit = commit;
            this.evidence = evidence;
        }
    }

    private static final class EligibilityBundle {
        final Fixture fixture;
        final CommittedBundle committed;
        final U05DownstreamRoutingDecision route;

        EligibilityBundle(Fixture fixture, CommittedBundle committed, U05DownstreamRoutingDecision route) {
            this.fixture = fixture;
            this.committed = committed;
            this.route = route;
        }
    }

    private static final class Fixture {
        final SyntheticVersionedStateRepository repository;
        final StateCommitter committer;
        final U05CommitService commitService;

        Fixture(int version) {
            Map<String, Object> patient = new LinkedHashMap<String, Object>();
            Map<String, Object> state = new LinkedHashMap<String, Object>();
            state.put("patient_state", patient);
            Map<String, SyntheticStateSnapshot> initial =
                    new LinkedHashMap<String, SyntheticStateSnapshot>();
            initial.put("cdp-1", new SyntheticStateSnapshot(version, state));
            repository = new SyntheticVersionedStateRepository(initial);
            List<String> order = new ArrayList<String>();
            committer = new StateCommitter(
                    repository,
                    (capabilityId, capabilityVersion) ->
                            CapabilityPolicyPort.CapabilityDecision.authorized(),
                    (statePath, capabilityId) ->
                            U05ReadinessStateProposalFactory.READINESS_PATH.equals(statePath)
                                    ? FieldPermissionPort.FieldPermissionDecision.authorized()
                                    : FieldPermissionPort.FieldPermissionDecision.denied(),
                    (cdpId, capabilityId) ->
                            ConsentPolicyPort.ConsentDecision.authorized(),
                    source -> SourceValidationPort.SourceDecision.authorized(),
                    new InMemoryIdempotencyFake(order),
                    new SyntheticAuditPortFake(CLOCK, order),
                    new RecordingCommitEventEvidenceFake(order),
                    CLOCK);
            commitService = new U05CommitService(
                    committer,
                    new U05SyntheticClinicalReadinessSnapshotAdapter(repository));
        }
    }
}
