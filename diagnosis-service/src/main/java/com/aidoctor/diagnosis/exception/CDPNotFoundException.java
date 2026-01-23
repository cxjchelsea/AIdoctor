package com.aidoctor.diagnosis.exception;

import com.aidoctor.diagnosis.constant.ErrorCode;

/**
 * CDP不存在异常
 */
public class CDPNotFoundException extends BusinessException {
    public CDPNotFoundException(Long cdpId) {
        super(ErrorCode.CDP_NOT_FOUND, String.format("CDP不存在: %d", cdpId));
    }
    
    public CDPNotFoundException(String cdpId) {
        super(ErrorCode.CDP_NOT_FOUND, String.format("CDP不存在: %s", cdpId));
    }
}

