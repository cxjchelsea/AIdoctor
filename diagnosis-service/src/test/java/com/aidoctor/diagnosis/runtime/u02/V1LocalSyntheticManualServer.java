package com.aidoctor.diagnosis.runtime.u02;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;

/**
 * LOCAL TEST CLASSPATH ONLY. Runs the already-verified two-turn synthetic host
 * on loopback, never deployable in the application jar.
 *
 * Requires a disposable MySQL container and the explicit local environment
 * guard of U01U02PhysicalMysqlHandoffTest.DatabaseConfig.
 */
public final class V1LocalSyntheticManualServer {
    private V1LocalSyntheticManualServer() {}

    public static void main(String[] args) {
        if (!"LOCAL_SYNTHETIC_ONLY".equals(System.getenv("V1_LOCAL_SYNTHETIC_HOST"))
                || "true".equals(System.getenv("GITHUB_ACTIONS"))
                || !"DISPOSABLE_U01_U02_ONLY".equals(System.getenv("V1_U01_U02_MYSQL_MODE")))
            throw new IllegalStateException("V1_LOCAL_SYNTHETIC_ONLY");
        SpringApplication app=new SpringApplication(V1SyntheticTwoTurnTcpMysqlTest.Host.class);
        Map<String,Object> props=new HashMap<>();
        props.put("server.address","127.0.0.1");
        props.put("server.port","8084");
        props.put("spring.cloud.discovery.enabled","false");
        props.put("spring.cloud.nacos.discovery.enabled","false");
        props.put("springfox.documentation.enabled","false");
        props.put("spring.flyway.enabled","false");
        props.put("aidoctor.v1.clinical-e2e.first-round-wait-http-enabled","true");
        props.put("aidoctor.v1.clinical-e2e.answer-resume-http-enabled","true");
        props.put("aidoctor.v1.clinical-e2e.answer-risk-http-enabled","true");
        props.put("aidoctor.v1.clinical-e2e.synthetic-question-content-enabled","true");
        props.put("aidoctor.v1.clinical-e2e.synthetic-two-turn-fixture","true");
        props.put("aidoctor.v1.clinical-e2e.readiness-diagnostics-enabled","true");
        app.setDefaultProperties(props);
        app.run("--spring.profiles.active=clinical-e2e-test");
    }
}
