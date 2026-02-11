package com.aidoctor.diagnosis.util;

import com.aidoctor.diagnosis.entity.CDP;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * CDP字段写回工具类
 * 负责解析fieldPath并更新CDP字段
 * 
 * 参考文档：
 * - 《5.主agent设计/主Agent运行循环设计.md》Update步骤
 * - 《7.接口规范/工具调用协议.md》SuggestedWrite
 */
@Slf4j
@Component
public class CDPFieldWriter {
    
    /**
     * 写回CDP字段
     * 
     * @param cdp CDP对象
     * @param fieldPath 字段路径（如：cdp.ddx.tier1_most_likely[0].probability）
     * @param value 字段值
     * @return 是否成功写回
     */
    public boolean writeField(CDP cdp, String fieldPath, Object value) {
        log.debug("写回CDP字段: fieldPath={}, valueType={}", fieldPath, value != null ? value.getClass().getSimpleName() : "null");
        
        try {
            // 解析fieldPath
            FieldPathInfo pathInfo = parseFieldPath(fieldPath);
            if (pathInfo == null) {
                log.warn("无法解析fieldPath: {}", fieldPath);
                return false;
            }
            
            // 获取目标字段的当前值
            Object currentValue = getFieldValue(cdp, pathInfo.getBaseField());
            if (currentValue == null) {
                log.warn("字段不存在: {}", pathInfo.getBaseField());
                return false;
            }
            
            // 更新字段值
            Object updatedValue = updateNestedField(currentValue, pathInfo.getNestedPath(), value);
            
            // 写回CDP
            setFieldValue(cdp, pathInfo.getBaseField(), updatedValue);
            
            log.debug("CDP字段写回成功: fieldPath={}", fieldPath);
            return true;
            
        } catch (Exception e) {
            log.error("CDP字段写回失败: fieldPath={}, error={}", fieldPath, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 解析fieldPath
     * 支持格式：
     * - cdp.patient_state
     * - cdp.ddx.tier1_most_likely[0].probability
     * - cdp.workup_plan[1].test_name
     */
    private FieldPathInfo parseFieldPath(String fieldPath) {
        if (fieldPath == null || !fieldPath.startsWith("cdp.")) {
            return null;
        }
        
        // 移除"cdp."前缀
        String path = fieldPath.substring(4);
        
        // 分割路径
        String[] parts = path.split("\\.");
        if (parts.length == 0) {
            return null;
        }
        
        String baseField = parts[0];
        List<String> nestedPath = new ArrayList<>();
        
        // 处理嵌套路径和数组索引
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            
            // 检查是否包含数组索引（如：tier1_most_likely[0]）
            if (part.contains("[")) {
                int bracketIndex = part.indexOf('[');
                String fieldName = part.substring(0, bracketIndex);
                String indexStr = part.substring(bracketIndex + 1, part.indexOf(']'));
                
                nestedPath.add(fieldName);
                nestedPath.add("[" + indexStr + "]");
            } else {
                nestedPath.add(part);
            }
        }
        
        return FieldPathInfo.builder()
            .baseField(baseField)
            .nestedPath(nestedPath)
            .build();
    }
    
    /**
     * 获取CDP字段值
     */
    @SuppressWarnings("unchecked")
    private Object getFieldValue(CDP cdp, String fieldName) {
        switch (fieldName) {
            case "patient_state":
                return cdp.getPatientStateMap();
            case "ddx":
                return cdp.getDdxMap();
            case "triage":
                return cdp.getTriageMap();
            case "workup_plan":
                return cdp.getWorkupPlanMap();
            case "management_plan":
                return cdp.getManagementPlanMap();
            case "evidence_graph":
                return cdp.getEvidenceGraphMap();
            case "uncertainty":
                return cdp.getUncertaintyMap();
            case "health_state_assessment":
                return cdp.getHealthStateAssessmentMap();
            case "wellness_plan":
                return cdp.getWellnessPlanMap();
            case "final_conclusion":
                return cdp.getFinalConclusionMap();
            default:
                log.warn("未知的CDP字段: {}", fieldName);
                return null;
        }
    }
    
    /**
     * 设置CDP字段值
     */
    @SuppressWarnings("unchecked")
    private void setFieldValue(CDP cdp, String fieldName, Object value) {
        if (value instanceof Map) {
            switch (fieldName) {
                case "patient_state":
                    cdp.setPatientState((Map<String, Object>) value);
                    break;
                case "ddx":
                    cdp.setDdx((Map<String, Object>) value);
                    break;
                case "triage":
                    cdp.setTriage((Map<String, Object>) value);
                    break;
                case "workup_plan":
                    cdp.setWorkupPlan((List<Map<String, Object>>) value);
                    break;
                case "management_plan":
                    cdp.setManagementPlan((Map<String, Object>) value);
                    break;
                case "evidence_graph":
                    cdp.setEvidenceGraph((List<Map<String, Object>>) value);
                    break;
                case "uncertainty":
                    cdp.setUncertainty((Map<String, Object>) value);
                    break;
                case "health_state_assessment":
                    cdp.setHealthStateAssessment((Map<String, Object>) value);
                    break;
                case "wellness_plan":
                    cdp.setWellnessPlan((Map<String, Object>) value);
                    break;
                case "final_conclusion":
                    cdp.setFinalConclusion((Map<String, Object>) value);
                    break;
                default:
                    log.warn("未知的CDP字段: {}", fieldName);
            }
        } else if (value instanceof List) {
            switch (fieldName) {
                case "workup_plan":
                    cdp.setWorkupPlan((List<Map<String, Object>>) value);
                    break;
                case "evidence_graph":
                    cdp.setEvidenceGraph((List<Map<String, Object>>) value);
                    break;
                case "ddx":
                    // ddx可能是List格式
                    cdp.setDdx((List<Map<String, Object>>) value);
                    break;
                default:
                    log.warn("未知的CDP字段（List类型）: {}", fieldName);
            }
        }
    }
    
    /**
     * 更新嵌套字段值
     */
    @SuppressWarnings("unchecked")
    private Object updateNestedField(Object currentValue, List<String> nestedPath, Object newValue) {
        if (nestedPath.isEmpty()) {
            return newValue;
        }
        
        Object result = currentValue;
        
        // 如果是Map，需要深拷贝
        if (result instanceof Map) {
            result = new HashMap<>((Map<String, Object>) result);
        } else if (result instanceof List) {
            result = new ArrayList<>((List<Object>) result);
        }
        
        Object current = result;
        
        // 遍历嵌套路径
        for (int i = 0; i < nestedPath.size(); i++) {
            String pathPart = nestedPath.get(i);
            
            if (pathPart.startsWith("[")) {
                // 数组索引
                int index = Integer.parseInt(pathPart.substring(1, pathPart.length() - 1));
                
                if (current instanceof List) {
                    List<Object> list = (List<Object>) current;
                    if (index >= list.size()) {
                        // 扩展列表
                        while (list.size() <= index) {
                            list.add(new HashMap<>());
                        }
                    }
                    
                    if (i == nestedPath.size() - 1) {
                        // 最后一个路径，直接设置值
                        list.set(index, newValue);
                    } else {
                        // 继续遍历
                        Object item = list.get(index);
                        if (item == null) {
                            item = new HashMap<>();
                            list.set(index, item);
                        }
                        current = item;
                    }
                }
            } else {
                // 字段名
                if (current instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) current;
                    
                    if (i == nestedPath.size() - 1) {
                        // 最后一个路径，直接设置值
                        map.put(pathPart, newValue);
                    } else {
                        // 继续遍历
                        Object next = map.get(pathPart);
                        if (next == null) {
                            // 检查下一个路径是否是数组索引
                            if (i + 1 < nestedPath.size() && nestedPath.get(i + 1).startsWith("[")) {
                                next = new ArrayList<>();
                            } else {
                                next = new HashMap<>();
                            }
                            map.put(pathPart, next);
                        }
                        current = next;
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * 字段路径信息
     */
    @lombok.Data
    @lombok.Builder
    private static class FieldPathInfo {
        /**
         * 基础字段名（如：ddx、patient_state）
         */
        private String baseField;
        
        /**
         * 嵌套路径（如：["tier1_most_likely", "[0]", "probability"]）
         */
        private List<String> nestedPath;
    }
}

