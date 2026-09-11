package com.aidoctor.diagnosis.dto.trace;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 执行追踪事件。
 *
 * <p>Foundation-0 extends the event with optional governance/runtime
 * correlation references. These fields identify already-governed objects;
 * they do not turn Trace into Clinical Truth and should not carry full PHI
 * payloads by default.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionTraceEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Existing clinical aggregate identifier. */
    private String cdpId;
    /** Existing trace identifier. */
    private String traceId;

    /** Foundation-0 correlation references. */
    private String consultationId;
    private String eventId;
    private String threadId;
    private String runId;
    private String unitId;
    private String capabilityCallId;
    private String decisionId;
    private String proposalId;
    private String commitResultRef;
    private String checkpointId;
    private String deliveryId;
    private Integer clinicalStateVersion;

    private String type;
    private String service;
    private String module;
    private String method;
    private String step;
    private String status;
    private Long duration;
    private Long timestamp;
    private Object input;
    private Object output;
    private String errorMessage;
    private String url;
    private Map<String, Object> attributes;
}
