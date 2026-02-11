package com.aidoctor.diagnosis.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 解释生成服务客户端（tool_7）
 */
@FeignClient(name = "explanation-service", url = "${explanation.service-url:http://localhost:8089}")
public interface ExplanationServiceClient {
    
    /**
     * 生成终点结论包
     */
    @PostMapping("/api/v1/explain/conclusion-package")
    Object generateConclusionPackage(@RequestBody Object request);
    
    /**
     * 统一的工具调用接口（tool_7）
     * 
     * 参考文档：《7.接口规范/工具调用协议.md》
     */
    @PostMapping("/api/v1/tools/tool_7/invoke")
    com.aidoctor.diagnosis.dto.tool.ToolResult invokeTool(com.aidoctor.diagnosis.dto.tool.ToolContext toolContext);
}

