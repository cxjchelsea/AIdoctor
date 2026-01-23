package com.aidoctor.diagnosis.exception;

import com.aidoctor.diagnosis.constant.ErrorCode;

/**
 * 健康筛查记录不存在异常
 */
public class WellnessScreeningRecordNotFoundException extends BusinessException {
    public WellnessScreeningRecordNotFoundException(Long recordId) {
        super(ErrorCode.WELLNESS_SCREENING_RECORD_NOT_FOUND, String.format("健康筛查记录不存在: %d", recordId));
    }
}

