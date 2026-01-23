package com.aidoctor.diagnosis.dto.request;

import lombok.Data;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.Map;

/**
 * 症状信息
 */
@Data
public class SymptomInfo {
    
    private String chiefComplaint;
    
    private String duration;
    
    @Min(value = 0, message = "严重程度不能小于0")
    @Max(value = 10, message = "严重程度不能大于10")
    private Integer severity;
    
    private String frequency;
    
    private String location;
    
    private List<String> accompanyingSymptoms;
    
    private Map<String, Object> features;
}

