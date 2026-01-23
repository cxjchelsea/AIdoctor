package com.aidoctor.diagnosis.util;

/**
 * 追踪上下文
 * 使用ThreadLocal存储当前请求的CDP ID
 */
public class TraceContext {
    
    private static final ThreadLocal<String> CDP_ID = new ThreadLocal<>();
    
    /**
     * 设置CDP ID
     */
    public static void setCdpId(String cdpId) {
        CDP_ID.set(cdpId);
    }
    
    /**
     * 获取CDP ID
     */
    public static String getCdpId() {
        return CDP_ID.get();
    }
    
    /**
     * 清除上下文
     */
    public static void clear() {
        CDP_ID.remove();
    }
}

