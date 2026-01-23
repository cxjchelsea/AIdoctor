package com.aidoctor.diagnosis.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 健康筛查服务客户端
 * 调用健康筛查相关服务
 */
@FeignClient(name = "wellness-service", url = "${wellness.service-url:http://localhost:8089}")
public interface WellnessServiceClient {
    
    /**
     * 执行健康筛查
     */
    @PostMapping("/api/v1/wellness/screening")
    Object performScreening(@RequestBody Object request);
}

