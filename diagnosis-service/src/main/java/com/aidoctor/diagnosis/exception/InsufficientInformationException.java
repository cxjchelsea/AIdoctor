package com.aidoctor.diagnosis.exception;

import com.aidoctor.diagnosis.constant.ErrorCode;

/**
 * 信息完整度不足异常
 */
public class InsufficientInformationException extends BusinessException {
    public InsufficientInformationException(Double completeness, Double required) {
        super(ErrorCode.INSUFFICIENT_INFORMATION, String.format("信息完整度不足: 当前=%.2f, 需要≥%.2f", 
            completeness, required));
    }
}

