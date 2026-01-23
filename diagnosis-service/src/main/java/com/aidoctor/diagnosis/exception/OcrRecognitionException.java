package com.aidoctor.diagnosis.exception;

import com.aidoctor.diagnosis.constant.ErrorCode;

/**
 * OCR识别失败异常
 */
public class OcrRecognitionException extends BusinessException {
    public OcrRecognitionException(String reason) {
        super(ErrorCode.OCR_FAILED, String.format("OCR识别失败: %s", reason));
    }
}

