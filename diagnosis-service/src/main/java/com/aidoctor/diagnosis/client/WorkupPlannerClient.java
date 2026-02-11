package com.aidoctor.diagnosis.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 检查建议服务客户端（tool_4）
 */
@FeignClient(name = "workup-planner-service", url = "${workup-planner.service-url:http://localhost:8082}")
public interface WorkupPlannerClient {
    
    /**
     * 构建验证计划
     */
    @PostMapping("/api/v1/workup/build-verification-plan")
    Object buildVerificationPlan(@RequestBody Object request);
    
    /**
     * 统一的工具调用接口（tool_4）
     * 
     * 参考文档：《7.接口规范/工具调用协议.md》
     */
    @PostMapping("/api/v1/tools/tool_4/invoke")
    com.aidoctor.diagnosis.dto.tool.ToolResult invokeTool(com.aidoctor.diagnosis.dto.tool.ToolContext toolContext);
}

