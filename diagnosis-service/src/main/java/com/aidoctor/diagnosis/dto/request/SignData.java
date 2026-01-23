package com.aidoctor.diagnosis.dto.request;

import lombok.Data;
import java.util.Map;

/**
 * 体征数据
 */
@Data
public class SignData {
    
    private BloodPressure bp;
    
    private Integer heartRate;
    
    private Double temperature;
    
    private Integer oxygenSaturation;
    
    private Integer respiratoryRate;
    
    private Map<String, Object> otherSigns;
    
    @Data
    public static class BloodPressure {
        private Integer systolic;
        private Integer diastolic;
    }
}

