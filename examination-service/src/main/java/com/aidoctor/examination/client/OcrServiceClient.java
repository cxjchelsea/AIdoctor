package com.aidoctor.examination.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * OCR服务客户端
 */
@FeignClient(name = "ocr-service", url = "${ocr.service.url:http://localhost:8087}")
public interface OcrServiceClient {
    
    /**
     * OCR识别报告
     */
    @PostMapping(value = "/api/v1/ocr/recognize", consumes = "multipart/form-data")
    Map<String, Object> recognize(@RequestPart("file") MultipartFile file);
}

