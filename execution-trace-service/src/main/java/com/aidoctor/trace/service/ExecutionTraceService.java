package com.aidoctor.trace.service;

import com.aidoctor.trace.dto.ExecutionTraceEvent;
import com.aidoctor.trace.dto.TraceSummary;
import com.aidoctor.trace.entity.ExecutionTrace;
import com.aidoctor.trace.repository.ExecutionTraceRepository;
import com.aidoctor.trace.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 执行追踪服务
 */
@Slf4j
@Service
public class ExecutionTraceService {

    @Autowired
    private ExecutionTraceRepository traceRepository;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    @Autowired(required = false)
    private ExecutionTraceEventPublisher eventPublisher;

    @Autowired
    private JsonUtil jsonUtil;

    @Value("${trace.cdp-update-url:http://localhost:8084/api/v1/cdp/update-trace-summary}")
    private String cdpUpdateUrl;

    @Value("${execution.trace.data-retention-days:30}")
    private int dataRetentionDays;

    @Value("${trace.cdp-update-delay-ms:500}")
    private long cdpUpdateDelayMs;

    // 用于防抖：记录每个CDP的最后更新时间
    private final Map<String, Long> lastUpdateTime = new ConcurrentHashMap<>();
    // 用于防抖：记录每个CDP的待更新任务
    private final Map<String, java.util.TimerTask> pendingUpdates = new ConcurrentHashMap<>();
    private final java.util.Timer updateTimer = new java.util.Timer("CDP-Update-Timer", true);

    /**
     * 记录追踪事件（异步）
     */
    @Async("traceExecutor")
    public void recordEvent(ExecutionTraceEvent event) {
        try {
            if (event.getCdpId() == null) {
                return;
            }

            ExecutionTrace trace = convertToEntity(event);
            traceRepository.save(trace);

            // 使用防抖机制，减少CDP更新频率（性能优化）
            scheduleCDPUpdate(event.getCdpId());

            if (eventPublisher != null) {
                eventPublisher.publish(event);
            }

        } catch (Exception e) {
            log.error("记录追踪事件失败: cdpId={}, eventType={}",
                event.getCdpId(), event.getEventType(), e);
        }
    }

    /**
     * 根据CDP ID查询追踪记录
     */
    public List<ExecutionTrace> getTracesByCdpId(String cdpId) {
        return traceRepository.findByCdpIdOrderByTimestampAsc(cdpId);
    }

    /**
     * 获取CDP的追踪摘要
     */
    public TraceSummary getTraceSummary(String cdpId) {
        List<ExecutionTrace> traces = traceRepository.findByCdpIdOrderByTimestampAsc(cdpId);
        return buildTraceSummary(traces);
    }

