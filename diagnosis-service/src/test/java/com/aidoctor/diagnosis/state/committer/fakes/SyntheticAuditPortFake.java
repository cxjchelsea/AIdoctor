package com.aidoctor.diagnosis.state.committer.fakes;

import com.aidoctor.contracts.v1.ContractVersion;
import com.aidoctor.contracts.v1.FoundationTypes;
import com.aidoctor.diagnosis.state.committer.ports.AuditPort;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class SyntheticAuditPortFake implements AuditPort {
    private final Clock clock;
    private final AtomicInteger sequence = new AtomicInteger(1);
    private final List<AuditCommand> commands = new ArrayList<AuditCommand>();
    private boolean failNext;

    public SyntheticAuditPortFake(Clock clock) {
        this.clock = clock;
    }

    public void failNext() {
        failNext = true;
    }

    public List<AuditCommand> commands() {
        return commands;
    }

    @Override
    public FoundationTypes.AuditRef record(AuditCommand command) {
        commands.add(command);
        if (failNext) {
            failNext = false;
            throw new IllegalStateException("synthetic audit infrastructure failure");
        }
        FoundationTypes.AuditRef auditRef = new FoundationTypes.AuditRef();
        auditRef.contractVersion = ContractVersion.CONTRACT_VERSION;
        auditRef.auditId = "synthetic-audit-" + sequence.getAndIncrement();
        auditRef.auditType = command.auditType;
        auditRef.auditVersion = Integer.valueOf(1);
        auditRef.createdAt = clock.instant().toString();
        auditRef.accessLevel = "INTERNAL";
        auditRef.phiCapable = Boolean.FALSE;
        return auditRef;
    }
}
