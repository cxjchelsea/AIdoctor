package com.aidoctor.diagnosis.state.committer;

import com.aidoctor.contracts.v1.ContractVersion;
import com.aidoctor.contracts.v1.FoundationTypes;
import com.aidoctor.contracts.v1.StateTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Manual validation of the StatePatch properties needed at the PBNC-01 POJO
 * boundary. This is deliberately not advertised as a general JSON Schema
 * runtime. In particular, a parsed Java {@code null} cannot distinguish an
 * explicitly supplied JSON null from an absent optional value property.
 */
public final class StatePatchBoundaryValidator {
    private static final Pattern CONTRACT_NAME = Pattern.compile("^[A-Z][A-Za-z0-9]+$");
    private static final Pattern OPAQUE_ID = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]*$");
    private static final Pattern SERVICE_NAME = Pattern.compile("^[a-z][a-z0-9-]*$");
    private static final Pattern SEMVER = Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]+$");
    private static final Pattern REASON = Pattern.compile("^[A-Z][A-Z0-9_]*$");
    private static final Pattern JSON_POINTER =
            Pattern.compile("^/(?:[^~/]|~0|~1)+(?:/(?:[^~/]|~0|~1)+)*$");
    private static final Pattern TIMESTAMP_OFFSET = Pattern.compile(".*(?:Z|[+-][0-9]{2}:[0-9]{2})$");
    private static final BigDecimal MAX_SAFE_INTEGER = new BigDecimal("9007199254740991");
    private static final Set<String> ROOTS = setOf(
            "patient_state", "ddx", "evidence_graph", "workup_plan", "management_plan",
            "triage", "uncertainty", "health_state_assessment", "wellness_plan"
    );
    private static final Set<String> SOURCES = setOf(
            "PATIENT_FACT", "MEDICAL_EVIDENCE", "CLINICIAN_DECISION", "SAFETY_RULE", "TOOL_OUTPUT"
    );
    private static final Set<String> SENSITIVITIES = setOf(
            "PUBLIC", "INTERNAL", "INTERNAL_SENSITIVE", "PHI"
    );
    private static final Set<String> AUDIT_TYPES = setOf(
            "CONTRACT_RECEIVED", "TOOL_INVOKED", "STATE_PATCH_REQUESTED", "STATE_COMMITTED",
            "PATIENT_DELIVERY_CREATED", "REVIEW_DECISION_RECORDED"
    );
    private static final Set<String> ACCESS_LEVELS = setOf(
            "INTERNAL", "RESTRICTED", "SECURITY_REVIEW_REQUIRED"
    );

    public Validation validate(StateTypes.StatePatch patch) {
        if (patch == null) {
            return invalid("StatePatch is required.");
        }
        if (!ContractVersion.CONTRACT_VERSION.equals(patch.contractVersion)) {
            return invalid("StatePatch contract_version must be exactly 1.0.0.");
        }
        Validation envelope = validateEnvelope(patch.envelope);
        if (!envelope.valid) {
            return envelope;
        }
        if (!opaque(patch.cdpId) || !opaque(patch.patchId) || !opaque(patch.idempotencyKey)) {
            return invalid("StatePatch identifiers are malformed.");
        }
        if (patch.baseVersion == null || patch.baseVersion.intValue() < 0) {
            return invalid("base_version must be a non-negative integer.");
        }
        if (patch.operations == null || patch.operations.isEmpty() || patch.operations.size() > 64) {
            return invalid("operations must contain between 1 and 64 items.");
        }
        for (StateTypes.StatePatchOperation operation : patch.operations) {
            Validation operationValidation = validateOperation(operation);
            if (!operationValidation.valid) {
                return operationValidation;
            }
        }
        if (!matches(patch.reasonCode, REASON, 1, 64)) {
            return invalid("reason_code is malformed.");
        }
        if (patch.evidenceRefs == null || patch.evidenceRefs.size() > 64) {
            return invalid("evidence_refs is required and limited to 64 items.");
        }
        Set<String> distinctEvidence = new HashSet<String>();
        for (String evidenceRef : patch.evidenceRefs) {
            if (!opaque(evidenceRef) || !distinctEvidence.add(evidenceRef)) {
                return invalid("evidence_refs must be unique opaque identifiers.");
            }
        }
        if (!serviceName(patch.producer) || !timestamp(patch.createdAt)) {
            return invalid("producer or created_at is malformed.");
        }
        return Validation.valid();
    }

    static boolean validAuditRef(FoundationTypes.AuditRef auditRef) {
        return auditRef != null
                && ContractVersion.CONTRACT_VERSION.equals(auditRef.contractVersion)
                && opaque(auditRef.auditId)
                && AUDIT_TYPES.contains(auditRef.auditType)
                && auditRef.auditVersion != null
                && auditRef.auditVersion.intValue() >= 1
                && timestamp(auditRef.createdAt)
                && ACCESS_LEVELS.contains(auditRef.accessLevel)
                && auditRef.phiCapable != null;
    }

    private Validation validateEnvelope(FoundationTypes.ContractEnvelope envelope) {
        if (envelope == null
                || !matches(envelope.contractName, CONTRACT_NAME, 1, 80)
                || !"StatePatch".equals(envelope.contractName)
                || !ContractVersion.CONTRACT_VERSION.equals(envelope.contractVersion)
                || !opaque(envelope.messageId)
                || !opaque(envelope.correlationId)
                || !opaque(envelope.traceId)
                || !timestamp(envelope.createdAt)
                || !serviceName(envelope.producer)
                || !opaque(envelope.capabilityId)
                || !matches(envelope.capabilityVersion, SEMVER, 1, 32)) {
            return invalid("StatePatch envelope is malformed.");
        }
        return Validation.valid();
    }

    private Validation validateOperation(StateTypes.StatePatchOperation operation) {
        if (operation == null || operation.op == null || operation.op.length() == 0
                || operation.op.length() > 16) {
            return invalid("StatePatch operation is malformed.");
        }
        if (!jsonPointer(operation.path) || !authorizedRoot(operation.path)) {
            return invalid("StatePatch operation path is outside the v1 state roots.");
        }
        if (!SOURCES.contains(operation.source) || !SENSITIVITIES.contains(operation.sensitivity)) {
            return invalid("StatePatch operation source or sensitivity is malformed.");
        }
        if ("REMOVE".equals(operation.op) && operation.value != null) {
            return invalid("REMOVE cannot carry a non-null value.");
        }
        if (!controlledValue(operation.value, 4000)
                || !controlledValue(operation.expectedCurrentValue, 4000)) {
            return invalid("StatePatch operation value is outside the v1 controlled-value boundary.");
        }
        return Validation.valid();
    }

    private static boolean controlledValue(Object value, int stringLimit) {
        if (value == null || value instanceof Boolean) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).length() <= stringLimit;
        }
        if (value instanceof Number) {
            try {
                BigDecimal number = new BigDecimal(value.toString());
                return number.abs().compareTo(MAX_SAFE_INTEGER) <= 0;
            } catch (NumberFormatException exception) {
                return false;
            }
        }
        if (value instanceof List<?>) {
            List<?> values = (List<?>) value;
            if (values.size() > 64) {
                return false;
            }
            for (Object item : values) {
                if (item instanceof List<?> || !controlledValue(item, 1000)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static boolean authorizedRoot(String path) {
        int slash = path.indexOf('/', 1);
        String root = slash < 0 ? path.substring(1) : path.substring(1, slash);
        return slash > 1 && ROOTS.contains(root);
    }

    private static boolean jsonPointer(String value) {
        return matches(value, JSON_POINTER, 1, 256);
    }

    private static boolean opaque(String value) {
        return matches(value, OPAQUE_ID, 1, 128);
    }

    private static boolean serviceName(String value) {
        return matches(value, SERVICE_NAME, 1, 80);
    }

    private static boolean timestamp(String value) {
        if (value == null || value.length() < 1 || value.length() > 64 || !TIMESTAMP_OFFSET.matcher(value).matches()) {
            return false;
        }
        try {
            OffsetDateTime.parse(value);
            return true;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    private static boolean matches(String value, Pattern pattern, int minimum, int maximum) {
        return value != null
                && value.length() >= minimum
                && value.length() <= maximum
                && pattern.matcher(value).matches();
    }

    private static Set<String> setOf(String... values) {
        return new HashSet<String>(Arrays.asList(values));
    }

    private static Validation invalid(String message) {
        return Validation.invalid(message);
    }

    public static final class Validation {
        public final boolean valid;
        public final String message;

        private Validation(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        static Validation valid() {
            return new Validation(true, null);
        }

        static Validation invalid(String message) {
            return new Validation(false, message);
        }
    }
}
