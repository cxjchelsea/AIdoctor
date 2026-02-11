package com.aidoctor.diagnosis.client;

import com.aidoctor.diagnosis.dto.dialog.QuestionRequest;
import com.aidoctor.diagnosis.dto.dialog.QuestionResponse;
import com.aidoctor.diagnosis.dto.dialog.UserInputRequest;
import com.aidoctor.diagnosis.dto.dialog.UnderstandingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 对话服务客户端（tool_2）
 */
@FeignClient(name = "dialog-service", url = "${dialog.service-url:http://localhost:8087}")
public interface DialogServiceClient {
    
    /**
     * 生成追问问题（兼容旧接口）
     */
    @PostMapping("/api/v1/dialog/generate-question")
    QuestionResponse generateQuestion(@RequestBody QuestionRequest request);
    
    /**
     * 理解用户输入（兼容旧接口）
     */
    @PostMapping("/api/v1/dialog/understand")
    UnderstandingResponse understand(@RequestBody UserInputRequest request);
    
    /**
     * 识别信息缺口
     */
    @PostMapping("/api/v1/dialog/identify-gaps")
    Object identifyGaps(@RequestBody Object request);
    
    /**
     * 设计分流路径
     */
    @PostMapping("/api/v1/dialog/design-routing-path")
    Object designRoutingPath(@RequestBody Object request);
    
    /**
     * 采集关键证据
     */
    @PostMapping("/api/v1/dialog/collect-key-evidence")
    Object collectKeyEvidence(@RequestBody Object request);
    
    /**
     * 统一的工具调用接口（tool_2）
     * 
     * 参考文档：《7.接口规范/工具调用协议.md》
     */
    @PostMapping("/api/v1/tools/tool_2/invoke")
    com.aidoctor.diagnosis.dto.tool.ToolResult invokeTool(com.aidoctor.diagnosis.dto.tool.ToolContext toolContext);
}

