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
            if (currentValue == null && !pathInfo.getNestedPath().isEmpty()) {
                currentValue = pathInfo.getNestedPath().get(0).startsWith("[")
                    ? new ArrayList<>() : new HashMap<>();
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
        
        String baseField = extractFieldName(parts[0]);
        List<String> nestedPath = new ArrayList<>();

        addArrayIndex(parts[0], nestedPath);
        
        // 处理嵌套路径和数组索引
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            
            // 检查是否包含数组索引（如：tier1_most_likely[0]）
            if (part.contains("[")) {
                String fieldName = extractFieldName(part);
                if (!fieldName.isEmpty()) {
                    nestedPath.add(fieldName);
                }
                addArrayIndex(part, nestedPath);
            } else {
                nestedPath.add(part);
            }
        }
        
        return FieldPathInfo.builder()
            .baseField(baseField)
            .nestedPath(nestedPath)
            .build();
    }

    private String extractFieldName(String part) {
        int bracketIndex = part.indexOf('[');
        return bracketIndex >= 0 ? part.substring(0, bracketIndex) : part;
    }

    private void addArrayIndex(String part, List<String> nestedPath) {
        int bracketIndex = part.indexOf('[');
        if (bracketIndex < 0 || !part.endsWith("]")) {
            return;
        }
        String index = part.substring(bracketIndex + 1, part.length() - 1);
        nestedPath.add("[" + index + "]");
    }
    
    /**
     * 获取CDP字段值
     */
    private Object getFieldValue(CDP cdp, String fieldName) {
        switch (fieldName) {
            case "patient_state":
                return cdp.getPatientState();
            case "ddx":
                return cdp.getDdx();
            case "triage":
                return cdp.getTriage();
            case "workup_plan":
                return cdp.getWorkupPlan();
            case "management_plan":
                return cdp.getManagementPlan();
            case "evidence_graph":
                return cdp.getEvidenceGraph();
            case "uncertainty":
                return cdp.getUncertainty();
            case "health_state_assessment":
                return cdp.getHealthStateAssessment();
            case "wellness_plan":
                return cdp.getWellnessPlan();
            case "final_conclusion":
                return cdp.getPatientState().get("conclusion_package");
            default:
                log.warn("未知的CDP字段: {}", fieldName);
                return null;
        }
    }
    
    /**
     * 设置CDP字段值
     */
    private void setFieldValue(CDP cdp, String fieldName, Object value) {
        switch (fieldName) {
            case "patient_state":
                cdp.setPatientState(toStringObjectMap(value, fieldName));
                return;
            case "triage":
                cdp.setTriage(toStringObjectMap(value, fieldName));
                return;
            case "uncertainty":
                cdp.setUncertainty(toStringObjectMap(value, fieldName));
                return;
            case "health_state_assessment":
                cdp.setHealthStateAssessment(toStringObjectMap(value, fieldName));
                return;
            case "wellness_plan":
                cdp.setWellnessPlan(toStringObjectMap(value, fieldName));
                return;
            case "ddx":
                cdp.setDdx(toMapList(value, fieldName));
                return;
            case "workup_plan":
                cdp.setWorkupPlan(toMapList(value, fieldName));
                return;
            case "management_plan":
                cdp.setManagementPlan(toMapList(value, fieldName));
                return;
            case "evidence_graph":
                cdp.setEvidenceGraph(toMapList(value, fieldName));
                return;
            case "final_conclusion":
                Map<String, Object> patientState = new HashMap<>(cdp.getPatientState());
                patientState.put("conclusion_package", toStringObjectMap(value, fieldName));
                cdp.setPatientState(patientState);
                return;
            default:
                throw new IllegalArgumentException("未知的CDP字段: " + fieldName);
        }
    }

    private Map<String, Object> toStringObjectMap(Object value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException(fieldName + " requires a Map value");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                throw new IllegalArgumentException(fieldName + " requires String map keys");
            }
            result.put((String) entry.getKey(), entry.getValue());
        }
        return result;
    }

    private List<Map<String, Object>> toMapList(Object value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof List)) {
            throw new IllegalArgumentException(fieldName + " requires a List value");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            result.add(toStringObjectMap(item, fieldName));
        }
        return result;
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
                } else {
                    throw new IllegalArgumentException("数组索引只能应用于List字段");
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
                } else {
                    throw new IllegalArgumentException("字段名只能应用于Map字段");
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

