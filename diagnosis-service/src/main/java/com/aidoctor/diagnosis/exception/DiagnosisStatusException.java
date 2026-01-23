package com.aidoctor.diagnosis.exception;

import com.aidoctor.diagnosis.constant.ErrorCode;

/**
 * 诊断状态不允许此操作异常
 */
public class DiagnosisStatusException extends BusinessException {
    public DiagnosisStatusException(String diagnosisId, String currentStatus, String requiredStatus) {
        super(ErrorCode.DIAGNOSIS_STATUS_INVALID, String.format("诊断状态不允许此操作: 当前状态=%s, 需要状态=%s", 
            currentStatus, requiredStatus));
    }
}

