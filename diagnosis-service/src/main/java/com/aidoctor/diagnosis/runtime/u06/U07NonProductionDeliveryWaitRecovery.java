package com.aidoctor.diagnosis.runtime.u06;

import com.aidoctor.contracts.v1.ContractVersion;
import com.aidoctor.contracts.v1.FoundationTypes;
import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.runtime.u06.state.U06StateValues;
import com.aidoctor.diagnosis.state.committer.StateCommitter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Non-production durable intake and crash-resumable question-delivery boundary.
 *
 * Only an independently verified, already confirmed delivery may be persisted.
 * No external delivery is performed here. A persisted receipt is reconciled to
 * CDP through governed StateCommitter and then to a host's durable wait
 * authority; a crash at either boundary can be retried from the receipt.
 *
 * The host must bind WaitAuthority to actual ConsultationWaitTransitionService
 * and U06WaitCoordinator with durable repositories, not in-memory test ports.
 * SQL tables are integration fixtures until deployment migrations are approved.
 */
public final class U07NonProductionDeliveryWaitRecovery {
    public interface DeliveryVerifier {
        boolean verify(Receipt receipt);
    }

    public interface WaitAuthority {
        WaitReceipt establish(Receipt receipt, int committedCdpVersion);
    }

    public static final class Receipt {
        public final U06ProfileBRequest request;
        public final U06SyntheticDecisionBundle decision;
        public final String deliveryId, deliveryEffectId, confirmationRef;

        public Receipt(U06ProfileBRequest request, U06SyntheticDecisionBundle decision,
                String deliveryId, String deliveryEffectId, String confirmationRef) {
            if (request == null || decision == null)
                throw new IllegalArgumentException("U07_DELIVERY_REQUEST_REQUIRED");
            this.request = request;
            this.decision = decision;
            this.deliveryId = require(deliveryId);
            this.deliveryEffectId = require(deliveryEffectId);
            this.confirmationRef = require(confirmationRef);
            if (!U06ProfileBRequest.QUESTION_SELECTION_DELIVERY.equals(request.getMode())
                    || !U06ProfileBRequest.SYNTHETIC_STRUCTURAL_NONPROD.equals(
                            request.getExecutionProfile())
                    || !U06SyntheticDecisionBundle.SELECTED.equals(
                            decision.getQuestionSelectionStatus()))
                throw new IllegalStateException("U07_DELIVERY_NONPRODUCTION_SELECTION_REQUIRED");
        }
    }

    public static final class WaitReceipt {
        public final String waitEffectId, checkpointId, eligibilityId;
        public WaitReceipt(String waitEffectId, String checkpointId, String eligibilityId) {
            this.waitEffectId = require(waitEffectId);
            this.checkpointId = require(checkpointId);
            this.eligibilityId = require(eligibilityId);
        }
    }

    private final CDPRepository repository;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final StateCommitter committer;
    private final DeliveryVerifier verifier;
    private final WaitAuthority waitAuthority;

    public U07NonProductionDeliveryWaitRecovery(CDPRepository repository,
            JdbcTemplate jdbc, TransactionTemplate transactions, StateCommitter committer,
            DeliveryVerifier verifier, WaitAuthority waitAuthority) {
        if (repository == null || jdbc == null || transactions == null || committer == null
                || verifier == null || waitAuthority == null)
            throw new IllegalArgumentException("U07_DELIVERY_RECOVERY_DEPENDENCIES_REQUIRED");
        this.repository = repository;
        this.jdbc = jdbc;
        this.transactions = transactions;
        this.committer = committer;
        this.verifier = verifier;
        this.waitAuthority = waitAuthority;
    }

    /** Store independent immutable delivery proof before touching CDP or WAIT. */
    public void accept(Receipt receipt) {
        if (receipt == null || !verifier.verify(receipt))
            throw new IllegalStateException("U07_DELIVERY_RECEIPT_NOT_TRUSTED");
        transactions.execute(status -> {
            CDP cdp = locked(receipt.request.getCdpId());
            verifyQuestion(cdp, receipt, false);
            List<String[]> rows = readDelivery(receipt);
            if (!rows.isEmpty()) {
                if (rows.size() != 1 || !matches(rows.get(0), receipt))
                    throw new IllegalStateException("U07_DELIVERY_RECEIPT_REPLAY_CONFLICT");
                return null;
            }
            jdbc.update("INSERT INTO u07_question_delivery_evidence "
                    + "(consultation_id, cdp_id, question_id, selection_effect_id, delivery_id, "
                    + "delivery_effect_id, confirmation_ref) VALUES (?,?,?,?,?,?,?)",
                    receipt.request.getConsultationId(), receipt.request.getCdpId(),
                    receipt.decision.getQuestionId(), receipt.decision.getQuestionSelectionEffectId(),
                    receipt.deliveryId, receipt.deliveryEffectId, receipt.confirmationRef);
            return null;
        });
    }

