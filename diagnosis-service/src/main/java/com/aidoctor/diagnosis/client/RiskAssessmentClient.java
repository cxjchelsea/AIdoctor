package com.aidoctor.diagnosis.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 风险评估服务客户端（tool_6）
 */
@FeignClient(name = "risk-assessment-service", url = "${risk-assessment.service-url:http://localhost:8084}")
public interface RiskAssessmentClient {
    
    /**
     * 风险评估
     */
    @PostMapping("/api/v1/risk/assess")
    Object assessRisk(@RequestBody Object request);
    
    /**
     * 最终风险评估
     */
    @PostMapping("/api/v1/risk/assess-final")
    Object assessFinalRisk(@RequestBody Object request);
    
    /**
     * 统一的工具调用接口（tool_6）
     * 
     * 参考文档：《7.接口规范/工具调用协议.md》
     */
    @PostMapping("/api/v1/tools/tool_6/invoke")
    com.aidoctor.diagnosis.dto.tool.ToolResult invokeTool(com.aidoctor.diagnosis.dto.tool.ToolContext toolContext);
}

