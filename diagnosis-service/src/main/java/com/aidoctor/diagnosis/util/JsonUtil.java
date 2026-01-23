package com.aidoctor.diagnosis.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Component;

/**
 * JSON工具类
 * 提供JSON序列化和反序列化功能
 */
@Component
public class JsonUtil {
    
    private final ObjectMapper objectMapper;
    
    public JsonUtil() {
        this.objectMapper = new ObjectMapper();
        // 注册 JavaTimeModule 以支持 LocalDateTime 等 Java 8 时间类型
        this.objectMapper.registerModule(new JavaTimeModule());
        // 禁用将日期写为时间戳，使用 ISO-8601 格式
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    /**
     * 对象转JSON字符串
     */
    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }
    
    /**
     * JSON字符串转对象
     */
    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert JSON to object", e);
        }
    }
}