    /**
     * Restart-safe recovery. WAIT is recorded ONLY after CDP delivery readback
     * and a real host WaitAuthority confirmation. Any failure remains resumable
     * from the immutable delivery receipt.
     */
    public WaitReceipt recover(Receipt receipt) {
        if (receipt == null || !verifier.verify(receipt))
            throw new IllegalStateException("U07_DELIVERY_RECEIPT_NOT_TRUSTED");
        int version = transactions.execute(status -> {
            CDP cdp = locked(receipt.request.getCdpId());
            List<String[]> saved = readDelivery(receipt);
            if (saved.size() != 1 || !matches(saved.get(0), receipt))
                throw new IllegalStateException("U07_DELIVERY_RECEIPT_MISSING");
            boolean delivered = verifyQuestion(cdp, receipt, true);
            if (delivered) return cdp.getVersion().intValue();
            int expected = cdp.getVersion().intValue();
            StateTypes.StatePatchOperation question = op("REPLACE",
                    "/patient_state/questions/" + receipt.decision.getQuestionId(),
                    U06StateValues.questionDelivered(receipt.request, receipt.decision,
                            receipt.deliveryEffectId, receipt.deliveryId));
            String parent = U06Ids.hash("u06wait", receipt.request.getConsultationId(),
                    receipt.decision.getQuestionId(), receipt.decision.getQuestionSelectionEffectId(),
                    receipt.deliveryId, receipt.decision.getQuestionContentFingerprint(), "1");
            StateTypes.StatePatchOperation pending = op("ADD",
                    "/patient_state/pending_question",
                    U06StateValues.pending(receipt.decision, parent, receipt.deliveryId));
            StateTypes.CommitResult committed = committer.commit(
                    patch(receipt, expected, question, pending));
            if (committed == null || !"COMMITTED".equals(committed.status)
                    || committed.committedVersion == null
                    || committed.committedVersion.intValue() != expected + 1)
                throw new IllegalStateException("U07_DELIVERY_CDP_COMMIT_FAILED");
            CDP check = locked(receipt.request.getCdpId());
            if (check.getVersion() == null
                    || check.getVersion().intValue() != expected + 1
                    || !verifyQuestion(check, receipt, true))
                throw new IllegalStateException("U07_DELIVERY_CDP_READBACK_MISMATCH");
            return check.getVersion().intValue();
        });

        // An exact durable replay never calls an external wait authority twice.
        WaitReceipt previous = transactions.execute(status -> {
            locked(receipt.request.getCdpId());
            List<String[]> existing = jdbc.query(
                    "SELECT wait_effect_ref, checkpoint_ref, eligibility_ref "
                    + "FROM u07_question_wait_evidence WHERE consultation_id=? AND cdp_id=? "
                    + "AND question_id=?",
                    (rs,n) -> new String[]{rs.getString(1),rs.getString(2),rs.getString(3)},
                    receipt.request.getConsultationId(),receipt.request.getCdpId(),
                    receipt.decision.getQuestionId());
            if (existing.size() > 1)
                throw new IllegalStateException("U07_DELIVERY_WAIT_DUPLICATE");
            return existing.isEmpty() ? null : new WaitReceipt(
                    existing.get(0)[0],existing.get(0)[1],existing.get(0)[2]);
        });
        if (previous != null) return previous;

        // Real durable wait services execute outside the CDP transaction.
        // Failed wait establishment does not erase evidence or claim success.
        WaitReceipt host = waitAuthority.establish(receipt, version);
        if (host == null) throw new IllegalStateException("U07_DELIVERY_WAIT_NOT_CONFIRMED");
        return transactions.execute(status -> {
            locked(receipt.request.getCdpId()); // serialize with CDP mutators
            List<String[]> existing = jdbc.query(
                    "SELECT wait_effect_ref, checkpoint_ref, eligibility_ref "
                    + "FROM u07_question_wait_evidence WHERE consultation_id=? AND cdp_id=? "
                    + "AND question_id=?",
                    (rs,n) -> new String[]{rs.getString(1),rs.getString(2),rs.getString(3)},
                    receipt.request.getConsultationId(), receipt.request.getCdpId(),
                    receipt.decision.getQuestionId());
            if (!existing.isEmpty()) {
                if (existing.size() != 1 || !host.waitEffectId.equals(existing.get(0)[0])
                        || !host.checkpointId.equals(existing.get(0)[1])
                        || !host.eligibilityId.equals(existing.get(0)[2]))
                    throw new IllegalStateException("U07_DELIVERY_WAIT_REPLAY_CONFLICT");
                return host;
            }
            jdbc.update("INSERT INTO u07_question_wait_evidence "
                    + "(consultation_id, cdp_id, question_id, wait_effect_ref, "
                    + "checkpoint_ref, eligibility_ref) VALUES (?,?,?,?,?,?)",
                    receipt.request.getConsultationId(), receipt.request.getCdpId(),
                    receipt.decision.getQuestionId(), host.waitEffectId, host.checkpointId,
                    host.eligibilityId);
            return host;
        });
    }

    private CDP locked(String id) {
        return repository.findByIdWithLock(id)
                .orElseThrow(() -> new IllegalStateException("U07_DELIVERY_CDP_MISSING"));
    }

