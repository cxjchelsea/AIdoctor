package com.aidoctor.diagnosis.state.committer.ports;

import com.aidoctor.contracts.v1.FoundationTypes;

/**
 * Fake / non-production audit reference emitter. Not AuditTrail SoR.
 * A failure after the repository authoritative commit must not downgrade the
 * already committed result; the core supplies a synthetic committed fallback.
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
