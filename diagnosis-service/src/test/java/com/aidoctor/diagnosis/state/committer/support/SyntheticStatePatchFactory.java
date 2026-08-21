package com.aidoctor.diagnosis.state.committer.support;

import com.aidoctor.contracts.v1.ContractVersion;
import com.aidoctor.contracts.v1.FoundationTypes;
import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.state.committer.fakes.SyntheticCapabilityPolicyFake;
import com.aidoctor.diagnosis.state.committer.fakes.SyntheticFieldPermissionFake;
import com.aidoctor.diagnosis.state.committer.fakes.SyntheticSourceValidationFake;

import java.util.ArrayList;
import java.util.List;

/**
 * Already-parsed synthetic StatePatch factory. Values are non-clinical.
 */
public final class SyntheticStatePatchFactory {
    public static final String CDP_ID = "synthetic-cdp-001";
    public static final String FIXED_AT = "2026-08-21T06:00:00Z";
    public static final String VALUE = "alpha";

    private SyntheticStatePatchFactory() {
    }

    public static StateTypes.StatePatch valid(int baseVersion, String idempotencyKey, String patchId) {
        return patch(
                CDP_ID,
                baseVersion,
                idempotencyKey,
                patchId,
                SyntheticCapabilityPolicyFake.AUTHORIZED_ID,
                singleOperation(SyntheticFieldPermissionFake.AUTHORIZED_PATH, SyntheticSourceValidationFake.AUTHORIZED_SOURCE)
        );
    }

    public static StateTypes.StatePatch withCapability(String capabilityId, int baseVersion) {
        return patch(
                CDP_ID,
                baseVersion,
                "synthetic-idem-capability",
                "synthetic-patch-capability",
                capabilityId,
                singleOperation(SyntheticFieldPermissionFake.AUTHORIZED_PATH, SyntheticSourceValidationFake.AUTHORIZED_SOURCE)
        );
    }

    public static StateTypes.StatePatch withPath(String path, int baseVersion) {
        return patch(
                CDP_ID,
                baseVersion,
                "synthetic-idem-field",
                "synthetic-patch-field",
                SyntheticCapabilityPolicyFake.AUTHORIZED_ID,
                singleOperation(path, SyntheticSourceValidationFake.AUTHORIZED_SOURCE)
        );
    }

    public static StateTypes.StatePatch withSource(String source, int baseVersion) {
        return patch(
                CDP_ID,
                baseVersion,
                "synthetic-idem-source",
                "synthetic-patch-source",
                SyntheticCapabilityPolicyFake.AUTHORIZED_ID,
                singleOperation(SyntheticFieldPermissionFake.AUTHORIZED_PATH, source)
        );
    }

    public static StateTypes.StatePatch withCdp(String cdpId, int baseVersion) {
        return patch(
                cdpId,
                baseVersion,
                "synthetic-idem-cdp",
                "synthetic-patch-cdp",
                SyntheticCapabilityPolicyFake.AUTHORIZED_ID,
                singleOperation(SyntheticFieldPermissionFake.AUTHORIZED_PATH, SyntheticSourceValidationFake.AUTHORIZED_SOURCE)
        );
    }

    public static StateTypes.StatePatch multiOperation(int baseVersion, String secondPath) {
        List<StateTypes.StatePatchOperation> operations = new ArrayList<StateTypes.StatePatchOperation>();
        operations.add(operation(SyntheticFieldPermissionFake.AUTHORIZED_PATH, SyntheticSourceValidationFake.AUTHORIZED_SOURCE));
        operations.add(operation(secondPath, SyntheticSourceValidationFake.AUTHORIZED_SOURCE));
        return patch(
                CDP_ID,
                baseVersion,
                "synthetic-idem-multi",
                "synthetic-patch-multi",
                SyntheticCapabilityPolicyFake.AUTHORIZED_ID,
                operations
        );
    }

    public static StateTypes.StatePatch patch(
            String cdpId,
            int baseVersion,
            String idempotencyKey,
            String patchId,
            String capabilityId,
            List<StateTypes.StatePatchOperation> operations
    ) {
        StateTypes.StatePatch patch = new StateTypes.StatePatch();
        patch.contractVersion = ContractVersion.CONTRACT_VERSION;
        patch.envelope = envelope(capabilityId, patchId);
        patch.cdpId = cdpId;
        patch.baseVersion = Integer.valueOf(baseVersion);
        patch.patchId = patchId;
        patch.idempotencyKey = idempotencyKey;
        patch.operations = operations;
        patch.reasonCode = "SYNTHETIC_TEST_WRITE";
        patch.evidenceRefs = new ArrayList<String>();
        patch.producer = "diagnosis-service";
        patch.createdAt = FIXED_AT;
        return patch;
    }

    private static FoundationTypes.ContractEnvelope envelope(String capabilityId, String patchId) {
        FoundationTypes.ContractEnvelope envelope = new FoundationTypes.ContractEnvelope();
        envelope.contractName = "StatePatch";
        envelope.contractVersion = ContractVersion.CONTRACT_VERSION;
        envelope.messageId = "synthetic-msg-" + patchId;
        envelope.correlationId = "synthetic-corr-001";
        envelope.traceId = "synthetic-trace-001";
        envelope.createdAt = FIXED_AT;
        envelope.producer = "diagnosis-service";
        envelope.capabilityId = capabilityId;
        envelope.capabilityVersion = SyntheticCapabilityPolicyFake.AUTHORIZED_VERSION;
        return envelope;
    }

    private static List<StateTypes.StatePatchOperation> singleOperation(String path, String source) {
        List<StateTypes.StatePatchOperation> operations = new ArrayList<StateTypes.StatePatchOperation>();
        operations.add(operation(path, source));
        return operations;
    }

    private static StateTypes.StatePatchOperation operation(String path, String source) {
        StateTypes.StatePatchOperation operation = new StateTypes.StatePatchOperation();
        operation.op = "REPLACE";
        operation.path = path;
        operation.value = VALUE;
        operation.expectedCurrentValue = null;
        operation.source = source;
        operation.sensitivity = "PUBLIC";
        return operation;
    }
}
