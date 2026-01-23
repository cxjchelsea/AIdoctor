package com.aidoctor.trace.service;

import com.aidoctor.trace.dto.ExecutionTraceEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 执行追踪事件发布器
 * 通过WebSocket推送事件到前端
 */
@Slf4j
@Component
public class ExecutionTraceEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ExecutionTraceEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Async("traceExecutor")
    public void publish(ExecutionTraceEvent event) {
        if (event.getCdpId() == null) {
            return;
        }
        // 推送给订阅该CDP的管理端客户端
        messagingTemplate.convertAndSend("/topic/trace/" + event.getCdpId(), event);
    }
}


