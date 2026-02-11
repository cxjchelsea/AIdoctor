package com.aidoctor.diagnosis.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 治疗推理服务客户端（tool_5）
 */
@FeignClient(name = "treatment-engine-service", url = "${treatment-engine.service-url:http://localhost:8083}")
public interface TreatmentEngineClient {
    
    /**
     * 生成治疗方案
     */
    @PostMapping("/api/v1/treatment/generate-plan")
    Object generateTreatmentPlan(@RequestBody Object request);
    
    /**
     * 统一的工具调用接口（tool_5）
     * 
     * 参考文档：《7.接口规范/工具调用协议.md》
     */
    @PostMapping("/api/v1/tools/tool_5/invoke")
    com.aidoctor.diagnosis.dto.tool.ToolResult invokeTool(com.aidoctor.diagnosis.dto.tool.ToolContext toolContext);
}

