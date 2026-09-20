package com.aidoctor.diagnosis.runtime.u04;

import com.aidoctor.diagnosis.runtime.u03.U03OutboundHandoff;

/** Accepted U04 consumer input. This is not yet a Safety Gate decision. */
public final class U04AdmittedInput {
    private final U03OutboundHandoff handoff;
    private final int currentClinicalStateVersion;
    private final U04ScopeContext scopeContext;
    private final String environmentId;

    U04AdmittedInput(
            U03OutboundHandoff handoff,
            int currentClinicalStateVersion,
            U04ScopeContext scopeContext,
            String environmentId) {
        this.handoff = handoff;
        this.currentClinicalStateVersion = currentClinicalStateVersion;
        this.scopeContext = scopeContext;
        this.environmentId = environmentId;
    }

    public U03OutboundHandoff getHandoff() { return handoff; }
    public int getCurrentClinicalStateVersion() { return currentClinicalStateVersion; }
    public U04ScopeContext getScopeContext() { return scopeContext; }
    public String getEnvironmentId() { return environmentId; }
}
