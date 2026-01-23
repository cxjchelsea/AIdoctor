package com.aidoctor.diagnosis.exception;

import com.aidoctor.diagnosis.constant.ErrorCode;

/**
 * 文件格式不支持异常
 */
public class UnsupportedFileFormatException extends BusinessException {
    public UnsupportedFileFormatException(String format) {
        super(ErrorCode.UNSUPPORTED_FILE_FORMAT, String.format("文件格式不支持: %s", format));
    }
}

