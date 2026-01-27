package com.aidoctor.trace.controller;

import com.aidoctor.trace.dto.ExecutionTraceEvent;
import com.aidoctor.trace.dto.TraceSummary;
import com.aidoctor.trace.entity.ExecutionTrace;
import com.aidoctor.trace.service.ExecutionTraceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 执行追踪控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/trace")
public class ExecutionTraceController {

    @Autowired
    private ExecutionTraceService traceService;

    /**
     * 接收追踪事件（Java和Python服务都调用这个接口）
     */
    @PostMapping("/events")
    public ResponseEntity<Void> recordEvent(@RequestBody ExecutionTraceEvent event) {
        traceService.recordEvent(event);
        return ResponseEntity.ok().build();
    }

    /**
     * 查询CDP的执行追踪
     */
    @GetMapping("/cdp/{cdpId}")
    public ResponseEntity<List<ExecutionTrace>> getTraceByCdpId(@PathVariable String cdpId) {
        List<ExecutionTrace> traces = traceService.getTracesByCdpId(cdpId);
        return ResponseEntity.ok(traces);
    }

    /**
     * 获取CDP的追踪摘要
     */
    @GetMapping("/cdp/{cdpId}/summary")
    public ResponseEntity<TraceSummary> getTraceSummary(@PathVariable String cdpId) {
        TraceSummary summary = traceService.getTraceSummary(cdpId);
        return ResponseEntity.ok(summary);
    }

    /**
     * 获取所有有追踪记录的 CDP ID 列表
     */
    @GetMapping("/cdps")
    public ResponseEntity<List<Map<String, Object>>> getAllCdpIds() {
        List<Map<String, Object>> cdpList = traceService.getAllCdpIds();
        return ResponseEntity.ok(cdpList);
    }
}


