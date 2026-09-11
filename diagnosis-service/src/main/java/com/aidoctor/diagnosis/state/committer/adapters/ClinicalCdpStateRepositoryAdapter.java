package com.aidoctor.diagnosis.state.committer.adapters;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.repository.CDPRepository;
import com.aidoctor.diagnosis.service.cdp.CDPVersionService;
import com.aidoctor.diagnosis.state.committer.ports.StateRepositoryPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Authoritative Clinical CDP adapter for the StateCommitter repository port.
 *
 * <p>This adapter is intentionally narrow. It connects the already existing
 * StateCommitter admission/idempotency/version machinery to the real CDP row
 * and its version history. It does not expand the diagnosis-service shared
 * contract boundary and it never stringifies structured clinical objects to
 * bypass that boundary. Unsupported paths fail closed before the CDP is
 * mutated.</p>
 */
@Component
public class ClinicalCdpStateRepositoryAdapter implements StateRepositoryPort {
    static final String CDP_NOT_FOUND = "CDP_NOT_FOUND";
    static final String UNSUPPORTED_CLINICAL_PATH = "UNSUPPORTED_CLINICAL_PATH";
    static final String INVALID_CLINICAL_OPERATION = "INVALID_CLINICAL_OPERATION";

    private final CDPRepository cdpRepository;
    private final CDPVersionService cdpVersionService;
    private final ObjectMapper objectMapper;

