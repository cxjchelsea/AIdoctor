package com.aidoctor.diagnosis.exception;

import com.aidoctor.diagnosis.constant.ErrorCode;

/**
 * 诊断引擎服务不可用异常
 */
public class DiagnosisEngineUnavailableException extends BusinessException {
    public DiagnosisEngineUnavailableException(String serviceName) {
        super(ErrorCode.ENGINE_UNAVAILABLE, String.format("诊断引擎服务不可用: %s", serviceName));
    }
}

