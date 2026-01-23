package com.aidoctor.trace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 执行追踪事件DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionTraceEvent {
    private String cdpId;
    private String traceId;
    private String eventType; // e.g., SERVICE_CALL_START, SERVICE_CALL_END, STEP_START, STEP_END, FEIGN_CALL_START, FEIGN_CALL_END
    private String service;
    private String module;
    private String method;
    private String step; // For orchestration steps
    private String status; // e.g., IN_PROGRESS, SUCCESS, ERROR
    private Long duration; // in milliseconds
    private Long timestamp;
    private Object input; // Sanitized input data
    private Object output; // Sanitized output data
    private String errorMessage;
    private String url; // For Feign calls
}


