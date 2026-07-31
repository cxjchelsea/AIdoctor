package com.aidoctor.diagnosis.agent;

import com.aidoctor.diagnosis.client.*;
import com.aidoctor.diagnosis.dto.tool.ToolContext;
import com.aidoctor.diagnosis.dto.tool.ToolResult;
import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.service.audit.AuditTrailManager;
import com.aidoctor.diagnosis.service.cdp.CDPManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 工具调用器
 * 负责调用工具并处理结果
 * 
 * 参考文档：
 * - 《2.架构设计/主Agent架构设计.md》
 * - 《7.接口规范/工具调用协议.md》
 */
@Slf4j
@Component
public class ToolCaller {
    
    @Autowired
    private CDPManager cdpManager;
    
    @Autowired
    private AuditTrailManager auditTrailManager;
    
    @Autowired
    private ClinicalParsingClient clinicalParsingClient;
    
    @Autowired
    private DialogServiceClient dialogServiceClient;
    
    @Autowired
    private DiagnosisEngineClient diagnosisEngineClient;
    
    @Autowired
    private WorkupPlannerClient workupPlannerClient;
    
    @Autowired
    private TreatmentEngineClient treatmentEngineClient;
    
    @Autowired
    private RiskAssessmentClient riskAssessmentClient;
    
    @Autowired
    private ExplanationServiceClient explanationServiceClient;
    
    @Autowired
    private HealthStateAssessmentClient healthStateAssessmentClient;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 调用工具
     * 
     * @param toolContext 工具调用上下文
     * @param toolId 工具ID
     * @return 工具返回结果
     */
    public ToolResult callTool(ToolContext toolContext, String toolId) {
        log.info("调用工具: toolId={}, traceId={}", toolId, toolContext.getTraceId());
        
        long startTime = System.currentTimeMillis();
        String cdpId = toolContext.getCdpReference().getCdpId();
        String sessionId = getSessionIdFromCdp(cdpId);
        
        try {
            // 调用工具（使用统一接口，直接返回ToolResult）
            ToolResult toolResult = invokeToolClient(toolId, toolContext);
            
            // 计算执行时间（如果工具返回的durationMs为null，则使用本地计算的时间）
            long durationMs = System.currentTimeMillis() - startTime;
            if (toolResult.getDurationMs() == null) {
                toolResult.setDurationMs(durationMs);
            }
            
            // 确保toolId和traceId正确设置
            toolResult.setToolId(toolId);
            toolResult.setTraceId(toolContext.getTraceId());
            
            // 记录到AuditTrail
            recordToolCallToAuditTrail(cdpId, sessionId, toolId, toolContext, toolResult);
            
            log.info("工具调用完成: toolId={}, traceId={}, status={}, durationMs={}", 
                toolId, toolContext.getTraceId(), toolResult.getStatus(), toolResult.getDurationMs());
            
            return toolResult;
            
        } catch (Exception e) {
            log.error("工具调用失败: toolId={}, traceId={}, error={}", 
                toolId, toolContext.getTraceId(), e.getMessage(), e);
            
            // 返回失败结果
            ToolResult toolResult = ToolResult.builder()
                .traceId(toolContext.getTraceId())
                .toolId(toolId)
                .status("failure")
                .payload(new HashMap<>())
                .evidence(new ArrayList<>())
                .quality(ToolResult.Quality.builder()
                    .confidence(0.0)
                    .completeness(0.0)
                    .build())
                .suggestedWrites(new ArrayList<>())
                .errors(Collections.singletonList(ToolResult.ErrorInfo.builder()
                    .errorType("runtime_error")
                    .errorMessage(e.getMessage())
                    .errorDetails(new HashMap<>())
                    .build()))
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
            
            // 记录到AuditTrail
            recordToolCallToAuditTrail(cdpId, sessionId, toolId, toolContext, toolResult);
            
            return toolResult;
        }
    }
    
    /**
     * 调用工具客户端（使用统一接口）
     * 
     * 参考文档：《7.接口规范/工具调用协议.md》
     */
    private ToolResult invokeToolClient(String toolId, ToolContext toolContext) {
        // 使用统一接口调用工具
        switch (toolId) {
            case "tool_0":
                return healthStateAssessmentClient.invokeTool(toolContext);
            case "tool_1":
                return clinicalParsingClient.invokeTool(toolContext);
            case "tool_2":
                return dialogServiceClient.invokeTool(toolContext);
            case "tool_3":
                return diagnosisEngineClient.invokeTool(toolContext);
            case "tool_4":
                return workupPlannerClient.invokeTool(toolContext);
            case "tool_5":
                return treatmentEngineClient.invokeTool(toolContext);
            case "tool_6":
                return riskAssessmentClient.invokeTool(toolContext);
            case "tool_7":
                return explanationServiceClient.invokeTool(toolContext);
            default:
                throw new IllegalArgumentException("未知的工具ID: " + toolId);
        }
    }
    
