package com.aidoctor.examination.exception;

import lombok.Getter;

/**
 * 业务异常基类
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final Integer code;
    private final String message;
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
    
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }
}

