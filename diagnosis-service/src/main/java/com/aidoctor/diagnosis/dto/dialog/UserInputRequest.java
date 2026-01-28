package com.aidoctor.diagnosis.dto.dialog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 用户输入请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInputRequest {
    private String cdpId;  // 使用cdpId与对话服务保持一致
    private String userInput;
    private Map<String, Object> context;
}

