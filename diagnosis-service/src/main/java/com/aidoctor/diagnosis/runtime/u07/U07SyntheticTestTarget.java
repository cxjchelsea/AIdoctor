package com.aidoctor.diagnosis.runtime.u07;

/** Fixed disposable profiles, never a production datasource configuration. */
public enum U07SyntheticTestTarget {
    D3(33320, "u07_d3_synthetic"), D4(33321, "u07_d4_synthetic");
    private final String url;
    U07SyntheticTestTarget(int port, String schema) {
        url = "jdbc:mysql://127.0.0.1:" + port + "/" + schema
                + "?useSSL=false&allowPublicKeyRetrieval=true";
    }
    public String jdbcUrl() { return url; }
    public static U07SyntheticTestTarget fromExactUrl(String url) {
        for (U07SyntheticTestTarget target : values()) {
            if (target.url.equals(url)) return target;
        }
        throw new IllegalArgumentException("INVALID_SYNTHETIC_TARGET");
    }
}
