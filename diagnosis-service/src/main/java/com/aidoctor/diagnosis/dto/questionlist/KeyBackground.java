package com.aidoctor.diagnosis.dto.questionlist;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * 关键背景
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyBackground {
    private Integer age;
    private String gender;
    private List<String> medicalHistory;
    private List<String> medicationHistory;
    private List<String> familyHistory;
    private Map<String, Object> lifestyle;
    private List<String> recentEvents;
}