    /**
     * 构建工具请求（保留作为兼容层，用于旧接口调用）
     * 
     * @deprecated 已迁移到统一接口，此方法保留用于向后兼容
     */
    @Deprecated
    private Map<String, Object> buildToolRequest(ToolContext toolContext) {
        Map<String, Object> request = new HashMap<>();
        
        // 获取CDP数据
        CDP cdp = cdpManager.getCDPById(toolContext.getCdpReference().getCdpId())
            .orElseThrow(() -> new RuntimeException("CDP不存在: " + toolContext.getCdpReference().getCdpId()));
        
        // 根据readFields读取CDP字段
        Map<String, Object> cdpData = new HashMap<>();
        List<String> readFields = toolContext.getCdpReference().getReadFields();
        if (readFields != null && !readFields.isEmpty()) {
            for (String fieldPath : readFields) {
                Object value = getCdpFieldValue(cdp, fieldPath);
                cdpData.put(fieldPath, value);
            }
        }
        
        request.put("cdp_data", cdpData);
        request.put("agent_state_summary", toolContext.getAgentStateSummary());
        request.put("constraints", toolContext.getConstraints());
        request.put("call_params", toolContext.getCallParams());
        request.put("trace_id", toolContext.getTraceId());
        
        return request;
    }
    
    /**
     * 获取CDP字段值（保留作为兼容层）
     * 
     * @deprecated 已迁移到统一接口，此方法保留用于向后兼容
     */
    @Deprecated
    private Object getCdpFieldValue(CDP cdp, String fieldPath) {
        // 简化实现：根据字段路径返回对应的CDP字段
        // 实际应该实现完整的路径解析
        if (fieldPath.startsWith("cdp.patient_state")) {
            return cdp.getPatientState();
        } else if (fieldPath.startsWith("cdp.ddx")) {
            return cdp.getDdx();
        } else if (fieldPath.startsWith("cdp.triage")) {
            return cdp.getTriage();
        } else if (fieldPath.startsWith("cdp.workup_plan")) {
            return cdp.getWorkupPlan();
        } else if (fieldPath.startsWith("cdp.management_plan")) {
            return cdp.getManagementPlan();
        } else if (fieldPath.startsWith("cdp.evidence_graph")) {
            return cdp.getEvidenceGraph();
        }
        return null;
    }
    
    /**
     * 记录工具调用到AuditTrail
     */
    private void recordToolCallToAuditTrail(String cdpId, String sessionId, String toolId, 
                                           ToolContext toolContext, ToolResult toolResult) {
        try {
            Map<String, Object> toolCallData = new HashMap<>();
            toolCallData.put("tool_id", toolId);
            toolCallData.put("tool_name", getToolName(toolId));
            toolCallData.put("trace_id", toolContext.getTraceId());
            toolCallData.put("input", buildToolCallInput(toolContext));
            toolCallData.put("output", buildToolCallOutput(toolResult));
            toolCallData.put("evidence", toolResult.getEvidence());
            toolCallData.put("suggested_writes", toolResult.getSuggestedWrites());
            toolCallData.put("quality", toolResult.getQuality());
            toolCallData.put("errors", toolResult.getErrors());
            toolCallData.put("duration_ms", toolResult.getDurationMs());
            
            auditTrailManager.recordToolCall(cdpId, sessionId, toolCallData);
        } catch (Exception e) {
            log.error("记录工具调用到AuditTrail失败: toolId={}, traceId={}, error={}", 
                toolId, toolContext.getTraceId(), e.getMessage(), e);
        }
    }
    
    /**
     * 构建工具调用输入摘要
     */
    private Map<String, Object> buildToolCallInput(ToolContext toolContext) {
        Map<String, Object> input = new HashMap<>();
        input.put("cdp_reference", toolContext.getCdpReference());
        input.put("agent_state_summary", toolContext.getAgentStateSummary());
        input.put("constraints", toolContext.getConstraints());
        return input;
    }
    
    /**
     * 构建工具调用输出摘要
     */
    private Map<String, Object> buildToolCallOutput(ToolResult toolResult) {
        Map<String, Object> output = new HashMap<>();
        output.put("status", toolResult.getStatus());
        output.put("payload_summary", "payload数据");
        output.put("evidence_count", toolResult.getEvidence().size());
        return output;
    }
    
    /**
     * 获取工具名称
     */
    private String getToolName(String toolId) {
        switch (toolId) {
            case "tool_0": return "健康状态判定工具";
            case "tool_1": return "病例理解工具";
            case "tool_2": return "主动问诊工具";
            case "tool_3": return "鉴别诊断工具";
            case "tool_4": return "检查建议工具";
            case "tool_5": return "治疗建议工具";
            case "tool_6": return "风险评估工具";
            case "tool_7": return "证据链工具";
            default: return "未知工具";
        }
    }
    
    /**
     * 从CDP获取会话ID
     */
    private String getSessionIdFromCdp(String cdpId) {
        return cdpManager.getCDPById(cdpId)
            .map(CDP::getSessionId)
            .orElse("unknown");
    }
}

