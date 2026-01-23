package com.aidoctor.diagnosis.dto.engine;

import lombok.Data;
import lombok.Builder;
import java.util.List;
import java.util.Map;

/**
 * 诊断引擎请求
 */
@Data
@Builder
public class DiagnosisEngineRequest {
    
    private Map<String, Object> symptomInfo;
    private Map<String, Object> vitalSigns;
    private List<Map<String, Object>> examinationResults;
    private Map<String, Object> healthProfile;
}

