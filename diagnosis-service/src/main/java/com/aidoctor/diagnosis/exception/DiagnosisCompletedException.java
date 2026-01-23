package com.aidoctor.diagnosis.exception;

import com.aidoctor.diagnosis.constant.ErrorCode;

/**
 * 诊断已完成异常
 */
public class DiagnosisCompletedException extends BusinessException {
    public DiagnosisCompletedException(String diagnosisId) {
        super(ErrorCode.DIAGNOSIS_COMPLETED, String.format("诊断已完成，无法修改: %s", diagnosisId));
    }
}