    private boolean verifyQuestion(CDP cdp, Receipt receipt, boolean allowDelivered) {
        Map<String,Object> patient = cdp.getPatientState();
        String questionId = receipt.decision.getQuestionId();
        if (questionId == null || !questionId.matches("[A-Za-z0-9][A-Za-z0-9._:-]*"))
            throw new IllegalStateException("U07_DELIVERY_QUESTION_ID_INVALID");
        Object raw = patient == null ? null : patient.get("questions");
        Object question = raw instanceof Map<?, ?> ? ((Map<?, ?>)raw).get(questionId) : null;
        if (!(question instanceof Map<?, ?>)
                || !receipt.decision.getQuestionSelectionEffectId().equals(
                        ((Map<?, ?>)question).get("selection_effect_id"))
                || !receipt.decision.getQuestionContentFingerprint().equals(
                        ((Map<?, ?>)question).get("content_fingerprint")))
            throw new IllegalStateException("U07_DELIVERY_SELECTION_NOT_CURRENT");
        Object f3 = patient.get("f3_gap_assessment");
        if (!(f3 instanceof Map<?, ?>)
                || receipt.decision.getF3CanonicalEffectId() == null
                || !receipt.decision.getF3CanonicalEffectId().equals(
                        ((Map<?, ?>) f3).get("f3_canonical_effect_id")))
            throw new IllegalStateException("U07_DELIVERY_F3_NOT_CURRENT");
        String state = (String)((Map<?, ?>)question).get("status");
        if ("SELECTED".equals(state)) {
            if (patient.containsKey("pending_question"))
                throw new IllegalStateException("U07_DELIVERY_PENDING_CONFLICT");
            return false;
        }
        Object pending = patient.get("pending_question");
        if (allowDelivered && "DELIVERED_TO_USER".equals(state)
                && receipt.deliveryId.equals(((Map<?, ?>) question).get("delivery_id"))
                && receipt.deliveryEffectId.equals(((Map<?, ?>) question).get("delivery_effect_id"))
                && pending instanceof Map<?, ?>
                && questionId.equals(((Map<?, ?>) pending).get("question_id"))
                && receipt.deliveryId.equals(((Map<?, ?>) pending).get("delivery_id")))
            return true;
        throw new IllegalStateException("U07_DELIVERY_EXISTING_STATE_CONFLICT");
    }

    private List<String[]> readDelivery(Receipt r) {
        return jdbc.query("SELECT selection_effect_id, delivery_id, delivery_effect_id, confirmation_ref "
                + "FROM u07_question_delivery_evidence WHERE consultation_id=? AND cdp_id=? "
                + "AND question_id=?",
                (rs,n) -> new String[]{rs.getString(1),rs.getString(2),
                        rs.getString(3),rs.getString(4)},
                r.request.getConsultationId(),r.request.getCdpId(),r.decision.getQuestionId());
    }

    private static boolean matches(String[] saved, Receipt r) {
        return r.decision.getQuestionSelectionEffectId().equals(saved[0])
                && r.deliveryId.equals(saved[1]) && r.deliveryEffectId.equals(saved[2])
                && r.confirmationRef.equals(saved[3]);
    }

    private static StateTypes.StatePatchOperation op(String type, String path, Object value) {
        StateTypes.StatePatchOperation o = new StateTypes.StatePatchOperation();
        o.op=type; o.path=path; o.value=value; o.source="RULE_DERIVED"; o.sensitivity="PHI";
        return o;
    }

    private static StateTypes.StatePatch patch(Receipt r, int version,
            StateTypes.StatePatchOperation question, StateTypes.StatePatchOperation pending) {
        String id = U06Ids.hash("u07delivery",r.request.getCdpId(),
                r.decision.getQuestionSelectionEffectId(),r.deliveryEffectId);
        FoundationTypes.ContractEnvelope e = new FoundationTypes.ContractEnvelope();
        e.contractName="StatePatch"; e.contractVersion=ContractVersion.CONTRACT_VERSION;
        e.messageId=id; e.correlationId=r.request.getCorrelationId();
        e.traceId=r.request.getTraceId(); e.createdAt=r.request.getCreatedAt();
        e.producer="diagnosis-service"; e.capabilityId="P01"; e.capabilityVersion="1.0.0";
        StateTypes.StatePatch p = new StateTypes.StatePatch();
        p.contractVersion=ContractVersion.CONTRACT_VERSION; p.envelope=e;
        p.cdpId=r.request.getCdpId(); p.baseVersion=Integer.valueOf(version);
        p.patchId=id; p.idempotencyKey=U06Ids.hash("u07deliveryidem",id);
        p.operations=Arrays.asList(question,pending);
        p.reasonCode="U07_CONFIRMED_QUESTION_DELIVERY";
        p.evidenceRefs=java.util.Collections.singletonList(r.confirmationRef);
        p.producer="diagnosis-service"; p.createdAt=r.request.getCreatedAt();
        return p;
    }

    private static String require(String value) {
        if (value == null || value.trim().isEmpty())
            throw new IllegalArgumentException("U07_DELIVERY_RECEIPT_FIELD_REQUIRED");
        return value;
    }
}
