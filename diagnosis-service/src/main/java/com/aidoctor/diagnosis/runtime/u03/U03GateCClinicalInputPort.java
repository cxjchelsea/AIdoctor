package com.aidoctor.diagnosis.runtime.u03;

/**
 * Non-production adapter boundary for loading the accepted clinical fact values
 * consumed by the exact Gate-C-frozen C02 rules.
 */
public interface U03GateCClinicalInputPort {
    U03GateCClinicalInput load(
            U03ExecutionCommand command,
            U03AcceptedEvidenceBinding acceptedEvidenceBinding);
}
