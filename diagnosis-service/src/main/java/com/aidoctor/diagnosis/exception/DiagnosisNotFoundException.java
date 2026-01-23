package com.aidoctor.diagnosis.exception;

import com.aidoctor.diagnosis.constant.ErrorCode;

/**
 * 诊断记录不存在异常
 */
public class DiagnosisNotFoundException extends BusinessException {
    public DiagnosisNotFoundException(String diagnosisId) {
        super(ErrorCode.DIAGNOSIS_NOT_FOUND, String.format("诊断记录不存在: %s", diagnosisId));
    }
}

