package com.aidoctor.diagnosis.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 健康档案服务客户端
 * 调用健康档案服务（如果依赖）
 */
@FeignClient(name = "profile-service", url = "${profile.service-url:http://localhost:8090}")
public interface ProfileServiceClient {
    
    /**
     * 获取用户健康档案
     */
    @GetMapping("/api/v1/profile/{userId}")
    Object getUserProfile(@PathVariable("userId") String userId);
    
    /**
     * 更新用户健康档案
     */
    @PostMapping("/api/v1/profile/update")
    Object updateUserProfile(@RequestBody Object profileData);
    
    /**
     * 获取用户历史诊断记录
     */
    @GetMapping("/api/v1/profile/{userId}/diagnosis-history")
    Object getDiagnosisHistory(@PathVariable("userId") String userId);
}

