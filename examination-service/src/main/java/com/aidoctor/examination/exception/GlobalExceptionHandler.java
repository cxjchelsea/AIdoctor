package com.aidoctor.examination.exception;

import com.aidoctor.examination.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 全局异常处理器
 * 按照《AI医生系统-错误处理规范.md》定义
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(
            BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: code={}, message={}, path={}", 
            e.getCode(), e.getMessage(), request.getRequestURI());
        
        ApiResponse<Object> response = ApiResponse.builder()
            .code(e.getCode())
            .message(e.getMessage())
            .data(null)
            .timestamp(System.currentTimeMillis())
            .path(request.getRequestURI())
            .build();
        
        HttpStatus httpStatus = getHttpStatus(e.getCode());
        return ResponseEntity.status(httpStatus).body(response);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        log.warn("参数验证失败: path={}", request.getRequestURI());
        
        List<ApiResponse.ErrorDetail> errors = new ArrayList<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                errors.add(ApiResponse.ErrorDetail.builder()
                    .field(fieldError.getField())
                    .message(fieldError.getDefaultMessage())
                    .build());
            }
        });
        
        ApiResponse<Object> response = ApiResponse.builder()
            .code(5001)
            .message("参数验证失败")
            .data(null)
            .errors(errors)
            .timestamp(System.currentTimeMillis())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUploadSizeException(
            MaxUploadSizeExceededException e, HttpServletRequest request) {
        log.warn("文件大小超限: path={}", request.getRequestURI());
        
        ApiResponse<Object> response = ApiResponse.builder()
            .code(2003)
            .message("文件大小超限")
            .data(null)
            .timestamp(System.currentTimeMillis())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(
            Exception e, HttpServletRequest request) {
        log.error("系统异常: path={}", request.getRequestURI(), e);
        
        ApiResponse<Object> response = ApiResponse.builder()
            .code(500)
            .message("系统内部错误")
            .data(null)
            .timestamp(System.currentTimeMillis())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 根据错误码获取HTTP状态码
     * 按照《AI医生系统-错误处理规范.md》定义
     * 注意：检查服务错误码（2000-2999）与诊断服务错误码有重叠，此处按检查服务规范处理
     */
    private HttpStatus getHttpStatus(Integer code) {
        // 检查服务错误码（2000-2999）
        if (code >= 2000 && code < 3000) {
            if (code == 2001 || code == 2004) return HttpStatus.NOT_FOUND; // 检查记录不存在、检查方案不存在
            if (code == 2006 || code == 2007 || code == 2008) return HttpStatus.INTERNAL_SERVER_ERROR;
            return HttpStatus.BAD_REQUEST;
        }
        // OCR服务错误码（4000-4999）
        else if (code >= 4000 && code < 5000) {
            if (code == 4003 || code == 4004 || code == 4005) return HttpStatus.BAD_REQUEST;
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        // 通用错误码（5000-5999）
        else if (code >= 5000 && code < 6000) {
            if (code == 5001 || code == 5002) return HttpStatus.BAD_REQUEST;
            if (code == 5006) return HttpStatus.SERVICE_UNAVAILABLE;
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        // 默认返回500
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}

