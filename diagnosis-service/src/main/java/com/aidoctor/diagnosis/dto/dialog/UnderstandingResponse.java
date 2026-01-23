package com.aidoctor.diagnosis.dto.dialog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 理解响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnderstandingResponse {
    private Map<String, Object> extractedInfo;
    private Double confidence;
}

