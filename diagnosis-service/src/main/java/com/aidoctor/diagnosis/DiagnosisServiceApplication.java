package com.aidoctor.diagnosis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import javax.annotation.PostConstruct;
import java.util.TimeZone;

/**
 * 智能诊断服务启动类
 * 
 * @author AIdoctor Team
 * @since 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableJpaAuditing
public class DiagnosisServiceApplication {

    @PostConstruct
    void setDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(DiagnosisServiceApplication.class, args);
    }
}

