package com.aidoctor.diagnosis.dto.questionlist;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 生命体征
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalSigns {
    private BloodPressure bloodPressure;
    private Integer heartRate;
    private Double temperature;
    private Integer oxygenSaturation;
    private Integer respiratoryRate;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BloodPressure {
        private Integer systolic;
        private Integer diastolic;
    }
}