    /**
     * 获取所有有追踪记录的 CDP ID 列表（包含最新时间戳）
     */
    public List<Map<String, Object>> getAllCdpIds() {
        List<String> cdpIds = traceRepository.findAllDistinctCdpIds();
        return cdpIds.stream().map(cdpId -> {
            Map<String, Object> info = new HashMap<>();
            info.put("cdpId", cdpId);
            LocalDateTime latestTimestamp = traceRepository.findLatestTimestampByCdpId(cdpId);
            if (latestTimestamp != null) {
                info.put("latestTimestamp", latestTimestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            }
            long traceCount = traceRepository.countByCdpId(cdpId);
            info.put("traceCount", traceCount);
            return info;
        }).collect(Collectors.toList());
    }

    /**
     * 使用防抖机制调度CDP更新（性能优化：减少更新频率）
     * 如果距离上次更新不足delayMs，则延迟更新；否则立即更新
     */
    private void scheduleCDPUpdate(String cdpId) {
        long now = System.currentTimeMillis();
        Long lastUpdate = lastUpdateTime.get(cdpId);
        
        // 取消之前的待更新任务
        java.util.TimerTask oldTask = pendingUpdates.remove(cdpId);
        if (oldTask != null) {
            oldTask.cancel();
        }
        
        // 如果距离上次更新超过延迟时间，立即更新
        if (lastUpdate == null || (now - lastUpdate) >= cdpUpdateDelayMs) {
            updateCDPTraceSummary(cdpId);
            lastUpdateTime.put(cdpId, now);
        } else {
            // 否则，延迟更新
            long delay = cdpUpdateDelayMs - (now - lastUpdate);
            java.util.TimerTask task = new java.util.TimerTask() {
                @Override
                public void run() {
                    updateCDPTraceSummary(cdpId);
                    lastUpdateTime.put(cdpId, System.currentTimeMillis());
                    pendingUpdates.remove(cdpId);
                }
            };
            pendingUpdates.put(cdpId, task);
            updateTimer.schedule(task, delay);
        }
    }

    /**
     * 更新CDP的执行追踪摘要
     */
    @Async("traceExecutor")
    private void updateCDPTraceSummary(String cdpId) {
        try {
            List<ExecutionTrace> traces = traceRepository.findByCdpIdOrderByTimestampAsc(cdpId);
            Map<String, Object> summary = buildTraceSummary(traces).toSummaryMap();

            // 调用diagnosis-service的API更新CDP
            if (restTemplate != null) {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("cdpId", cdpId);
                requestBody.put("executionTrace", summary);
                restTemplate.postForEntity(cdpUpdateUrl, requestBody, Void.class);
            } else {
                log.warn("RestTemplate未注入，无法更新diagnosis-service的CDP追踪摘要。请确保diagnosis-service的URL配置正确。");
            }

        } catch (Exception e) {
            log.error("更新CDP追踪摘要失败: cdpId={}", cdpId, e);
        }
    }

    /**
     * 构建追踪摘要
     */
    private TraceSummary buildTraceSummary(List<ExecutionTrace> traces) {
        if (traces.isEmpty()) {
            return TraceSummary.builder()
                .totalDuration(0L)
                .serviceCalls(Collections.emptyMap())
                .steps(Collections.emptyList())
                .errorCount(0L)
                .build();
        }

        List<TraceSummary.StepSummary> stepSummaries = new ArrayList<>();
        Map<String, List<ExecutionTrace>> stepEvents = traces.stream()
            .filter(t -> t.getStep() != null && !t.getStep().isEmpty())
            .collect(Collectors.groupingBy(ExecutionTrace::getStep));

        // Sort steps by their first appearance
        List<String> sortedStepNames = traces.stream()
            .filter(t -> t.getStep() != null && !t.getStep().isEmpty())
            .map(ExecutionTrace::getStep)
            .distinct()
            .collect(Collectors.toList());

        for (String stepName : sortedStepNames) {
            List<ExecutionTrace> eventsInStep = stepEvents.get(stepName);
            if (eventsInStep != null && !eventsInStep.isEmpty()) {
                LocalDateTime startTime = eventsInStep.stream()
                    .map(ExecutionTrace::getTimestamp)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);
                Set<String> services = eventsInStep.stream()
                    .map(ExecutionTrace::getService)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
                stepSummaries.add(TraceSummary.StepSummary.builder()
                    .step(stepName)
                    .startTime(startTime != null ? startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null)
                    .services(new ArrayList<>(services))
                    .build());
            }
        }

        Map<String, Long> serviceCallCounts = traces.stream()
            .filter(t -> t.getService() != null && t.getEventType().startsWith("SERVICE_CALL"))
            .collect(Collectors.groupingBy(ExecutionTrace::getService, Collectors.counting()));

        Optional<LocalDateTime> firstTimestamp = traces.stream().map(ExecutionTrace::getTimestamp).min(LocalDateTime::compareTo);
        Optional<LocalDateTime> lastTimestamp = traces.stream().map(ExecutionTrace::getTimestamp).max(LocalDateTime::compareTo);
        Long totalDuration = 0L;
        if (firstTimestamp.isPresent() && lastTimestamp.isPresent()) {
            totalDuration = Duration.between(firstTimestamp.get(), lastTimestamp.get()).toMillis();
        }

        long errorCount = traces.stream().filter(t -> "ERROR".equals(t.getStatus())).count();

        return TraceSummary.builder()
            .totalDuration(totalDuration)
            .serviceCalls(serviceCallCounts)
            .steps(stepSummaries)
            .errorCount(errorCount)
            .build();
    }

    /**
     * 转换事件为实体
     */
    private ExecutionTrace convertToEntity(ExecutionTraceEvent event) {
        // 确保 eventType 不为 null（如果为 null，使用默认值）
        String eventType = event.getEventType();
        if (eventType == null || eventType.isEmpty()) {
            log.warn("事件类型为空，使用默认值: cdpId={}", event.getCdpId());
            eventType = "UNKNOWN";
        }
        
        return ExecutionTrace.builder()
            .cdpId(event.getCdpId())
            .traceId(event.getTraceId())
            .eventType(eventType)
            .service(event.getService())
            .module(event.getModule())
            .method(event.getMethod())
            .step(event.getStep())
            .status(event.getStatus())
            .duration(event.getDuration())
            .timestamp(LocalDateTime.ofInstant(
                Instant.ofEpochMilli(event.getTimestamp() != null ? event.getTimestamp() : System.currentTimeMillis()),
                ZoneId.systemDefault()
            ))
            .inputData(event.getInput() != null ? jsonUtil.toJson(event.getInput()) : null)
            .outputData(event.getOutput() != null ? jsonUtil.toJson(event.getOutput()) : null)
            .errorMessage(event.getErrorMessage())
            .requestUrl(event.getUrl())
            .build();
    }

    /**
     * 定时清理旧数据
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    @Transactional
    public void cleanupOldTraces() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(dataRetentionDays);
        List<ExecutionTrace> oldTraces = traceRepository.findByTimestampBefore(cutoff);
        if (!oldTraces.isEmpty()) {
            traceRepository.deleteAll(oldTraces);
            log.info("清理了 {} 条 {} 天前的旧追踪数据", oldTraces.size(), dataRetentionDays);
        }
    }
}


