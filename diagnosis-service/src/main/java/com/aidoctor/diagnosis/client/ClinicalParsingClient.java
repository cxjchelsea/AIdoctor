package com.aidoctor.diagnosis.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 病例理解服务客户端（tool_1）
 */
@FeignClient(name = "clinical-parsing-service", url = "${clinical-parsing.service-url:http://localhost:8082}")
public interface ClinicalParsingClient {
    
    /**
     * 病例理解（概念归一化、结构化提取）
     */
    @PostMapping("/api/v1/parsing/parse")
    Object parse(@RequestBody Object request);
    
    /**
     * 回填证据
     */
    @PostMapping("/api/v1/parsing/backfill-evidence")
    Object backfillEvidence(@RequestBody Object request);
    
    /**
     * 统一的工具调用接口（tool_1）
     * 
     * 参考文档：《7.接口规范/工具调用协议.md》
     */
    @PostMapping("/api/v1/tools/tool_1/invoke")
    com.aidoctor.diagnosis.dto.tool.ToolResult invokeTool(com.aidoctor.diagnosis.dto.tool.ToolContext toolContext);
}

