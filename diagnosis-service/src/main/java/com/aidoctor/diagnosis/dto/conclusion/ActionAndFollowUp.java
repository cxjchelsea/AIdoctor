package com.aidoctor.diagnosis.dto.conclusion;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

/**
 * 行动与随访
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionAndFollowUp {
    private List<Action> immediateActions;
    private String reviewWindow;
    private List<String> upgradeTriggers;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Action {
        private String type;  // examination / medical_advice / observation
        private String name;
        private String priority;  // high / medium / low
    }
}

