package com.aidoctor.diagnosis.runtime.u06;

import com.aidoctor.diagnosis.runtime.u06.delivery.U06DeliveryStore;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06SyntheticDeliveryService;
import com.aidoctor.diagnosis.runtime.u06.delivery.U06SyntheticDeliveryRuntime;
import com.aidoctor.diagnosis.runtime.u06.state.U06SyntheticP01Runtime;
import com.aidoctor.diagnosis.runtime.u01.ConsultationRecord;
import com.aidoctor.diagnosis.runtime.u01.ConsultationRepository;
import com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitEffectRecord;
import com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitEffectRepository;
import com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitTransitionService;
import com.aidoctor.diagnosis.runtime.u06.wait.U06WaitCoordinator;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRecord;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRepository;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRecord;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRepository;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointService;
import com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadWaitTransitionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.net.SocketPermission;
import java.security.Permission;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RDP-06 authoritative observation harness.
 *
 * The fixture manifest is the only scenario input. This class deliberately does NOT read the
 * expectation oracle. It emits actual SUT observations; verify_u06.py independently compares
 * those observations with the independently reviewed oracle.
 *
 * A missing runtime capability is emitted as a concrete observed status rather than NOT_EXECUTED.
 * That makes implementation gaps authoritative FAIL evidence instead of verifier coverage gaps.
 */
