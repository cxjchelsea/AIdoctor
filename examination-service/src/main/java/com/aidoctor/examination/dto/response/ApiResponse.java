package com.aidoctor.examination.dto.response;

import lombok.Data;
import lombok.Builder;
import java.util.List;

/**
 * 统一API响应
 */
@Data
@Builder
public class ApiResponse<T> {
    
    private Integer code;
    private String message;
    private T data;
    private List<ErrorDetail> errors;
    private Long timestamp;
    private String path;
    
    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .code(200)
            .message("success")
            .data(data)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }
    
    /**
     * 错误响应
     */
    public static <T> ApiResponse<T> error(Integer code, String message) {
        return ApiResponse.<T>builder()
            .code(code)
            .message(message)
            .data(null)
            .timestamp(System.currentTimeMillis())
            .build();
    }
    
    @Data
    @Builder
    public static class ErrorDetail {
        private String field;
        private String message;
    }
}

