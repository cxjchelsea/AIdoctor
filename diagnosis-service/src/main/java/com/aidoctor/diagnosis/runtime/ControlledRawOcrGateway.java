package com.aidoctor.diagnosis.runtime;

import com.aidoctor.contracts.v1.ToolTypes;
import com.aidoctor.diagnosis.client.PythonRuntimeClient;
import feign.FeignException;
import org.springframework.http.ResponseEntity;

/**
 * diagnosis-service 有界 Raw OCR 受控路由网关。
 *
 * <p>分类：NON_PRODUCTION_ENGINEERING_CONTROLLED_CUTOVER。
 * 不是 Spring 组件，不得被默认应用扫描或临床编排接线。
 * 仅在显式 server-side / 工程 / 测试组合中构造。
 * 只转发精确的 {@code engineering.ocr.raw / 0.0.1} ToolContext，
 * 不打开 InputRef，不回退遗留 OCR，不重试。
 */
public final class ControlledRawOcrGateway {

    public static final String RAW_OCR_CAPABILITY_ID = "engineering.ocr.raw";
    public static final String RAW_OCR_CAPABILITY_VERSION = "0.0.1";

    private final PythonRuntimeClient pythonRuntimeClient;
    private final ControlledRawOcrRoutePolicy routePolicy;

    public ControlledRawOcrGateway(
            PythonRuntimeClient pythonRuntimeClient,
            ControlledRawOcrRoutePolicy routePolicy
    ) {
        if (pythonRuntimeClient == null) {
            throw new ControlledRawOcrGatewayException(
                    ControlledRawOcrGatewayException.Code.INVALID_ROUTE_POLICY,
                    "python runtime client is required"
            );
        }
        if (routePolicy == null) {
            throw new ControlledRawOcrGatewayException(
                    ControlledRawOcrGatewayException.Code.INVALID_ROUTE_POLICY,
                    "route policy is required"
            );
        }
        this.pythonRuntimeClient = pythonRuntimeClient;
        this.routePolicy = routePolicy;
    }

    /**
     * 按构造期策略路由有界 Raw OCR。路由不作为方法参数暴露。
     */
    public ToolTypes.ToolResult invokeRawOcr(ToolTypes.ToolContext context) {
        if (routePolicy.isDisabled()) {
            throw new ControlledRawOcrGatewayException(
                    ControlledRawOcrGatewayException.Code.ROUTE_DISABLED,
                    "raw ocr route is disabled"
            );
        }
        validateExactRawOcrContext(context);

        String traceId = context.envelope.traceId;
        String cdpId = null;
        if (context.identifiers != null) {
            cdpId = context.identifiers.cdpId;
        }

        try {
            ResponseEntity<ToolTypes.ToolResult> response =
                    pythonRuntimeClient.invokeToolContext(traceId, cdpId, context);
            return requireToolResult(response);
        } catch (ControlledRawOcrGatewayException alreadyBounded) {
            throw alreadyBounded;
        } catch (FeignException failure) {
            throw new ControlledRawOcrGatewayException(
                    ControlledRawOcrGatewayException.Code.PYTHON_RUNTIME_CALL_FAILED,
                    "python runtime call failed",
                    failure
            );
        } catch (RuntimeException failure) {
            throw new ControlledRawOcrGatewayException(
                    ControlledRawOcrGatewayException.Code.PYTHON_RUNTIME_CALL_FAILED,
                    "python runtime call failed",
                    failure
            );
        }
    }

    private static void validateExactRawOcrContext(ToolTypes.ToolContext context) {
        if (context == null) {
            throw new ControlledRawOcrGatewayException(
                    ControlledRawOcrGatewayException.Code.INVALID_TOOL_CONTEXT,
                    "tool context is required"
            );
        }
        if (context.capability == null || context.envelope == null) {
            throw new ControlledRawOcrGatewayException(
                    ControlledRawOcrGatewayException.Code.INVALID_TOOL_CONTEXT,
                    "tool context capability and envelope are required"
            );
        }
        if (isBlank(context.envelope.traceId)) {
            throw new ControlledRawOcrGatewayException(
                    ControlledRawOcrGatewayException.Code.INVALID_TOOL_CONTEXT,
                    "tool context envelope trace id is required"
            );
        }
        if (!RAW_OCR_CAPABILITY_ID.equals(context.capability.capabilityId)
                || !RAW_OCR_CAPABILITY_VERSION.equals(context.capability.capabilityVersion)
                || !RAW_OCR_CAPABILITY_ID.equals(context.envelope.capabilityId)
                || !RAW_OCR_CAPABILITY_VERSION.equals(context.envelope.capabilityVersion)) {
            throw new ControlledRawOcrGatewayException(
                    ControlledRawOcrGatewayException.Code.INVALID_RAW_OCR_CAPABILITY,
                    "capability is not the exact raw ocr allowlist identity"
            );
        }
    }

    private static ToolTypes.ToolResult requireToolResult(
            ResponseEntity<ToolTypes.ToolResult> response
    ) {
        if (response == null) {
            throw new ControlledRawOcrGatewayException(
                    ControlledRawOcrGatewayException.Code.INVALID_RUNTIME_RESPONSE,
                    "runtime response entity is absent"
            );
        }
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new ControlledRawOcrGatewayException(
                    ControlledRawOcrGatewayException.Code.INVALID_RUNTIME_RESPONSE,
                    "runtime response is not successful"
            );
        }
        ToolTypes.ToolResult body = response.getBody();
        if (body == null) {
            throw new ControlledRawOcrGatewayException(
                    ControlledRawOcrGatewayException.Code.INVALID_RUNTIME_RESPONSE,
                    "runtime response body is absent"
            );
        }
        return body;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
