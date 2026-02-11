package com.aidoctor.diagnosis.exception;

import com.aidoctor.diagnosis.dto.response.ApiResponse;
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
     */
    private HttpStatus getHttpStatus(Integer code) {
        // tool_0：健康状态判定服务错误码（1000-1099）
        if (code >= 1000 && code < 1100) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        // tool_1：病例理解服务错误码（1100-1199）
        else if (code >= 1100 && code < 1200) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        // tool_2：主动问诊服务错误码（1200-1299）
        else if (code >= 1200 && code < 1300) {
            if (code == 1206) return HttpStatus.BAD_REQUEST; // 追问次数超限
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        // tool_4：检查建议引擎错误码（1300-1399）
        else if (code >= 1300 && code < 1400) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        // tool_5：治疗推理引擎错误码（1400-1499）
        else if (code >= 1400 && code < 1500) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        // tool_6：风险评估引擎错误码（1500-1599）
        else if (code >= 1500 && code < 1600) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        // tool_7：可解释性服务错误码（1600-1699）
        else if (code >= 1600 && code < 1700) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        // CDP管理错误码（1700-1799）
        else if (code >= 1700 && code < 1800) {
            if (code == 1704) return HttpStatus.NOT_FOUND; // CDP不存在
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        // 健康筛查流程（A路径）错误码（1800-1899）
        else if (code >= 1800 && code < 1900) {
            if (code == 1802 || code == 1804 || code == 1805) return HttpStatus.BAD_REQUEST;
            if (code == 1813) return HttpStatus.NOT_FOUND; // 健康筛查记录不存在
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        // 诊断服务错误码（2000-2099）
        else if (code >= 2000 && code < 2100) {
            if (code == 2001) return HttpStatus.NOT_FOUND; // 诊断记录不存在
            if (code == 2004) return HttpStatus.NOT_FOUND; // 检查方案不存在
            if (code == 2006 || code == 2007 || code == 2008) return HttpStatus.INTERNAL_SERVER_ERROR;
            if (code == 2012) return HttpStatus.INTERNAL_SERVER_ERROR; // CDP关联失败
            return HttpStatus.BAD_REQUEST;
        }
        // tool_3：鉴别诊断引擎错误码（3000-3999）
        else if (code >= 3000 && code < 4000) {
            if (code == 3001) return HttpStatus.SERVICE_UNAVAILABLE; // 诊断引擎服务不可用
            if (code == 3002) return HttpStatus.GATEWAY_TIMEOUT; // 诊断引擎调用超时
            return HttpStatus.INTERNAL_SERVER_ERROR;
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