    public ClinicalCdpStateRepositoryAdapter(
            CDPRepository cdpRepository,
            CDPVersionService cdpVersionService,
            ObjectMapper objectMapper
    ) {
        this.cdpRepository = cdpRepository;
        this.cdpVersionService = cdpVersionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public int readCurrentVersion(String cdpId) {
        Optional<CDP> cdp = cdpRepository.findById(cdpId);
        if (!cdp.isPresent()) {
            throw new IllegalStateException("CDP not found: " + cdpId);
        }
        return safeVersion(cdp.get());
    }

    /**
     * Locks, pre-validates, snapshots and mutates the authoritative CDP in one
     * transaction. No CDP mutation occurs when the base version is stale or an
     * operation cannot be represented by the current CDP aggregate.
     */
    @Override
    @Transactional
    public AtomicCommitOutcome attemptAtomicCommit(AtomicCommitCommand command) {
        Optional<CDP> locked = cdpRepository.findByIdWithLock(command.cdpId);
        if (!locked.isPresent()) {
            return AtomicCommitOutcome.failedNonRetryable(CDP_NOT_FOUND, "Authoritative CDP does not exist.");
        }

        CDP cdp = locked.get();
        int currentVersion = safeVersion(cdp);
        if (currentVersion != command.expectedCurrentVersion) {
            return AtomicCommitOutcome.conflict(command.expectedCurrentVersion, currentVersion);
        }

        WorkingState working;
        try {
            working = WorkingState.from(cdp, objectMapper);
            for (StateTypes.StatePatchOperation operation : command.patch.operations) {
                apply(operation, working);
            }
        } catch (UnsupportedClinicalMutation exception) {
            return AtomicCommitOutcome.failedNonRetryable(exception.code, exception.getMessage());
        } catch (RuntimeException exception) {
            return AtomicCommitOutcome.failedRetryable("CDP_ADAPTER_PREVALIDATION_FAILED",
                    "Clinical CDP mutation could not be pre-validated.");
        }

        cdpVersionService.createVersion(cdp);
        working.writeTo(cdp);
        cdp.setVersion(Math.addExact(currentVersion, 1));
        cdpRepository.save(cdp);
        return AtomicCommitOutcome.committed(currentVersion);
    }

    private void apply(StateTypes.StatePatchOperation operation, WorkingState working) {
        if (operation == null) {
            throw invalid("Clinical operation is required.");
        }
        String[] tokens = decode(operation.path);
        if (tokens.length < 2) {
            throw unsupported("Clinical path must include a root and a leaf.");
        }

        Map<String, Object> root = working.objectRoot(tokens[0]);
        Map<String, Object> parent = parent(root, tokens);
        String leaf = tokens[tokens.length - 1];
        boolean exists = parent.containsKey(leaf);

        if ("ADD".equals(operation.op)) {
            if (exists || operation.value == null) {
                throw invalid("ADD requires a missing target and a non-null value.");
            }
            parent.put(leaf, copyValue(operation.value));
            return;
        }
        if ("REPLACE".equals(operation.op)) {
            if (!exists || operation.value == null) {
                throw invalid("REPLACE requires an existing target and a non-null value.");
            }
            parent.put(leaf, copyValue(operation.value));
            return;
        }
        if ("REMOVE".equals(operation.op)) {
            if (!exists) {
                throw invalid("REMOVE target is missing.");
            }
            parent.remove(leaf);
            return;
        }
        throw invalid("Unsupported clinical operation: " + operation.op);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parent(Map<String, Object> root, String[] tokens) {
        Map<String, Object> current = root;
        for (int index = 1; index < tokens.length - 1; index++) {
            Object child = current.get(tokens[index]);
            if (!(child instanceof Map<?, ?>)) {
                throw unsupported("Clinical path parent is missing or is not an object: " + tokens[index]);
            }
            current = (Map<String, Object>) child;
        }
        return current;
    }

    private Object copyValue(Object value) {
        return objectMapper.convertValue(value, Object.class);
    }

    private static String[] decode(String pointer) {
        if (pointer == null || !pointer.startsWith("/")) {
            throw unsupported("Clinical path is not a JSON pointer.");
        }
        String[] tokens = pointer.substring(1).split("/", -1);
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = tokens[i].replace("~1", "/").replace("~0", "~");
        }
        return tokens;
    }

    private static int safeVersion(CDP cdp) {
        if (cdp.getVersion() == null || cdp.getVersion() < 0) {
            throw new IllegalStateException("CDP version is missing or invalid: " + cdp.getId());
        }
        return cdp.getVersion();
    }

    private static UnsupportedClinicalMutation unsupported(String message) {
        return new UnsupportedClinicalMutation(UNSUPPORTED_CLINICAL_PATH, message);
    }

    private static UnsupportedClinicalMutation invalid(String message) {
        return new UnsupportedClinicalMutation(INVALID_CLINICAL_OPERATION, message);
    }

    private static final class UnsupportedClinicalMutation extends RuntimeException {
        private final String code;

        private UnsupportedClinicalMutation(String code, String message) {
            super(message);
            this.code = code;
        }
    }

    private static final class WorkingState {
        private final Map<String, Object> patientState;
        private final Map<String, Object> triage;
        private final Map<String, Object> uncertainty;
        private final Map<String, Object> healthStateAssessment;
        private final Map<String, Object> wellnessPlan;

        private WorkingState(
                Map<String, Object> patientState,
                Map<String, Object> triage,
                Map<String, Object> uncertainty,
                Map<String, Object> healthStateAssessment,
                Map<String, Object> wellnessPlan
        ) {
            this.patientState = patientState;
            this.triage = triage;
            this.uncertainty = uncertainty;
            this.healthStateAssessment = healthStateAssessment;
            this.wellnessPlan = wellnessPlan;
        }

        static WorkingState from(CDP cdp, ObjectMapper mapper) {
            return new WorkingState(
                    copyMap(cdp.getPatientState(), mapper),
                    copyMap(cdp.getTriage(), mapper),
                    copyMap(cdp.getUncertainty(), mapper),
                    copyMap(cdp.getHealthStateAssessment(), mapper),
                    copyMap(cdp.getWellnessPlan(), mapper));
        }

        Map<String, Object> objectRoot(String root) {
            if ("patient_state".equals(root)) return patientState;
            if ("triage".equals(root)) return triage;
            if ("uncertainty".equals(root)) return uncertainty;
            if ("health_state_assessment".equals(root)) return healthStateAssessment;
            if ("wellness_plan".equals(root)) return wellnessPlan;
            throw unsupported("CDP root is not safely writable by Foundation-0: " + root);
        }

        void writeTo(CDP cdp) {
            cdp.setPatientState(patientState);
            cdp.setTriage(triage);
            cdp.setUncertainty(uncertainty);
            cdp.setHealthStateAssessment(healthStateAssessment);
            cdp.setWellnessPlan(wellnessPlan);
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> copyMap(Map<String, Object> source, ObjectMapper mapper) {
            Map<String, Object> safe = source == null ? new LinkedHashMap<String, Object>() : source;
            return mapper.convertValue(safe, LinkedHashMap.class);
        }
    }
}
