package com.aidoctor.diagnosis.dto.capability.c01;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Typed request for the governed C01/U01 capability slice. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class C01U01CapabilityRequest {
    private String userId;
    private String text;
    private String consultationId;
    private String knownSubjectReferenceId;
    private String bindingId;
    private String capabilitySetVersion;
    private String scopeVersion;
    private String contractVersion;
}
