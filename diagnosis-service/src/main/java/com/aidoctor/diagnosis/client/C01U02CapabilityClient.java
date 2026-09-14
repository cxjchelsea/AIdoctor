package com.aidoctor.diagnosis.client;

import com.aidoctor.diagnosis.dto.capability.c01.C01U02CapabilityRequest;
import com.aidoctor.diagnosis.dto.capability.c01.C01U02CapabilityResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Typed client for the governed C01/U02 observation-candidate path. */
@FeignClient(
        name = "clinical-parsing-service",
        contextId = "c01U02CapabilityClient",
        url = "${clinical-parsing.service-url:http://localhost:8082}")
public interface C01U02CapabilityClient {

    @PostMapping("/api/v1/capabilities/c01/u02/interpret")
    C01U02CapabilityResponse interpret(@RequestBody C01U02CapabilityRequest request);
}
