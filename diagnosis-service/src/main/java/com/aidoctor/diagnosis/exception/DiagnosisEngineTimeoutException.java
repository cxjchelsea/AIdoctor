package com.aidoctor.diagnosis.exception;

import com.aidoctor.diagnosis.constant.ErrorCode;

/**
 * 诊断引擎调用超时异常
 */
public class DiagnosisEngineTimeoutException extends BusinessException {
    public DiagnosisEngineTimeoutException(String serviceName, Long timeout) {
        super(ErrorCode.ENGINE_TIMEOUT, String.format("诊断引擎调用超时: %s, 超时时间=%dms", serviceName, timeout));
    }
}

