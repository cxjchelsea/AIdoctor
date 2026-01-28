package com.aidoctor.trace.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 执行追踪实体
 */
@Entity
@Table(name = "execution_trace", indexes = {
    @Index(name = "idx_cdp_id", columnList = "cdp_id"),
    @Index(name = "idx_event_timestamp", columnList = "event_timestamp"),
    @Index(name = "idx_service", columnList = "service")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionTrace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cdp_id", nullable = false, length = 64)
    private String cdpId;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "service", length = 100)
    private String service;

    @Column(name = "module", length = 100)
    private String module;

    @Column(name = "method", length = 100)
    private String method;

    @Column(name = "step", length = 100)
    private String step;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "duration")
    private Long duration;

    @CreationTimestamp
    @Column(name = "event_timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Lob
    @Column(name = "input_data", columnDefinition = "CLOB")
    private String inputData;

    @Lob
    @Column(name = "output_data", columnDefinition = "CLOB")
    private String outputData;

    @Lob
    @Column(name = "error_message", columnDefinition = "CLOB")
    private String errorMessage;

    @Column(name = "request_url", length = 512)
    private String requestUrl;
}


