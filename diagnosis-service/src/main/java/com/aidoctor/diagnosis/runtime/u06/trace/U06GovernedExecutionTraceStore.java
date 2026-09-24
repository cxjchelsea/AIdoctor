package com.aidoctor.diagnosis.runtime.u06.trace;
public interface U06GovernedExecutionTraceStore{
 void start(String traceId,String consultationId,String admissionId,String mode,String executionProfile,String payloadFingerprint,String createdAt);
 void complete(String traceId,String lifecycleStatus,String outcomeStatus,String updatedAt);
}
