package com.aidoctor.diagnosis.runtime;

/**
 * 受控 Raw OCR 网关自有失败。
 *
 * <p>分类：NON_PRODUCTION_ENGINEERING_CONTROLLED_CUTOVER。
 * 对外消息只保留有界代码语义，不得携带工件路径、载荷字节、PHI、
 * Python 堆栈、完整 Feign 响应或密钥。
 */
public final class ControlledRawOcrGatewayException extends RuntimeException {

    public enum Code {
        ROUTE_DISABLED,
        INVALID_RAW_OCR_CAPABILITY,
        INVALID_TOOL_CONTEXT,
        INVALID_ROUTE_POLICY,
        PYTHON_RUNTIME_CALL_FAILED,
        INVALID_RUNTIME_RESPONSE
    }

    private final Code code;

    public ControlledRawOcrGatewayException(Code code, String message) {
        super(message);
        this.code = requireCode(code);
    }

    public ControlledRawOcrGatewayException(Code code, String message, Throwable cause) {
        super(message, cause);
        this.code = requireCode(code);
    }

    public Code getCode() {
        return code;
    }

    private static Code requireCode(Code code) {
        if (code == null) {
            throw new IllegalArgumentException("gateway exception code is required");
        }
        return code;
    }
}
