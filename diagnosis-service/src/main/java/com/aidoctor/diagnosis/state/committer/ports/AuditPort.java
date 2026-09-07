package com.aidoctor.diagnosis.state.committer.ports;

import com.aidoctor.contracts.v1.FoundationTypes;

/**
 * Pre-commit audit authorization/evidence gate for the mechanical core.
 * Implementations must return a complete Shared Contracts v1 AuditRef before
 * repository mutation is attempted. This port is not a claim of durable or
 * transactional AuditTrail persistence and is not production wiring.
 */
public interface AuditPort {

    FoundationTypes.AuditRef record(AuditCommand command);

    final class AuditCommand {
        public final String auditType;
        public final String patchId;
        public final String cdpId;
        public final String status;
        public final String reasonCode;

        public AuditCommand(
                String auditType,
                String patchId,
                String cdpId,
                String status,
                String reasonCode
        ) {
            this.auditType = auditType;
            this.patchId = patchId;
            this.cdpId = cdpId;
            this.status = status;
            this.reasonCode = reasonCode;
        }
    }
}
