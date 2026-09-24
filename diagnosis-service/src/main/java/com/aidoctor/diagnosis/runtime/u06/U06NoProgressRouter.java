package com.aidoctor.diagnosis.runtime.u06;

public final class U06NoProgressRouter {
    public static final String F1="F1";
    public static final String F3="F3";
    public static final String RETURN_U01_F1="RETURN_U01_F1";
    public static final String READINESS_REEVALUATION_REQUIRED_U05_D03="READINESS_REEVALUATION_REQUIRED_U05_D03";

    public String route(String noProgressClass) {
        if(F1.equals(noProgressClass))return RETURN_U01_F1;
        if(F3.equals(noProgressClass))return READINESS_REEVALUATION_REQUIRED_U05_D03;
        throw new IllegalArgumentException("unsupported no-progress class");
    }
}
