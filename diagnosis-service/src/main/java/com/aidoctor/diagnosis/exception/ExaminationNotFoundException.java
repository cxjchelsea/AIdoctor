package com.aidoctor.diagnosis.exception;

import com.aidoctor.diagnosis.constant.ErrorCode;

/**
 * 检查记录不存在异常
 */
public class ExaminationNotFoundException extends BusinessException {
    public ExaminationNotFoundException(Long examinationId) {
        super(ErrorCode.EXAMINATION_NOT_FOUND, String.format("检查记录不存在: %d", examinationId));
    }
}

