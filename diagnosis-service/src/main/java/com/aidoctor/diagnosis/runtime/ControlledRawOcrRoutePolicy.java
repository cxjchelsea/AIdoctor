package com.aidoctor.diagnosis.runtime;

/**
 * 不可变、构造期注入的 Raw OCR 受控路由策略。
 *
 * <p>分类：NON_PRODUCTION_ENGINEERING_CONTROLLED_CUTOVER。
 * 默认安全语义为 {@link ControlledRawOcrRoute#DISABLED}。
 * 不是字符串 profile、请求策略、环境开关或 Spring 组件。
 */
public final class ControlledRawOcrRoutePolicy {

    private final ControlledRawOcrRoute route;

    public ControlledRawOcrRoutePolicy(ControlledRawOcrRoute route) {
        if (route == null) {
            throw new ControlledRawOcrGatewayException(
                    ControlledRawOcrGatewayException.Code.INVALID_ROUTE_POLICY,
                    "route policy requires a non-null route"
            );
        }
        this.route = route;
    }

    public static ControlledRawOcrRoutePolicy disabled() {
        return new ControlledRawOcrRoutePolicy(ControlledRawOcrRoute.DISABLED);
    }

    public static ControlledRawOcrRoutePolicy pythonRuntimeControlled() {
        return new ControlledRawOcrRoutePolicy(ControlledRawOcrRoute.PYTHON_RUNTIME_CONTROLLED);
    }

    public ControlledRawOcrRoute getRoute() {
        return route;
    }

    public boolean isDisabled() {
        return route == ControlledRawOcrRoute.DISABLED;
    }

    public boolean isPythonRuntimeControlled() {
        return route == ControlledRawOcrRoute.PYTHON_RUNTIME_CONTROLLED;
    }
}
