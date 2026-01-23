package com.aidoctor.diagnosis.exception;

import com.aidoctor.diagnosis.constant.ErrorCode;

/**
 * 文件大小超限异常
 */
public class FileSizeExceededException extends BusinessException {
    public FileSizeExceededException(Long size, Long maxSize) {
        super(ErrorCode.FILE_SIZE_EXCEEDED, String.format("文件大小超限: 当前=%d, 最大=%d", size, maxSize));
    }
}

