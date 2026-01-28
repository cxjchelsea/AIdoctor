package com.aidoctor.diagnosis.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON工具类
 * 用于CDP实体中JSON字段的序列化和反序列化
 * 
 * 由于Oracle不支持原生JSON类型，CDP实体中的JSON字段使用String + CLOB存储
 * 在应用层使用此工具类进行对象与JSON字符串之间的转换
 */
@Slf4j
@Component
public class JsonUtil {
    
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());
    
    // 用于序列化CDP等实体对象的ObjectMapper，使用字段访问而不是getter
    // 这样可以避免调用自定义的getter方法（返回Map/List），直接序列化String字段
    private static final ObjectMapper fieldMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
    
    /**
     * 将Map转换为JSON字符串
     * 
     * @param map Map对象
     * @return JSON字符串，如果map为null则返回null
     */
    public static String mapToJson(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Map转JSON失败", e);
            return null;
        }
    }
    
    /**
     * 将List转换为JSON字符串
     * 
     * @param list List对象
     * @return JSON字符串，如果list为null则返回null
     */
    public static String listToJson(List<Map<String, Object>> list) {
        if (list == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            log.error("List转JSON失败", e);
            return null;
        }
    }
    
    /**
     * 将JSON字符串转换为Map
     * 
     * @param json JSON字符串
     * @return Map对象，如果json为null或空则返回空的HashMap
     */
    public static Map<String, Object> jsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("JSON转Map失败: {}", json, e);
            return new HashMap<>();
        }
    }
    
    /**
     * 将JSON字符串转换为List
     * 
     * @param json JSON字符串
     * @return List对象，如果json为null或空则返回空的ArrayList
     */
    public static List<Map<String, Object>> jsonToList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("JSON转List失败: {}", json, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 将对象转换为JSON字符串（通用方法）
     * 
     * @param obj 要序列化的对象
     * @return JSON字符串，如果obj为null则返回null
     */
    public String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            // 如果是CDP实体类，使用字段映射器（避免调用自定义getter）
            if (obj.getClass().getName().equals("com.aidoctor.diagnosis.entity.CDP")) {
                String json = fieldMapper.writeValueAsString(obj);
                if (json == null || json.trim().isEmpty()) {
                    log.warn("CDP序列化结果为空: cdpId={}", 
                        ((com.aidoctor.diagnosis.entity.CDP) obj).getId());
                } else {
                    log.debug("CDP序列化成功: cdpId={}, jsonLength={}", 
                        ((com.aidoctor.diagnosis.entity.CDP) obj).getId(), json.length());
                }
                return json;
            }
            // 其他对象使用标准映射器
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("对象转JSON失败: className={}, error={}", 
                obj.getClass().getName(), e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 将JSON字符串转换为对象（通用方法）
     * 
     * @param json JSON字符串
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 反序列化后的对象，如果json为null或空则返回null
     */
    public <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("JSON转对象失败: {}", json, e);
            return null;
        }
    }
}