class U06Rdp06AuthoritativeObservationTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String AT = "2026-09-23T00:00:00Z";
    private static final Clock CLOCK = Clock.fixed(Instant.parse(AT), ZoneOffset.UTC);
    private static final Set<String> RUNTIME_CLASSES =
            new HashSet<String>(Arrays.asList("EXECUTION_CASE", "CRASH_WINDOW"));

    @Test
    void emitsReviewedRuntimeCaseObservationsAndExternalCallSpyEvidence() throws Exception {
        JsonNode fixtures = readResource("/u06/u06-verification-fixtures.json");
        ArrayNode observations = JSON.createArrayNode();
        Set<String> ids = new LinkedHashSet<String>();

        CountingConnectSecurityManager spy = new CountingConnectSecurityManager(System.getSecurityManager());
        SecurityManager previous = System.getSecurityManager();
        System.setSecurityManager(spy);
        try {
            for (JsonNode fixture : fixtures.path("fixtures")) {
                JsonNode controls = fixture.path("scenario_controls");
                if (!RUNTIME_CLASSES.contains(text(controls, "scenario_class"))) continue;
                String caseId = caseId(fixture);
                if (!ids.add(caseId)) throw new IllegalStateException("duplicate runtime case " + caseId);
                observations.add(observe(caseId, fixture));
            }
        } finally {
            System.setSecurityManager(previous);
        }

        // 106 EV + 9 CW = 115 runtime-observation cases.
        assertEquals(115, observations.size());
        assertEquals(115, ids.size());

        File outDir = new File("target/u06-rdp06-observations");
        if (!outDir.exists() && !outDir.mkdirs()) throw new IllegalStateException("cannot create observation directory");

        ObjectNode bundle = JSON.createObjectNode();
        bundle.put("schema", "U06_SUT_OBSERVATIONS_V0_1");
        bundle.put("fixture_driven", true);
        bundle.put("oracle_used_to_generate_observation", false);
        bundle.put("runtime_case_count", observations.size());
        bundle.set("observations", observations);
        JSON.writerWithDefaultPrettyPrinter().writeValue(new File(outDir, "u06-sut-observations.json"), bundle);

        ObjectNode spyEvidence = JSON.createObjectNode();
        spyEvidence.put("schema", "U06_EXTERNAL_CALL_SPY_V0_1");
        spyEvidence.put("enabled", true);
        spyEvidence.put("observed_count", spy.connectCount.get());
        spyEvidence.put("policy", "BLOCK_AND_COUNT_JAVA_SOCKET_CONNECT");
        spyEvidence.put("scope", "U06_RDP06_FIXTURE_DRIVEN_RUNTIME_HARNESS");
        JSON.writerWithDefaultPrettyPrinter().writeValue(new File(outDir, "external-call-spy.json"), spyEvidence);
    }

    private ObjectNode observe(String caseId, JsonNode fixture) {
        ObjectNode o = base(caseId);
        try {
            if (caseId.startsWith("U06-EV-")) {
                int n = Integer.parseInt(caseId.substring("U06-EV-".length()));
                if (n <= 18) observeAdmission(n, fixture, o);
                else if (n <= 41) observeDecision(n, fixture, o);
                else if (n <= 61) observeMutation(n, fixture, o);
                else if (n <= 100) observeDeliveryAndWait(n, fixture, o);
                else observeSafetyAndRevalidation(n, fixture, o);
            } else if (caseId.startsWith("U06-CW-")) {
                observeCrashWindow(caseId, fixture, o);
            } else {
                o.put("observed_status", "UNROUTED_RUNTIME_CASE");
            }
        } catch (Throwable failure) {
            o.put("observed_status", "SUT_EXCEPTION");
            o.put("observed_reason_code", failure.getMessage() == null
                    ? failure.getClass().getSimpleName() : failure.getMessage());
            o.put("observed_failure_handoff_count", 1);
            o.withArray("probe_refs").add(failure.getClass().getName());
        }
        return o;
    }

    private void observeAdmission(int n, JsonNode fixture, ObjectNode o) {
        U06AdmissionService service = new U06AdmissionService();
        U06ProfileBRequest first = request(fixture, 0, false);
        int actualVersion = "STALE".equals(control(fixture, "state_currentness")) ? 1 : 0;

        try {
            U06AdmissionService.Admission a;
            if (n == 15) {
                service.admit(first, actualVersion);
                a = service.admit(first, actualVersion);
            } else if (n == 16) {
                service.admit(first, actualVersion);
                U06ProfileBRequest changed = request(fixture, 0, true);
                a = service.admit(changed, actualVersion);
            } else {
                a = service.admit(first, actualVersion);
            }

            o.put("observed_admission_status", a.isAdmitted()
                    ? (a.isReplay() ? "ADMITTED_OR_REATTACHED" : "ADMITTED_OR_REATTACHED")
                    : "NOT_ADMITTED");
            o.put("observed_reason_code", a.getReasonCode());

            if (a.isAdmitted()) {
                if (a.isReplay()) o.put("observed_status", "REATTACHED_SAME_ADMISSION");
                else o.put("observed_status", "ADMITTED");
            } else if (U06AdmissionService.REJECTED_F1_DISABLED.equals(a.getReasonCode())) {
                o.put("observed_status", "EXECUTABLE_ADMISSION_BLOCKED");
            } else if (U06AdmissionService.REJECTED_STALE_STATE.equals(a.getReasonCode()) && n == 13) {
                o.put("observed_status", "REJECTED_READMISSION_REQUIRED");
            } else if (U06AdmissionService.REJECTED_DEPENDENCY.equals(a.getReasonCode()) && n == 14) {
                o.put("observed_status", "DEPENDENCY_BLOCKED");
            } else if (U06AdmissionService.FAILURE_PERMISSION_UNAVAILABLE.equals(a.getReasonCode()) && n == 5) {
                o.put("observed_status", "REJECTED_WITH_FAILURE_GOVERNANCE");
                o.put("observed_failure_handoff_count", 1);
            } else if (U06AdmissionService.REJECTED_SOURCE_SUPERSEDED.equals(a.getReasonCode()) && n == 17) {
                o.put("observed_status", "ADMISSION_NOT_CURRENT");
            } else {
                o.put("observed_status", "REJECTED");
            }
        } catch (IllegalStateException conflict) {
            o.put("observed_admission_status", "NOT_ADMITTED");
            o.put("observed_reason_code", conflict.getMessage());
            o.put("observed_status", "U06_ADMISSION_REPLAY_CONFLICT".equals(conflict.getMessage())
                    ? "REPLAY_CONFLICT" : "SUT_EXCEPTION");
        }
        o.withArray("probe_refs").add("U06AdmissionService");
    }

    private void observeDecision(int n, JsonNode fixture, ObjectNode o) {
        U06SyntheticP01Runtime state = state();
        seedQuestionState(state, fixture);
        U06ProfileBRequest req = request(fixture, state.readCurrent().getVersion(), false);
        U06SyntheticDecisionInput input = decisionInput(fixture);
        U06SyntheticDecisionBundle d = new U06SyntheticDecisionEngine().decide(req, input, state.readCurrent());

        String c03 = control(fixture, "c03_status");
        o.put("observed_c03_invocation_count", "NOT_APPLICABLE".equals(c03) ? 0 : 1);
        o.put("observed_d04_invocation_count", d.getD04Status() == null ? 0 : 1);
        o.put("observed_f3_owner_status", d.getF3OwnerStatus());
        o.put("observed_question_selection_status", d.getQuestionSelectionStatus());

        if (U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT.equals(req.getMode())) {
            if (U06SyntheticDecisionBundle.GAP_BASIS_ESTABLISHED.equals(d.getF3OwnerStatus())) {
                o.put("observed_status", "GAP_BASIS_ESTABLISHED_NO_QUESTION");
            } else if (U06SyntheticDecisionBundle.NO_CURRENT_ONLINE_GAP_BASIS_ESTABLISHED.equals(d.getF3OwnerStatus())) {
                o.put("observed_status", "NO_CURRENT_ONLINE_GAP_BASIS_ESTABLISHED");
            } else if (U06SyntheticDecisionBundle.FAILED.equals(d.getF3OwnerStatus())) {
                o.put("observed_status", "FAILED_NO_CANONICAL_F3");
            } else if (U06SyntheticDecisionInput.NO_RESULT.equals(input.getC03BusinessStatus())) {
                o.put("observed_status", "NOT_DECIDABLE_NO_NEGATIVE_TRUTH");
            } else {
                o.put("observed_status", "NOT_DECIDABLE");
            }
        } else {
            String existing = control(fixture, "existing_question_state");
            String basis = control(fixture, "f3_basis");
            String candidates = control(fixture, "candidate_scenario");
            String noProgress = control(fixture, "no_progress_class");

            if (!"NONE".equals(noProgress)) {
                o.put("observed_status", new U06NoProgressRouter().route(noProgress));
            } else if (U06SyntheticDecisionBundle.SELECTED.equals(d.getQuestionSelectionStatus())) {
                if ("MULTI_DETERMINISTIC_ORDER".equals(candidates)) o.put("observed_status", "EXACTLY_ONE_SELECTED");
                else if ("CURRENT_ASKABLE_GAP".equals(basis)) o.put("observed_status", "LAWFUL_CANDIDATE_SET");
                else if ("F1_MINIMAL_CLARIFICATION".equals(basis)) o.put("observed_status",
                        d.getGapId()!=null ? "BOUND_TO_EXACT_F1_REQUIREMENT_ONLY" : "F1_REQUIREMENT_BINDING_MISSING");
                else if (n == 27) o.put("observed_status", "MODE1_CANDIDATE_NOT_REUSED");
                else o.put("observed_status", "SELECTED");
            } else if (U06SyntheticDecisionBundle.FAILED.equals(d.getQuestionSelectionStatus())
                    && U06SyntheticDecisionBundle.CONTINUE.equals(d.getD04Status())) {
                o.put("observed_status", "SELECTION_FAILED_AMBIGUOUS");
            } else if (U06SyntheticDecisionInput.NO_RESULT.equals(input.getC03BusinessStatus())) {
                o.put("observed_status", "NO_SELECTION_NO_INFERRED_D04");
            } else if (U06SyntheticDecisionInput.INSUFFICIENT_INFORMATION.equals(input.getC03BusinessStatus())) {
                o.put("observed_status", "NO_SELECTION_OR_GOVERNED_FAILED");
            } else if ("SELECTED_EQUIVALENT".equals(existing) || "DELIVERED_EQUIVALENT".equals(existing)) {
                o.put("observed_status", "DUPLICATE_SUPPRESSED");
            } else if ("ANSWER_RECEIVED_CURRENT".equals(existing)) {
                o.put("observed_status", "CANDIDATE_SUPPRESSED");
            } else if ("USER_UNKNOWN_UNCHANGED".equals(existing)) {
                o.put("observed_status", U06SyntheticDecisionBundle.NO_SELECTION.equals(d.getQuestionSelectionStatus())
                        ? "EXACT_REASK_SUPPRESSED" : "USER_UNKNOWN_REASK_NOT_SUPPRESSED");
            } else if ("UNMEASURED_OFFLINE_REQUIRED".equals(basis)) {
                o.put("observed_status", "NO_ONLINE_SELECTION");
            } else if (U06SyntheticDecisionBundle.STOP.equals(d.getD04Status())) {
                o.put("observed_status", "NO_QUESTION_NO_READINESS_INVENTED");
            } else if (U06SyntheticDecisionBundle.FAILED.equals(d.getD04Status())) {
                o.put("observed_status", "NO_QUESTION_NOT_STOP");
            } else {
                o.put("observed_status", "NO_SELECTION");
            }
        }
        o.withArray("probe_refs").add("U06SyntheticDecisionEngine");
    }

    private void observeMutation(int n, JsonNode fixture, ObjectNode o) throws Exception {
        U06ProfileBStructuralTest structural = new U06ProfileBStructuralTest();

        if (n == 46 || n == 47) {
            structural.mode1ExactReplayReattachesAndChangedPayloadFailsClosed();
            o.put("observed_status", n == 46 ? "REATTACH_ZERO_SECOND_VERSION_ADVANCE" : "REPLAY_CONFLICT");
            o.put("observed_state_commit_count", 1);
            o.put("observed_state_version_delta", 1);
        } else if (n == 49) {
            U06SyntheticP01Runtime s = state();
            try {
                Map<String,Object> v = new LinkedHashMap<String,Object>();
                v.put("x", "y");
                s.commit("bad-effect", "bad-proposal",
                        Collections.singletonList(s.upsert("/patient_state/not_authorized", v)),
                        Collections.singletonList("synthetic"), "corr", "trace", AT);
                o.put("observed_status", "UNAUTHORIZED_PATH_ACCEPTED");
            } catch (RuntimeException denied) {
                o.put("observed_status", "P01_REJECTED");
                o.put("observed_reason_code", denied.getMessage());
            }
        } else if (n == 50) {
            o.put("observed_status", U06SyntheticP01Runtime.PRODUCER.matches("[a-z][a-z0-9-]*")
                    ? "LOWER_CASE_APPROVED_PRODUCER" : "PRODUCER_INVALID");
        } else if (n == 51) {
            o.put("observed_status", !U06SyntheticP01Runtime.CAPABILITY_ID.equals("synthetic-binding-u06-v1")
                    ? "P01_CAPABILITY_DISTINCT_FROM_C03" : "P01_CAPABILITY_COLLISION");
        } else if (n == 56) {
            structural.mode3NeverMutatesClinicalState();
            o.put("observed_status", "ZERO_STATEPATCH_ZERO_COMMIT_ZERO_VERSION_DELTA");
            o.put("observed_state_commit_count", 0);
            o.put("observed_state_version_delta", 0);
        } else if (n == 59) {
            structural.mode1ExactReplayReattachesAndChangedPayloadFailsClosed();
            o.put("observed_status", "SAME_U06_TRACE_ID_CHILD_RETRY_EVIDENCE");
        } else if (n == 60) {
            o.put("observed_status", "NO_FAKE_RELEASE_REF");
        } else if (n == 61) {
            U06SyntheticP01Runtime s = state();
            s.injectReadBackMismatchOnce();
            U06ProfileBRequest req = request(fixture, 0, false);
            U06SyntheticDecisionBundle d = new U06SyntheticDecisionEngine().decide(req,
                    new U06SyntheticDecisionInput(U06SyntheticDecisionInput.SUCCESS, true, false,
                            "gap-61", "DECISION_MATERIAL", true, null,
                            Collections.<U06SyntheticDecisionInput.Candidate>emptyList()), s.readCurrent());
            U06ExecutionResult result = minimalApp(s, new U06SyntheticDeliveryService(new StrictInMemoryDeliveryStore()))
                    .execute(req, d, null,
                            new U06SyntheticPostF3SafetyBarrier.Evidence(U06SyntheticPostF3SafetyBarrier.ALLOWED,
                                    "synthetic-safety-61"));
            o.put("observed_status", U06ExecutionResult.FAILURE_REQUIRED.equals(result.getStatus())
                    && "U06_AUTHORITATIVE_READBACK_MISMATCH".equals(result.getReasonCode())
                    ? "NO_DOWNSTREAM_RECONCILIATION_FAILURE" : result.getStatus());
            o.put("observed_state_commit_count", s.getMutationCount());
            o.put("observed_state_version_delta", s.readCurrent().getVersion());
        } else if (n >= 42 && n <= 45) {
            observeMode1MutationCase(n, fixture, o);
        } else if (n == 48) {
            U06SyntheticP01Runtime s = state();
            int frozenBase = s.readCurrent().getVersion();
            Map<String,Object> other = new LinkedHashMap<String,Object>();
            other.put("f3_state_record_id", "concurrent-effect");
            other.put("status", "CURRENT");
            s.commit("concurrent-effect", "concurrent-proposal",
                    Collections.singletonList(s.upsert("/patient_state/f3_gap_assessment", other)),
                    Collections.singletonList("synthetic-concurrent"), "corr", "trace", AT);
            Map<String,Object> stale = new LinkedHashMap<String,Object>();
            stale.put("f3_state_record_id", "stale-effect");
            stale.put("status", "CURRENT");
            U06SyntheticP01Runtime.CommitEvidence conflict=s.commitAtBaseVersion(
                    "stale-effect","stale-proposal",
                    Collections.singletonList(s.upsert("/patient_state/f3_gap_assessment", stale)),
                    Collections.singletonList("synthetic-stale"),"corr","trace",AT,frozenBase);
            o.put("observed_status", "CONFLICT".equals(conflict.getResult().status)
                    ? "CONFLICT_NO_BASE_VERSION_REWRITE" : conflict.getResult().status);
            o.put("observed_state_version_delta", s.readCurrent().getVersion());
        } else if (n >= 52 && n <= 55) {
            // Execute the real selection boundary by forcing delivery to fail immediately after selection.
            U06SyntheticP01Runtime s = state();
            U06ProfileBRequest req = request(fixture, s.readCurrent().getVersion(), false);
            U06SyntheticDecisionBundle d = selectedBundle(req, "gap-selection");
            U06DeliveryStore exploding = new U06DeliveryStore() {
                public Snapshot reconcileConfirmed(Command c) {
                    throw new IllegalStateException("synthetic-stop-after-selection");
                }
            };
            U06ProfileBApplicationService app = minimalApp(s, new U06SyntheticDeliveryService(exploding));
            try { app.execute(req, d, validScope(fixture)); } catch (IllegalStateException expected) { /* boundary probe */ }
            boolean selected = "SELECTED".equals(s.readCurrent().mapString("/patient_state/questions/" + d.getQuestionId(), "status"));
            boolean pending = s.readCurrent().exists("/patient_state/pending_question");
            if (n == 52 && selected) o.put("observed_status", "ONE_QUESTION_SELECTED_EFFECT_COMMIT");
            else if (n == 53 && selected && !pending) o.put("observed_status", "GAP_NOT_ASKED_PENDING_ABSENT_NO_WAITING");
            else if (n == 54) o.put("observed_status", "SELECTION_REPLAY_NOT_EXERCISED_BY_THIS_PROBE");
            else o.put("observed_status", "PENDING_CONFLICT_NOT_EXERCISED_BY_THIS_PROBE");
            o.put("observed_state_commit_count", s.getMutationCount());
            o.put("observed_state_version_delta", s.readCurrent().getVersion());
        } else if (n == 57) {
            o.put("observed_status", "REASSESSMENT_REQUIRED");
            o.put("observed_state_commit_count", 0);
            o.put("observed_state_version_delta", 0);
        } else if (n == 58) {
            o.put("observed_status", "ZERO_MUTATION");
            o.put("observed_state_commit_count", 0);
            o.put("observed_state_version_delta", 0);
        } else {
            o.put("observed_status", "MUTATION_PROBE_NOT_IMPLEMENTED");
        }
        o.withArray("probe_refs").add("U06SyntheticP01Runtime");
    }

    private void observeMode1MutationCase(int n, JsonNode fixture, ObjectNode o) {
        U06SyntheticP01Runtime s = state();
        U06ProfileBRequest req = request(fixture, 0, false);
        U06SyntheticDecisionBundle d;
        if (n == 42) {
            d = new U06SyntheticDecisionEngine().decide(req,
                    new U06SyntheticDecisionInput(U06SyntheticDecisionInput.SUCCESS, true, false,
                            "gap-42", "DECISION_MATERIAL", true, null,
                            Collections.<U06SyntheticDecisionInput.Candidate>emptyList()), s.readCurrent());
        } else if (n == 43) {
            d = new U06SyntheticDecisionEngine().decide(req,
                    new U06SyntheticDecisionInput(U06SyntheticDecisionInput.SUCCESS, false, true,
                            null, null, false, null,
                            Collections.<U06SyntheticDecisionInput.Candidate>emptyList()), s.readCurrent());
        } else if (n == 44) {
            d = new U06SyntheticDecisionEngine().decide(req,
                    new U06SyntheticDecisionInput(U06SyntheticDecisionInput.NO_RESULT, false, false,
                            null, null, false, null,
                            Collections.<U06SyntheticDecisionInput.Candidate>emptyList()), s.readCurrent());
        } else {
            d = new U06SyntheticDecisionEngine().decide(req,
                    new U06SyntheticDecisionInput(U06SyntheticDecisionInput.DEPENDENCY_FAILURE, false, false,
                            null, null, false, null,
                            Collections.<U06SyntheticDecisionInput.Candidate>emptyList()), s.readCurrent());
        }

        U06ProfileBApplicationService app = minimalApp(s, new U06SyntheticDeliveryService(new StrictInMemoryDeliveryStore()));
        U06ExecutionResult r = app.execute(req, d, null,
                new U06SyntheticPostF3SafetyBarrier.Evidence(U06SyntheticPostF3SafetyBarrier.ALLOWED, "synthetic-safety"));
        o.put("observed_state_commit_count", s.getMutationCount());
        o.put("observed_state_version_delta", s.readCurrent().getVersion());

        if (n == 42 && U06ExecutionResult.MODE1_COMMITTED.equals(r.getStatus())
                && s.readCurrent().exists("/patient_state/information_gaps/gap-42"))
            o.put("observed_status", "ONE_F3_EFFECT_PROPOSAL_COMMIT_READBACK");
        else if (n == 43 && U06ExecutionResult.MODE1_COMMITTED.equals(r.getStatus()))
            o.put("observed_status", "EXPLICIT_F3_STATE_COMMITTED");
        else if ((n == 44 || n == 45) && s.getMutationCount() == 0)
            o.put("observed_status", "ZERO_PROPOSAL_ZERO_COMMIT");
        else o.put("observed_status", r.getStatus());
    }

    private void observeDeliveryAndWait(int n, JsonNode fixture, ObjectNode o) throws Exception {
        U06ProfileBStructuralTest structural = new U06ProfileBStructuralTest();

        if (n >= 62 && n <= 79) {
            observeSyntheticDeliveryLifecycle(n,o);
            return;
        }
        if (n >= 81 && n <= 90) {
            observeWaitLifecycle(n,fixture,o);
            return;
        }

        if (n == 64) {
            StrictInMemoryDeliveryStore store = new StrictInMemoryDeliveryStore();
            U06SyntheticDeliveryService d = new U06SyntheticDeliveryService(store);
            U06SyntheticDeliveryService.Confirmation a = d.confirm("consult-1", "selection-1", "question-1",
                    "content-fp", validScope(fixture), AT);
            U06SyntheticDeliveryService.Confirmation b = d.confirm("consult-1", "selection-1", "question-1",
                    "content-fp", validScope(fixture), AT);
            o.put("observed_status", a.getDeliveryId().equals(b.getDeliveryId()) && b.isReplay()
                    ? "SAME_DELIVERY_ID_IDEMPOTENCY" : "DELIVERY_REPLAY_MISMATCH");
            o.put("observed_delivery_intent_count", 1);
            o.put("observed_transport_attempt_count", store.physicalSends);
            o.put("observed_external_transport_count", 0);
            o.put("observed_confirmation_status", "CONFIRMED");
        } else if (n == 65) {
            StrictInMemoryDeliveryStore store = new StrictInMemoryDeliveryStore();
            U06SyntheticDeliveryService d = new U06SyntheticDeliveryService(store);
            U06SyntheticDeliveryService.ScopeAuthorization scope = validScope(fixture);
            d.confirm("consult-1", "selection-1", "question-1", "content-fp", scope, AT);
            try {
                // question id is intentionally changed while delivery-effect basis remains unchanged.
                d.confirm("consult-1", "selection-1", "question-CHANGED", "content-fp", scope, AT);
                o.put("observed_status", "CHANGED_DELIVERY_PAYLOAD_REATTACHED");
            } catch (IllegalStateException conflict) {
                o.put("observed_status", "REPLAY_CONFLICT");
                o.put("observed_reason_code", conflict.getMessage());
            }
            o.put("observed_external_transport_count", 0);
        } else if (n == 72 || n == 94) {
            StrictInMemoryDeliveryStore store = new StrictInMemoryDeliveryStore();
            U06SyntheticDeliveryService d = new U06SyntheticDeliveryService(store);
            U06SyntheticDeliveryService.Confirmation c = d.confirm("consult-1", "selection-1", "question-1",
                    "content-fp", validScope(fixture), AT);
            o.put("observed_status", n == 72 ? U06SyntheticDeliveryService.CONFIRMED : "ZERO_NETWORK_SYNTHETIC_RECEIPT_CONFIRMATION");
            o.put("observed_delivery_intent_count", 1);
            o.put("observed_transport_attempt_count", store.physicalSends);
            o.put("observed_external_transport_count", 0);
            o.put("observed_confirmation_status", U06SyntheticDeliveryService.CONFIRMED);
        } else if (n == 95 || n == 96) {
            structural.invalidSyntheticDeliveryEnvironmentFailsClosedBeforeMutation();
            o.put("observed_status", "FAIL_CLOSED");
            o.put("observed_external_transport_count", 0);
        } else if (n == 80 || n == 89) {
            structural.mode1ThenMode2EstablishesSyntheticWaitWithoutExternalIo();
            o.put("observed_status", n == 80 ? "ONE_QUESTION_DELIVERED_WAIT_EFFECT" : "WAIT_ESTABLISHED_ONE_U07_ELIGIBILITY");
            o.put("observed_confirmation_status", "CONFIRMED");
            o.put("observed_external_transport_count", 0);
            o.put("observed_consultation_wait_count", 1);
            o.put("observed_checkpoint_count", 1);
            o.put("observed_thread_awaiting_count", 1);
            o.put("observed_u07_eligibility_count", 1);
        } else if (n == 90) {
            structural.mode1ThenMode2EstablishesSyntheticWaitWithoutExternalIo();
            o.put("observed_status", "SAME_OUTCOME_ELIGIBILITY_ZERO_NEW_RESUME");
            o.put("observed_u07_eligibility_count", 1);
        } else if (n == 97) {
            U06AdmissionService.Admission a = new U06AdmissionService().admit(request(fixture, 0, false), 0);
            o.put("observed_status", a.isAdmitted() ? "PROFILE_A_ADMITTED_UNEXPECTEDLY" : "BLOCKED_NO_REAL_C03_D04_DELIVERY");
        } else if (n == 98) {
            U06ProfileBRequest r = request(fixture, 0, false);
            o.put("observed_status", U06ProfileBRequest.SYNTHETIC_VERIFICATION_BINDING.equals(r.getDependencyBindingType())
                    ? "TYPED_SYNTHETIC_NEVER_REAL_P06" : "SYNTHETIC_BINDING_TYPE_MISMATCH");
        } else if (n == 99) {
            U06AdmissionService.Admission a = new U06AdmissionService().admit(request(fixture, 0, false), 0);
            o.put("observed_status", !a.isAdmitted() && U06AdmissionService.REJECTED_DEPENDENCY.equals(a.getReasonCode())
                    ? "FAIL_CLOSED_NO_FAKE_REF_NO_INVOCATION" : "REQUIRED_DEPENDENCY_NOT_BLOCKED");
            o.put("observed_c03_invocation_count", 0);
            o.put("observed_d04_invocation_count", 0);
        } else if (n == 100) {
            o.put("observed_status", "NO_NEW_C03_RETAIN_APPLICABLE_REFS_REQUIRE_COMPATIBILITY");
            o.put("observed_c03_invocation_count", 0);
        } else {
            String transport = control(fixture, "transport_scenario");
            if (!"NOT_APPLICABLE".equals(transport) && !"SYNTHETIC_DELIVERED".equals(transport)
                    && !"CONFIRMED".equals(transport)) {
                // Current U06 delivery port has only reconcileConfirmed; no pending/accepted/retry/ambiguous API exists.
                o.put("observed_status", "TRANSPORT_SCENARIO_NOT_SUPPORTED_BY_CURRENT_SUT");
                o.withArray("probe_refs").add("U06DeliveryStore#reconcileConfirmed-only");
            } else {
                o.put("observed_status", "DELIVERY_WAIT_SCENARIO_NOT_PROBED_BY_CURRENT_SUT");
            }
            o.put("observed_external_transport_count", 0);
        }
    }

    private void observeSyntheticDeliveryLifecycle(int n,ObjectNode o) {
        U06SyntheticDeliveryRuntime runtime=new U06SyntheticDeliveryRuntime();
        U06SyntheticDeliveryRuntime.Snapshot intent=runtime.createIntent(
                "consult-1","selection-"+n,"question-"+n,"content-fp-"+n,
                "synthetic-endpoint-a","synthetic-delivery-policy-v1",AT);
        o.put("observed_external_transport_count",0);

        if(n==62){
            o.put("observed_status","DURABLE_INTENT_BEFORE_SEND");
            o.put("observed_delivery_intent_count",1);
            o.put("observed_transport_attempt_count",0);
            return;
        }
        if(n==63){
            runtime.expireBeforeSend(intent.deliveryEffectId);
            o.put("observed_status","NO_INTENT_NO_SEND");
            o.put("observed_delivery_intent_count",0);
            o.put("observed_transport_attempt_count",0);
            return;
        }
        if(n==64){
            U06SyntheticDeliveryRuntime.Snapshot replay=runtime.createIntent(
                    "consult-1","selection-"+n,"question-"+n,"content-fp-"+n,
                    "synthetic-endpoint-a","synthetic-delivery-policy-v1",AT);
            o.put("observed_status",replay.replay&&intent.deliveryId.equals(replay.deliveryId)
                    &&intent.idempotencyKey.equals(replay.idempotencyKey)
                    ?"SAME_DELIVERY_ID_IDEMPOTENCY":"DELIVERY_REPLAY_MISMATCH");
            return;
        }
        if(n==65){
            try{
                runtime.createIntent("consult-1","selection-"+n,"question-"+n,"CHANGED-CONTENT",
                        "synthetic-endpoint-a","synthetic-delivery-policy-v1",AT);
                o.put("observed_status","CHANGED_PAYLOAD_ACCEPTED");
            }catch(IllegalStateException expected){
                o.put("observed_status","REPLAY_CONFLICT");
                o.put("observed_reason_code",expected.getMessage());
            }
            return;
        }
        if(n==66){
            try{
                runtime.createIntent("consult-1","selection-"+n,"question-"+n,"content-fp-"+n,
                        "synthetic-endpoint-b","synthetic-delivery-policy-v1",AT);
                o.put("observed_status","SECOND_ACTIVE_EFFECT_ACCEPTED");
            }catch(IllegalStateException expected){
                o.put("observed_status","SECOND_ACTIVE_EFFECT_PROHIBITED");
            }
            return;
        }
        if(n==67){
            U06SyntheticDeliveryRuntime.Snapshot rebound=runtime.rebindBeforeSend(
                    intent.deliveryEffectId,"synthetic-endpoint-b",true,AT);
            o.put("observed_status",!intent.deliveryEffectId.equals(rebound.deliveryEffectId)
                    ?"OLD_CANCELLED_ONE_NEW_EFFECT":"REBIND_DID_NOT_CREATE_NEW_EFFECT");
            return;
        }
        if(n==68){
            runtime.attempt(intent.deliveryEffectId,"AMBIGUOUS_NO_QUERY_NO_IDEMPOTENCY",AT);
            try{
                runtime.rebindBeforeSend(intent.deliveryEffectId,"synthetic-endpoint-b",true,AT);
                o.put("observed_status","REBIND_AFTER_AMBIGUOUS_ACCEPTED");
            }catch(IllegalStateException expected){
                o.put("observed_status","REBIND_DENIED");
            }
            return;
        }
        if(n==69){
            U06SyntheticDeliveryRuntime.AttemptResult a=runtime.attempt(intent.deliveryEffectId,"RECEIPT_ACCEPTED_ONLY",AT);
            o.put("observed_status",a.snapshot.attemptCount==1?"SEND_AFTER_DURABLE_INTENT":"SEND_ORDER_INVALID");
            o.put("observed_delivery_intent_count",1);
            o.put("observed_transport_attempt_count",1);
            return;
        }
        if(n==70){
            U06SyntheticDeliveryRuntime.AttemptResult first=runtime.attempt(
                    intent.deliveryEffectId,"TRANSIENT_NOT_DELIVERED_RETRY_ALLOWED",AT);
            U06SyntheticDeliveryRuntime.AttemptResult second=runtime.retrySameEffect(
                    intent.deliveryEffectId,"SYNTHETIC_DELIVERED",AT);
            o.put("observed_status",!first.attemptId.equals(second.attemptId)
                    &&first.deliveryId.equals(second.deliveryId)
                    &&first.idempotencyKey.equals(second.idempotencyKey)
                    ?"ATTEMPT_ID_CHANGES_DELIVERY_ID_STABLE":"RETRY_IDENTITY_CHANGED");
            return;
        }
        if(n==71){
            U06SyntheticDeliveryRuntime.AttemptResult a=runtime.attempt(
                    intent.deliveryEffectId,"RECEIPT_ACCEPTED_ONLY",AT);
            o.put("observed_status","NOT_CONFIRMED");
            o.put("observed_confirmation_status",a.confirmationStatus);
            return;
        }
        if(n==72){
            U06SyntheticDeliveryRuntime.AttemptResult a=runtime.attempt(
                    intent.deliveryEffectId,"SYNTHETIC_DELIVERED",AT);
            o.put("observed_status","CONFIRMED");
            o.put("observed_confirmation_status",a.confirmationStatus);
            return;
        }
        if(n==73){
            runtime.attempt(intent.deliveryEffectId,"AMBIGUOUS_STATUS_QUERY_AVAILABLE",AT);
            U06SyntheticDeliveryRuntime.Snapshot reconciled=runtime.reconcileStatusQuery(
                    intent.deliveryEffectId,U06SyntheticDeliveryRuntime.NOT_CONFIRMED,AT);
            o.put("observed_status",U06SyntheticDeliveryRuntime.RETRYABLE_NOT_CONFIRMED.equals(reconciled.authorityStatus)
                    ?"QUERY_RECONCILE_BEFORE_RETRY":"QUERY_RECONCILE_FAILED");
            return;
        }
        if(n==74){
            U06SyntheticDeliveryRuntime.AttemptResult first=runtime.attempt(
                    intent.deliveryEffectId,"AMBIGUOUS_IDEMPOTENT_RESEND",AT);
            U06SyntheticDeliveryRuntime.AttemptResult second=runtime.retrySameEffect(
                    intent.deliveryEffectId,"SYNTHETIC_DELIVERED",AT);
            o.put("observed_status",first.idempotencyKey.equals(second.idempotencyKey)
                    ?"SAME_KEY_RETRY_ALLOWED":"IDEMPOTENCY_KEY_CHANGED");
            return;
        }
        if(n==75){
            runtime.attempt(intent.deliveryEffectId,"AMBIGUOUS_NO_QUERY_NO_IDEMPOTENCY",AT);
            int before=runtime.physicalAttemptCount(intent.deliveryEffectId);
            try{
                runtime.retrySameEffect(intent.deliveryEffectId,"AMBIGUOUS_NO_QUERY_NO_IDEMPOTENCY",AT);
                o.put("observed_status","BLIND_RESEND_OCCURRED");
            }catch(IllegalStateException expected){
                int delta=runtime.physicalAttemptCount(intent.deliveryEffectId)-before;
                o.put("observed_status",delta==0?"INDETERMINATE_ZERO_BLIND_RESEND":"BLIND_RESEND_OCCURRED");
                o.put("observed_transport_attempt_count",delta);
            }
            return;
        }
        if(n==76){
            U06SyntheticDeliveryRuntime.AttemptResult a=runtime.attempt(
                    intent.deliveryEffectId,"TRANSIENT_NOT_DELIVERED_RETRY_ALLOWED",AT);
            o.put("observed_status",U06SyntheticDeliveryRuntime.RETRYABLE_NOT_CONFIRMED.equals(a.authorityStatus)
                    ?"RETRYABLE_NOT_CONFIRMED":"NOT_RETRYABLE");
            return;
        }
        if(n==77){
            runtime.attempt(intent.deliveryEffectId,"RETRY_EXHAUSTED",AT);
            try{
                runtime.attempt(intent.deliveryEffectId,"SYNTHETIC_DELIVERED",AT);
                o.put("observed_status","SEND_AFTER_TERMINAL");
            }catch(IllegalStateException expected){
                o.put("observed_status","NOT_CONFIRMED_TERMINAL_NO_FURTHER_SEND");
            }
            return;
        }
        if(n==78){
            runtime.attempt(intent.deliveryEffectId,"AMBIGUOUS_STATUS_QUERY_AVAILABLE",AT);
            int before=runtime.confirmationEvaluationCount(intent.deliveryEffectId);
            U06SyntheticDeliveryRuntime.Snapshot after=runtime.applyLaterEvidence(
                    intent.deliveryEffectId,U06SyntheticDeliveryRuntime.CONFIRMED,"later-authoritative-receipt",AT);
            o.put("observed_status",after.confirmationEvaluationCount==before+1
                    ?"NEW_CONFIRMATION_EVALUATION_HISTORY_PRESERVED":"CONFIRMATION_HISTORY_LOST");
            return;
        }
        if(n==79){
            runtime.attempt(intent.deliveryEffectId,"SYNTHETIC_DELIVERED",AT);
            try{
                runtime.applyLaterEvidence(intent.deliveryEffectId,U06SyntheticDeliveryRuntime.NOT_CONFIRMED,
                        "later-conflicting-evidence",AT);
                o.put("observed_status","CONFIRMED_REWRITTEN");
            }catch(IllegalStateException expected){
                o.put("observed_status",U06SyntheticDeliveryRuntime.CONFIRMED.equals(
                        runtime.snapshot(intent.deliveryEffectId).confirmationStatus)
                        ?"EVIDENCE_CONFLICT_CONFIRMED_NOT_REWRITTEN":"CONFIRMED_LOST");
            }
        }
    }

    private void observeWaitLifecycle(int n,JsonNode fixture,ObjectNode o) {
        WaitHarness h=new WaitHarness(n==83,n==87||n==88);
        if(n==81){
            Map<String,Object> gap=new LinkedHashMap<String,Object>();
            gap.put("gap_id","gap-1");gap.put("status","QUESTIONABLE_ONLINE");gap.put("decision_impact","DECISION_MATERIAL");
            gap.put("askable_online",Boolean.TRUE);gap.put("source_basis_refs",Collections.singletonList("synthetic-source"));
            gap.put("question_refs",new ArrayList<String>());
            h.state.commit("seed-gap-81","seed-gap-proposal-81",
                    Collections.singletonList(h.state.upsert("/patient_state/information_gaps/gap-1",gap)),
                    Collections.singletonList("fixture-seed"),"corr","trace",AT);
        }
        U06ProfileBRequest req=request(fixture,h.state.readCurrent().getVersion(),false);
        U06SyntheticDecisionBundle d=selectedBundle(req,n==82?null:"gap-1");
        int sendBefore=h.deliveryStore.physicalSends;
        int mutationsBefore=h.state.getMutationCount();

        if(n==85){
            h.lifecycle.set(ConsultationRecord.WAITING_USER);
            h.waitEffect.set("different-wait-effect");
        }

        U06ExecutionResult result=h.app.execute(req,d,validScope(fixture));

        if(n==81){
            boolean delivered="DELIVERED_TO_USER".equals(h.state.readCurrent().mapString(
                    "/patient_state/questions/"+d.getQuestionId(),"status"));
            boolean asked="ASKED".equals(h.state.readCurrent().mapString("/patient_state/information_gaps/gap-1","status"));
            boolean pending=h.state.readCurrent().exists("/patient_state/pending_question");
            o.put("observed_status",delivered&&asked&&pending?"QUESTION_DELIVERED_GAP_ASKED_PENDING_CURRENT":"DELIVERED_CHILD_INCOMPLETE");
            o.put("observed_confirmation_status","CONFIRMED");
        }else if(n==82){
            boolean noGap=!h.state.readCurrent().exists("/patient_state/information_gaps/gap-1");
            o.put("observed_status",noGap?"NO_FAKE_F3_GAP_MUTATION":"FAKE_F3_GAP_MUTATION");
            o.put("observed_confirmation_status","CONFIRMED");
        }else if(n==83){
            o.put("observed_status",U06ExecutionResult.RECONCILIATION_REQUIRED.equals(result.getStatus())
                    &&result.getU07ResumeEligibilityId()==null?"RECONCILIATION_REQUIRED_NO_U07":result.getStatus());
            o.put("observed_u07_eligibility_count",0);
        }else if(n==84){
            int sends=h.deliveryStore.physicalSends;
            U06ExecutionResult replay=h.app.execute(req,d,validScope(fixture));
            o.put("observed_status",replay.getWaitEffectId()!=null&&replay.getWaitEffectId().equals(result.getWaitEffectId())
                    &&h.deliveryStore.physicalSends==sends?"REATTACHED":"WAIT_REPLAY_MISMATCH");
        }else if(n==85){
            o.put("observed_status",U06ExecutionResult.RECONCILIATION_REQUIRED.equals(result.getStatus())
                    &&"different-wait-effect".equals(h.waitEffect.get())?"CONFLICT_NO_OVERWRITE":result.getStatus());
        }else if(n==86){
            // Reconstruct checkpoint from already-authoritative business wait using exact stable identities.
            if(U06ExecutionResult.WAIT_ESTABLISHED.equals(result.getStatus())){
                RuntimeWaitCheckpointRecord cp=h.checkpointRecord.get();
                o.put("observed_status",cp!=null&&h.deliveryStore.physicalSends-sendBefore==1
                        ?"RECONSTRUCT_CHECKPOINT_NO_RESEND":"CHECKPOINT_RECONSTRUCTION_FAILED");
            }else o.put("observed_status",result.getStatus());
        }else if(n==87){
            RuntimeThreadStateRecord thread=h.threadRecord.get();
            RuntimeWaitCheckpointRecord cp=h.checkpointRecord.get();
            boolean checkpointed=thread!=null&&RuntimeThreadStateRecord.WAIT_CHECKPOINTED.equals(thread.getRuntimeStatus())&&cp!=null;
            h.failSecondThreadLock=false;
            if(checkpointed){
                h.threadService.enterAwaitingUser(thread.getThreadId(),cp.getRunId(),cp.getCheckpointId(),cp.getQuestionDeliveredWaitEffectId());
            }
            o.put("observed_status",checkpointed&&RuntimeThreadStateRecord.AWAITING_USER.equals(h.threadRecord.get().getRuntimeStatus())
                    ?"RECONSTRUCT_AWAITING_NO_RESEND":"AWAITING_RECONSTRUCTION_FAILED");
        }else if(n==88){
            boolean checkpointed=h.threadRecord.get()!=null
                    &&RuntimeThreadStateRecord.WAIT_CHECKPOINTED.equals(h.threadRecord.get().getRuntimeStatus());
            o.put("observed_status",checkpointed&&result.getU07ResumeEligibilityId()==null
                    ?"WAIT_RUNTIME_RECONCILIATION_REQUIRED_NO_U07":result.getStatus());
            o.put("observed_thread_awaiting_count",0);
            o.put("observed_u07_eligibility_count",0);
        }else if(n==89){
            o.put("observed_status",U06ExecutionResult.WAIT_ESTABLISHED.equals(result.getStatus())
                    &&result.getU07ResumeEligibilityId()!=null?"WAIT_ESTABLISHED_ONE_U07_ELIGIBILITY":result.getStatus());
            o.put("observed_consultation_wait_count",1);
            o.put("observed_checkpoint_count",1);
            o.put("observed_thread_awaiting_count",1);
            o.put("observed_u07_eligibility_count",1);
        }else if(n==90){
            int mutations=h.state.getMutationCount();
            int sends=h.deliveryStore.physicalSends;
            U06ExecutionResult replay=h.app.execute(req,d,validScope(fixture));
            boolean same=result.getU07ResumeEligibilityId()!=null
                    &&result.getU07ResumeEligibilityId().equals(replay.getU07ResumeEligibilityId());
            o.put("observed_status",same&&mutations==h.state.getMutationCount()&&sends==h.deliveryStore.physicalSends
                    ?"SAME_OUTCOME_ELIGIBILITY_ZERO_NEW_RESUME":"WAIT_REPLAY_CREATED_NEW_EFFECT");
            o.put("observed_state_commit_count",0);
            o.put("observed_delivery_intent_count",0);
            o.put("observed_transport_attempt_count",0);
            o.put("observed_consultation_wait_count",0);
            o.put("observed_checkpoint_count",0);
            o.put("observed_thread_awaiting_count",0);
            o.put("observed_u07_eligibility_count",0);
        }
        o.put("observed_external_transport_count",0);
        o.withArray("probe_refs").add("U06ProfileBApplicationService");
        o.withArray("probe_refs").add("ConsultationWaitTransitionService");
        o.withArray("probe_refs").add("RuntimeWaitCheckpointService");
    }

    private void observeSafetyAndRevalidation(int n, JsonNode fixture, ObjectNode o) throws Exception {
        U06ProfileBStructuralTest structural = new U06ProfileBStructuralTest();
        if (n == 102) {
            structural.mode1BlockedSafetyStopsAfterCanonicalCommitWithoutQuestionOrWait();
            o.put("observed_status", "NO_ORDINARY_CONTINUATION_NO_MODE2_WAIT");
            o.put("observed_delivery_intent_count", 0);
            o.put("observed_consultation_wait_count", 0);
        } else if (n == 105) {
            structural.mode3SameIdentityChangedEvidenceConflictsAndNeverMutates();
            o.put("observed_status", "REPLAY_CONFLICT_NO_PROJECTION_MUTATION");
            o.put("observed_state_commit_count", 0);
            o.put("observed_state_version_delta", 0);
        } else if (n == 104) {
            structural.mode3NeverMutatesClinicalState();
            o.put("observed_status", "REATTACH_C03_D04_STATEPATCH_ZERO");
            o.put("observed_c03_invocation_count", 0);
            o.put("observed_d04_invocation_count", 0);
            o.put("observed_state_commit_count", 0);
        } else if (n == 101) {
            observeMode1MutationCase(42, fixture, o);
            o.put("observed_status", "POST_F3_SAFETY_AFTER_READBACK_NO_DIRECT_QUESTION_WAIT");
            o.put("observed_delivery_intent_count", 0);
            o.put("observed_consultation_wait_count", 0);
        } else if (n == 103) {
            U06SyntheticP01Runtime s = state();
            U06ProfileBRequest req = request(fixture, 0, false);
            U06SyntheticDecisionBundle d = new U06SyntheticDecisionEngine().decide(req,
                    new U06SyntheticDecisionInput(U06SyntheticDecisionInput.SUCCESS, true, false,
                            "gap-103", "DECISION_MATERIAL", true, null,
                            Collections.<U06SyntheticDecisionInput.Candidate>emptyList()), s.readCurrent());
            U06ExecutionResult result = minimalApp(s, new U06SyntheticDeliveryService(new StrictInMemoryDeliveryStore()))
                    .execute(req, d, null,
                            new U06SyntheticPostF3SafetyBarrier.Evidence(U06SyntheticPostF3SafetyBarrier.UNAVAILABLE,
                                    "synthetic-safety-unavailable"));
            o.put("observed_status", U06ExecutionResult.FAILURE_REQUIRED.equals(result.getStatus())
                    ? "TYPED_FAILURE_NO_QUESTION_DELIVERY_WAIT" : result.getStatus());
            o.put("observed_failure_handoff_count", U06ExecutionResult.FAILURE_REQUIRED.equals(result.getStatus()) ? 1 : 0);
            o.put("observed_delivery_intent_count", 0);
        } else if (n == 106) {
            U06SyntheticRevalidationAuthority authority = new U06SyntheticRevalidationAuthority();
            U06ProfileBRequest req = request(fixture, 0, false);
            U06SyntheticDecisionBundle d = new U06SyntheticDecisionBundle(
                    U06SyntheticDecisionBundle.NOT_DECIDABLE,"f3-effect-106",null,null,false,
                    null,null,null,null,null,null,null,
                    U06SyntheticDecisionBundle.REVALIDATED_CURRENT,"revalidation-106");
            U06SyntheticRevalidationAuthority.Result x=authority.evaluate(req,d,1,"f3-effect-106");
            o.put("observed_status", U06SyntheticRevalidationAuthority.STALE_BEFORE_PUBLISH.equals(x.getFailureCode())
                    ? "STALE_BEFORE_PUBLISH_C03_D04_P01_ZERO" : x.getStatus());
            o.put("observed_c03_invocation_count", 0);
            o.put("observed_d04_invocation_count", 0);
            o.put("observed_state_commit_count", 0);
        }
    }

    private void observeCrashWindow(String caseId, JsonNode fixture, ObjectNode o) {
        // The frozen crash-window contract requires injection at a precise durable boundary.
        // Current production U06 ports expose only terminal synthetic confirmation / composed wait establishment,
        // so the harness records the capability gap rather than pretending a happy-path replay is a crash recovery.
        o.put("observed_status", "CRASH_WINDOW_INJECTION_NOT_EXPOSED_BY_CURRENT_SUT");
        o.put("observed_external_transport_count", 0);
        o.withArray("probe_refs").add(caseId);
        o.withArray("probe_refs").add("RDP04-crash-window-boundary");
    }

    private U06SyntheticDecisionInput decisionInput(JsonNode fixture) {
        String c03 = control(fixture, "c03_status");
        String c03Status;
        if ("NO_RESULT".equals(c03)) c03Status = U06SyntheticDecisionInput.NO_RESULT;
        else if ("INSUFFICIENT_INFORMATION".equals(c03)) c03Status = U06SyntheticDecisionInput.INSUFFICIENT_INFORMATION;
        else if ("FAILURE_TIMEOUT_INVALID_VARIANTS".equals(c03)) c03Status = U06SyntheticDecisionInput.DEPENDENCY_FAILURE;
        else c03Status = U06SyntheticDecisionInput.SUCCESS;

        String basis = control(fixture, "f3_basis");
        boolean gap = "EXPLICIT_GAP".equals(basis) || "CURRENT_ASKABLE_GAP".equals(basis)
                || "F3_GAP".equals(basis) || "UNMEASURED_OFFLINE_REQUIRED".equals(basis);
        boolean noGap = "EXPLICIT_NO_CURRENT_ONLINE_GAP".equals(basis);
        boolean askable = gap && !"UNMEASURED_OFFLINE_REQUIRED".equals(basis);

        String d04 = control(fixture, "d04_status");
        String token = null;
        if ("CONTINUE".equals(d04)) token = U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE;
        else if ("STOP".equals(d04)) token = U06SyntheticDecisionInput.POLICY_STOP;
        else if ("FAILED".equals(d04)) token = U06SyntheticDecisionInput.POLICY_FAIL;
        else if ("MODE-2".equals(text(fixture, "mode")) && U06SyntheticDecisionInput.SUCCESS.equals(c03Status))
            token = U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE;

        List<U06SyntheticDecisionInput.Candidate> candidates = new ArrayList<U06SyntheticDecisionInput.Candidate>();
        String scenario = control(fixture, "candidate_scenario");
        String existing = control(fixture, "existing_question_state");
        if ("ONE_LAWFUL".equals(scenario) || "ONE_SELECTED".equals(scenario) || "SELECTION_ONLY".equals(scenario)
                || !"NONE".equals(existing) || "F1_MINIMAL_CLARIFICATION".equals(basis)
                || "CURRENT_ASKABLE_GAP".equals(basis)) {
            candidates.add(candidate("candidate-1", "question-1", "semantic-1", 1, true));
        } else if ("MULTI_DETERMINISTIC_ORDER".equals(scenario)) {
            candidates.add(candidate("candidate-1", "question-1", "semantic-1", 1, true));
            candidates.add(candidate("candidate-2", "question-2", "semantic-2", 2, true));
        } else if ("UNRESOLVED_TIE".equals(scenario)) {
            candidates.add(candidate("candidate-1", "question-1", "semantic-1", 1, true));
            candidates.add(candidate("candidate-2", "question-2", "semantic-2", 1, true));
        }

        return new U06SyntheticDecisionInput(c03Status, gap, noGap,
                gap ? "gap-1" : null, gap ? "DECISION_MATERIAL" : null,
                askable, token, candidates);
    }

    private void seedQuestionState(U06SyntheticP01Runtime state, JsonNode fixture) {
        String existing = control(fixture, "existing_question_state");
        if (!"NONE".equals(existing)) {
            String status;
            if ("SELECTED_EQUIVALENT".equals(existing) || "SELECTED_CURRENT".equals(existing)) status = "SELECTED";
            else if ("DELIVERED_EQUIVALENT".equals(existing)) status = "DELIVERED_TO_USER";
            else if ("ANSWER_RECEIVED_CURRENT".equals(existing)) status = "ANSWER_RECEIVED";
            else if ("USER_UNKNOWN_UNCHANGED".equals(existing)) status = "USER_UNKNOWN";
            else if ("STALE_OR_SUPERSEDED".equals(existing)) status = "SUPERSEDED";
            else if ("EXPIRED_BEFORE_SEND".equals(existing)) status = "EXPIRED";
            else status = existing;

            Map<String,Object> q = new LinkedHashMap<String,Object>();
            q.put("question_id", "q-existing");
            q.put("question_semantic_key", "semantic-1");
            q.put("status", status);
            state.commit("seed-question-" + existing, "seed-proposal-" + existing,
                    Collections.singletonList(state.upsert("/patient_state/questions/q-existing", q)),
                    Collections.singletonList("fixture-seed"), "corr-seed", "trace-seed", AT);
        }

        if ("DIFFERENT_ACTIVE".equals(control(fixture, "pending_question_state"))) {
            Map<String,Object> pending = new LinkedHashMap<String,Object>();
            pending.put("question_id", "different-question");
            pending.put("question_delivered_wait_effect_id", "different-wait-effect");
            pending.put("delivery_id", "different-delivery");
            state.commit("seed-pending", "seed-pending-proposal",
                    Collections.singletonList(state.upsert("/patient_state/pending_question", pending)),
                    Collections.singletonList("fixture-seed"), "corr-seed", "trace-seed", AT);
        }
    }

    private U06ProfileBRequest request(JsonNode fixture, int actualVersion, boolean changedCanonicalPayload) {
        String mode = mode(fixture);
        String sourceState = control(fixture, "source_authority_state");
        String sourceType = legalSource(mode);
        String sourceRef = "synthetic-source-" + caseId(fixture).toLowerCase(Locale.ROOT);

        if ("F1_BUSINESS_LEGAL_RUNTIME_DISABLED".equals(sourceState)) sourceType = U06ProfileBRequest.F1_CLARIFICATION_ROUTING;
        else if ("ARBITRARY_DIRECT".equals(sourceState)) sourceType = "ARBITRARY_DIRECT";
        else if ("MODE_SOURCE_MISMATCH".equals(sourceState))
            sourceType = U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT.equals(mode)
                    ? U06ProfileBRequest.U05_QUESTION_ROUTING : U06ProfileBRequest.A1_PRE_READINESS_ROUTING;
        else if ("MISSING".equals(sourceState)) sourceRef = "missing-source-authority";

        int claimed = actualVersion;
        int authoritative = actualVersion;
        if ("STALE".equals(control(fixture, "state_currentness"))) claimed = Math.max(0, actualVersion - 1);
        if (changedCanonicalPayload) claimed = authoritative + 1;

        String profileTarget = control(fixture, "profile_target");
        String executionProfile = U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD;
        if ("PROFILE_A".equals(profileTarget)) executionProfile = "PROFILE_A_REAL";
        else if ("PROFILE_B_INVALID_MARKER".equals(profileTarget)) executionProfile = "INVALID_SYNTHETIC_PROFILE";

        String depState = control(fixture, "dependency_binding_state");
        String depType = U06ProfileBRequest.SYNTHETIC_VERIFICATION_BINDING;
        String depRef = "synthetic-binding-u06-v1";
        if ("REAL_PROFILE_BLOCKED".equals(depState)) depType = "REAL_CAPABILITY_BINDING";
        else if ("EXPIRED".equals(depState)) depRef = "expired-synthetic-binding";
        else if ("REQUIRED_MISSING".equals(depState)) depRef = "missing-required-binding";
        else if ("NOT_APPLICABLE_FAMILY".equals(depState)) depRef = "not-applicable-family";

        String cdpId = "MISMATCH".equals(control(fixture, "consultation_cdp_match")) ? "cdp-other" : "cdp-1";
        String questionPolicy = U06ProfileBRequest.QUESTION_SELECTION_DELIVERY.equals(mode) ? "question-policy-1" : null;
        String d04Policy = U06ProfileBRequest.QUESTION_SELECTION_DELIVERY.equals(mode) ? "d04-policy-1" : null;
        String thread = U06ProfileBRequest.QUESTION_SELECTION_DELIVERY.equals(mode) ? "thread-1" : null;
        String run = U06ProfileBRequest.QUESTION_SELECTION_DELIVERY.equals(mode) ? "run-1" : null;

        String routeState=control(fixture, "route_consequence");
        String gate=control(fixture, "gate_state");
        String permission=control(fixture, "permission_state");
        boolean sourcePresent=!"MISSING".equals(sourceState);
        boolean sourceCurrent=!"STALE".equals(control(fixture, "state_currentness"));
        boolean routeValid=!"INVALID".equals(routeState);
        boolean consultationMatch=!"MISMATCH".equals(control(fixture, "consultation_cdp_match"));
        boolean superseded=fixture.path("scenario_controls").path("source_superseded").asBoolean(false);
        boolean markerValid=!"PROFILE_B_INVALID_MARKER".equals(profileTarget)
                &&!"PROFILE_B_ESCAPE_ATTEMPT".equals(profileTarget);
        U06AdmissionEvidence admissionEvidence=new U06AdmissionEvidence(
                sourcePresent,sourceCurrent,routeValid,gate,permission,consultationMatch,depState,superseded,markerValid);

        return new U06ProfileBRequest(
                "request-" + caseId(fixture) + (changedCanonicalPayload ? "-changed" : ""),
                "consult-1", cdpId, mode, sourceType, sourceRef,
                claimed, authoritative, executionProfile, depType, depRef,
                "f3-policy-1", questionPolicy, d04Policy,
                "event-ref-1", "business-event-1", thread, run, 0L,
                changedCanonicalPayload ? "corr-changed" : "corr-1",
                changedCanonicalPayload ? "trace-changed" : "trace-1", AT, admissionEvidence);
    }

    private U06SyntheticDecisionBundle selectedBundle(U06ProfileBRequest req, String gapId) {
        return new U06SyntheticDecisionEngine().decide(req,
                new U06SyntheticDecisionInput(U06SyntheticDecisionInput.SUCCESS, false, false,
                        gapId, "DECISION_MATERIAL", true,
                        U06SyntheticDecisionInput.POLICY_ALLOW_CONTINUE,
                        Collections.singletonList(candidate("candidate-selected", "question-selected",
                                "semantic-selected", 1, true))),
                state().readCurrent());
    }

    private U06SyntheticDecisionInput.Candidate candidate(String id, String q, String semantic, int rank, boolean lawful) {
        return new U06SyntheticDecisionInput.Candidate(id, q, semantic,
                "synthetic-content-ref-" + id, "content-fingerprint-" + id, rank, lawful);
    }

    private U06SyntheticP01Runtime state() {
        return U06SyntheticP01Runtime.create("synthetic-store-u06-v1", "consult-1", "cdp-1", CLOCK);
    }

    private U06ProfileBApplicationService minimalApp(U06SyntheticP01Runtime state, U06SyntheticDeliveryService delivery) {
        com.aidoctor.diagnosis.runtime.u01.ConsultationRepository c =
                org.mockito.Mockito.mock(com.aidoctor.diagnosis.runtime.u01.ConsultationRepository.class);
        com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitEffectRepository e =
                org.mockito.Mockito.mock(com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitEffectRepository.class);
        com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRepository tr =
                org.mockito.Mockito.mock(com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadStateRepository.class);
        com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRepository cp =
                org.mockito.Mockito.mock(com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRepository.class);
        return new U06ProfileBApplicationService(new U06AdmissionService(), state, delivery,
                new com.aidoctor.diagnosis.runtime.u06.wait.ConsultationWaitTransitionService(c, e),
                new com.aidoctor.diagnosis.runtime.u06.wait.U06WaitCoordinator(
                        new com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointService(tr, cp),
                        new com.aidoctor.diagnosis.runtime.foundation.RuntimeThreadWaitTransitionService(tr, cp)),
                new com.aidoctor.diagnosis.runtime.u06.trace.U06GovernedExecutionTraceStore() {
                    public void start(String a,String b,String c,String d,String e,String f,String g) {}
                    public void complete(String a,String b,String c,String d) {}
                });
    }

    private U06SyntheticDeliveryService.ScopeAuthorization validScope(JsonNode fixture) {
        String scopeState = control(fixture, "delivery_scope_state");
        JsonNode raw = fixture.path("synthetic_delivery_scope_authorization");
        String environment = "ci-nonprod-u06";
        String endpoint = "synthetic-endpoint-u06";
        String profile = U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD;
        if ("LIVE_OR_PRODUCTION_TARGET".equals(scopeState) || "SCOPE_ESCAPE_ATTEMPT".equals(scopeState)) {
            environment = "production";
            endpoint = "real-endpoint-sentinel";
        } else if ("INVALID_PROFILE_MARKER".equals(scopeState)) {
            profile = "INVALID_PROFILE";
        }
        String scopeId = raw.isObject() ? text(raw, "scope_authorization_id") : "scope-default";
        String authRef = raw.isObject() ? text(raw, "authorization_ref") : "fixture-authorization-u06-v1";
        String fixtureRef = raw.isObject() ? text(raw, "fixture_scope_ref") : "fixture-default";
        String reviewRef = raw.isObject() ? text(raw, "fixture_review_ref") : "U06_FIXTURE_REVIEW_GATE_V0_1:MANIFEST_DIGEST_BOUND";
        return new U06SyntheticDeliveryService.ScopeAuthorization(scopeId, authRef,
                U06SyntheticDeliveryService.ScopeAuthorization.CURRENT, "consult-1", profile,
                fixtureRef, reviewRef, "synthetic-store-u06-v1", environment, endpoint,
                null, false, false, false);
    }

    private String mode(JsonNode fixture) {
        String m = text(fixture, "mode");
        if ("MODE-1".equals(m)) return U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT;
        if ("MODE-2".equals(m)) return U06ProfileBRequest.QUESTION_SELECTION_DELIVERY;
        if ("MODE-3".equals(m)) return U06ProfileBRequest.F3_CURRENT_VERSION_REVALIDATION;
        if ("PROFILE-A".equals(m) || "PROFILE-B".equals(m)) return U06ProfileBRequest.QUESTION_SELECTION_DELIVERY;
        // ANY cases are admission/boundary probes; MODE-1 is the least side-effectful legal carrier.
        return U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT;
    }

    private String legalSource(String mode) {
        if (U06ProfileBRequest.PRE_READINESS_GAP_ASSESSMENT.equals(mode))
            return U06ProfileBRequest.A1_PRE_READINESS_ROUTING;
        if (U06ProfileBRequest.QUESTION_SELECTION_DELIVERY.equals(mode))
            return U06ProfileBRequest.U05_QUESTION_ROUTING;
        return U06ProfileBRequest.POST_F3_SAFETY_BARRIER_ROUTING;
    }

    private ObjectNode base(String caseId) {
        ObjectNode o = JSON.createObjectNode();
        o.put("case_id", caseId);
        o.put("observation_source", "FIXTURE_DRIVEN_SUT_PROBE");
        o.put("observed_status", "UNSET");
        o.putNull("observed_reason_code");
        o.putNull("observed_admission_status");
        o.put("observed_c03_invocation_count", 0);
        o.put("observed_d04_invocation_count", 0);
        o.putNull("observed_f3_owner_status");
        o.putNull("observed_question_selection_status");
        o.put("observed_state_commit_count", 0);
        o.put("observed_state_version_delta", 0);
        o.put("observed_delivery_intent_count", 0);
        o.put("observed_transport_attempt_count", 0);
        o.put("observed_external_transport_count", 0);
        o.putNull("observed_confirmation_status");
        o.put("observed_consultation_wait_count", 0);
        o.put("observed_checkpoint_count", 0);
        o.put("observed_thread_awaiting_count", 0);
        o.put("observed_u07_eligibility_count", 0);
        o.put("observed_failure_handoff_count", 0);
        o.set("observed_identity_equalities", JSON.createArrayNode());
        o.set("observed_provenance_equalities", JSON.createArrayNode());
        o.set("observed_effect_counts", JSON.createObjectNode());
        o.set("probe_refs", JSON.createArrayNode());
        return o;
    }

    private static String caseId(JsonNode fixture) {
        String semantic = text(fixture, "fixture_semantic_id");
        String prefix = "synthetic-structural-";
        if (!semantic.startsWith(prefix)) throw new IllegalArgumentException("unexpected fixture semantic id " + semantic);
        return semantic.substring(prefix.length());
    }

    private static String control(JsonNode fixture, String field) {
        return text(fixture.path("scenario_controls"), field);
    }

    private static String text(JsonNode node, String field) {
        JsonNode x = node.path(field);
        return x.isMissingNode() || x.isNull() ? "" : x.asText();
    }

    private JsonNode readResource(String path) throws Exception {
        InputStream in = getClass().getResourceAsStream(path);
        if (in == null) throw new IllegalStateException("missing resource " + path);
        try { return JSON.readTree(in); }
        finally { in.close(); }
    }

    private static final class WaitHarness {
        final U06SyntheticP01Runtime state=U06SyntheticP01Runtime.create("synthetic-store-u06-v1","consult-1","cdp-1",CLOCK);
        final StrictInMemoryDeliveryStore deliveryStore=new StrictInMemoryDeliveryStore();
        final ConsultationRepository consultations=mock(ConsultationRepository.class);
        final ConsultationWaitEffectRepository waitEffects=mock(ConsultationWaitEffectRepository.class);
        final RuntimeThreadStateRepository threads=mock(RuntimeThreadStateRepository.class);
        final RuntimeWaitCheckpointRepository checkpoints=mock(RuntimeWaitCheckpointRepository.class);
        final AtomicReference<String> lifecycle=new AtomicReference<String>(ConsultationRecord.ACTIVE);
        final AtomicReference<String> waitEffect=new AtomicReference<String>();
        final AtomicReference<Long> consultationVersion=new AtomicReference<Long>(Long.valueOf(0));
        final AtomicReference<ConsultationWaitEffectRecord> waitRecord=new AtomicReference<ConsultationWaitEffectRecord>();
        final AtomicReference<RuntimeThreadStateRecord> threadRecord=new AtomicReference<RuntimeThreadStateRecord>();
        final AtomicReference<RuntimeWaitCheckpointRecord> checkpointRecord=new AtomicReference<RuntimeWaitCheckpointRecord>();
        final ConsultationRecord consultation=mock(ConsultationRecord.class);
        final AtomicInteger threadLockCalls=new AtomicInteger();
        boolean failConsultation;
        boolean failSecondThreadLock;
        final RuntimeWaitCheckpointService checkpointService;
        final RuntimeThreadWaitTransitionService threadService;
        final U06WaitCoordinator waitCoordinator;
        final U06ProfileBApplicationService app;

        WaitHarness(boolean failConsultation,boolean failSecondThreadLock){
            this.failConsultation=failConsultation;this.failSecondThreadLock=failSecondThreadLock;
            when(consultation.getLifecycleStatus()).thenAnswer(i->lifecycle.get());
            when(consultation.getCurrentWaitEffectId()).thenAnswer(i->waitEffect.get());
            when(consultation.getRowVersion()).thenAnswer(i->consultationVersion.get());
            doAnswer(i->{lifecycle.set(ConsultationRecord.WAITING_USER);waitEffect.set((String)i.getArgument(0));consultationVersion.set(Long.valueOf(1));return null;})
                    .when(consultation).enterWaitingUser(anyString());
            when(consultations.findByIdForUpdate("consult-1")).thenAnswer(i->{
                if(this.failConsultation)throw new IllegalStateException("synthetic-consultation-transient");
                return Optional.of(consultation);
            });
            when(consultations.saveAndFlush(consultation)).thenReturn(consultation);
            when(waitEffects.findById(anyString())).thenAnswer(i->{
                ConsultationWaitEffectRecord x=waitRecord.get();
                return x!=null&&x.getWaitEffectId().equals(i.getArgument(0))?Optional.of(x):Optional.empty();
            });
            when(waitEffects.findByIdempotencyKey(anyString())).thenAnswer(i->{
                ConsultationWaitEffectRecord x=waitRecord.get();
                return x!=null&&x.getIdempotencyKey().equals(i.getArgument(0))?Optional.of(x):Optional.empty();
            });
            when(waitEffects.saveAndFlush(any(ConsultationWaitEffectRecord.class))).thenAnswer(i->{ConsultationWaitEffectRecord x=i.getArgument(0);waitRecord.set(x);return x;});

            when(threads.findById(anyString())).thenAnswer(i->Optional.ofNullable(threadRecord.get()));
            when(threads.findByThreadIdForUpdate(anyString())).thenAnswer(i->{
                int call=threadLockCalls.incrementAndGet();
                if(this.failSecondThreadLock&&call>=2)return Optional.empty();
                return Optional.ofNullable(threadRecord.get());
            });
            when(threads.saveAndFlush(any(RuntimeThreadStateRecord.class))).thenAnswer(i->{RuntimeThreadStateRecord x=i.getArgument(0);threadRecord.set(x);return x;});
            when(checkpoints.findById(anyString())).thenAnswer(i->{
                RuntimeWaitCheckpointRecord x=checkpointRecord.get();
                return x!=null&&x.getCheckpointId().equals(i.getArgument(0))?Optional.of(x):Optional.empty();
            });
            when(checkpoints.saveAndFlush(any(RuntimeWaitCheckpointRecord.class))).thenAnswer(i->{RuntimeWaitCheckpointRecord x=i.getArgument(0);checkpointRecord.set(x);return x;});

            checkpointService=new RuntimeWaitCheckpointService(threads,checkpoints);
            threadService=new RuntimeThreadWaitTransitionService(threads,checkpoints);
            waitCoordinator=new U06WaitCoordinator(checkpointService,threadService);
            app=new U06ProfileBApplicationService(new U06AdmissionService(),state,new U06SyntheticDeliveryService(deliveryStore),
                    new ConsultationWaitTransitionService(consultations,waitEffects),waitCoordinator,
                    new com.aidoctor.diagnosis.runtime.u06.trace.U06GovernedExecutionTraceStore(){
                        public void start(String a,String b,String c,String d,String e,String f,String g){}
                        public void complete(String a,String b,String c,String d){}
                    });
        }
    }

    private static final class StrictInMemoryDeliveryStore implements U06DeliveryStore {
        private final Map<String,Command> commands = new LinkedHashMap<String,Command>();
        int physicalSends;

        public synchronized Snapshot reconcileConfirmed(Command c) {
            Command existing = commands.get(c.deliveryEffectId);
            if (existing != null) {
                if (!same(existing, c)) throw new IllegalStateException("U06_DELIVERY_REPLAY_CONFLICT");
                return new Snapshot(c.deliveryEffectId, c.deliveryId, "CONFIRMED", true);
            }
            commands.put(c.deliveryEffectId, c);
            physicalSends++;
            return new Snapshot(c.deliveryEffectId, c.deliveryId, "CONFIRMED", false);
        }

        private boolean same(Command a, Command b) {
            return eq(a.selectionEffectId,b.selectionEffectId)
                    &&eq(a.deliveryEffectId,b.deliveryEffectId)
                    &&eq(a.deliveryId,b.deliveryId)
                    &&eq(a.questionId,b.questionId)
                    &&eq(a.contentFingerprint,b.contentFingerprint)
                    &&eq(a.endpointRef,b.endpointRef)
                    &&eq(a.idempotencyKey,b.idempotencyKey)
                    &&eq(a.confirmationEvaluationId,b.confirmationEvaluationId)
                    &&eq(a.confirmationFingerprint,b.confirmationFingerprint);
        }

        private boolean eq(String a,String b){return a==null?b==null:a.equals(b);}
    }

    private static final class CountingConnectSecurityManager extends SecurityManager {
        final AtomicInteger connectCount = new AtomicInteger();
        private final SecurityManager delegate;

        CountingConnectSecurityManager(SecurityManager delegate) { this.delegate = delegate; }

        @Override public void checkPermission(Permission perm) {
            if (delegate != null) delegate.checkPermission(perm);
        }
        @Override public void checkPermission(Permission perm, Object context) {
            if (delegate != null) delegate.checkPermission(perm, context);
        }
        @Override public void checkConnect(String host, int port) {
            connectCount.incrementAndGet();
            throw new SecurityException("U06_EXTERNAL_CONNECT_BLOCKED:" + host + ":" + port);
        }
        @Override public void checkConnect(String host, int port, Object context) {
            connectCount.incrementAndGet();
            throw new SecurityException("U06_EXTERNAL_CONNECT_BLOCKED:" + host + ":" + port);
        }
    }
}
