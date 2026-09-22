package com.aidoctor.diagnosis.client;

import com.aidoctor.diagnosis.dto.capability.c01.C01U01CapabilityRequest;
import com.aidoctor.diagnosis.dto.capability.c01.C01U01CapabilityResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Typed client for the governed C01/U01 capability path. */
@FeignClient(
        name = "clinical-parsing-service",
        contextId = "c01U01CapabilityClient",
        url = "${clinical-parsing.service-url:http://localhost:8082}")
public interface C01U01CapabilityClient {

    @PostMapping("/api/v1/capabilities/c01/u01/interpret")
    C01U01CapabilityResponse interpret(@RequestBody C01U01CapabilityRequest request);
}
