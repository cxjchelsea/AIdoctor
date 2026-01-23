package com.aidoctor.diagnosis.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * OCR服务客户端
 */
@FeignClient(name = "ocr-service", url = "${diagnosis.ocr.service-url}")
public interface OcrServiceClient {
    
    @PostMapping(value = "/api/v1/ocr/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Map<String, Object> recognize(@RequestPart("file") MultipartFile file);
}

