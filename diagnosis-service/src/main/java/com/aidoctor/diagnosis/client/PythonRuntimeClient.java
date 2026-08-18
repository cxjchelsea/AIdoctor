package com.aidoctor.diagnosis.client;

import com.aidoctor.contracts.v1.FoundationTypes;
import com.aidoctor.contracts.v1.ToolTypes;
import feign.Request;
import feign.Retryer;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 规范 Python Runtime HTTP 消费者（POSTFREEZE-02）。
 *
 * <p>分类：NON_PRODUCTION_ENGINEERING_PROTOCOL_PROOF。
 * 本客户端是未接线的规范边界，不得被生产编排调用。
 * 语义请求/响应使用 Shared Contracts v1，不另造并行 DTO。
 */
@FeignClient(
        name = "python-runtime",
        url = "${python-runtime.service-url:http://127.0.0.1:8099}",
        configuration = PythonRuntimeClient.TransportConfiguration.class
)
public interface PythonRuntimeClient {

    /**
     * 调用规范 Runtime 合成 invoke。调用方必须显式提供 X-Trace-Id，不得由客户端随机生成。
     *
     * @param traceId 必须与信封 trace_id 一致
     * @param cdpId 可选；仅作不透明合成相关元数据，不是患者权威
     * @param envelope Shared Contracts v1 ContractEnvelope
     * @return 传输包装中的 ToolResult
     */
    @PostMapping("/api/v1/runtime/tools/invoke")
    ResponseEntity<ToolTypes.ToolResult> invoke(
            @RequestHeader("X-Trace-Id") String traceId,
            @RequestHeader(value = "X-CDP-Id", required = false) String cdpId,
            @RequestBody FoundationTypes.ContractEnvelope envelope
    );

    /**
     * 仅作用于本 Feign 客户端的工程超时与禁止自动重试。
     * 不加 {@code @Configuration}，避免被组件扫描提升为全局 Feign 默认配置。
     */
    class TransportConfiguration {

        private static final int CONNECT_TIMEOUT_MILLIS = 5000;
        private static final int READ_TIMEOUT_MILLIS = 10000;

        /**
         * 工程传输超时：连接 5000ms，读取 10000ms。不是临床 SLA。
         */
        @Bean
        public Request.Options pythonRuntimeRequestOptions() {
            return new Request.Options(CONNECT_TIMEOUT_MILLIS, READ_TIMEOUT_MILLIS);
        }

        /**
         * 禁止 IO 自动重试，避免未来重复执行 Tool。
         */
        @Bean
        public Retryer pythonRuntimeRetryer() {
            return Retryer.NEVER_RETRY;
        }
    }
}
