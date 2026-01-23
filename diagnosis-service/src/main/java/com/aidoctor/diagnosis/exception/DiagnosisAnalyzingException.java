package com.aidoctor.diagnosis.exception;

import com.aidoctor.diagnosis.constant.ErrorCode;

/**
 * 诊断分析中异常
 */
public class DiagnosisAnalyzingException extends BusinessException {
    public DiagnosisAnalyzingException(String diagnosisId) {
        super(ErrorCode.DIAGNOSIS_ANALYZING, String.format("诊断分析中，请稍候: %s", diagnosisId));
    }
}

