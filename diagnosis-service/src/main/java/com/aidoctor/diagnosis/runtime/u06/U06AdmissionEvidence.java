package com.aidoctor.diagnosis.runtime.u06;

public final class U06AdmissionEvidence {
    public static final String GATE_ALLOW_CURRENT="ALLOW_CURRENT";
    public static final String GATE_RESTRICTED_CURRENT="RESTRICTED_CURRENT";
    public static final String GATE_STALE="STALE";

    public static final String PERMISSION_NOT_REQUIRED="NOT_REQUIRED";
    public static final String PERMISSION_ALLOWED="ALLOWED";
    public static final String PERMISSION_DENIED="DENIED";
    public static final String PERMISSION_UNAVAILABLE="UNAVAILABLE";

    public static final String DEPENDENCY_ACTIVE_SYNTHETIC="ACTIVE_SYNTHETIC";
    public static final String DEPENDENCY_EXPIRED="EXPIRED";
    public static final String DEPENDENCY_REQUIRED_MISSING="REQUIRED_MISSING";
    public static final String DEPENDENCY_REAL_PROFILE_BLOCKED="REAL_PROFILE_BLOCKED";
    public static final String DEPENDENCY_SYNTHETIC_TYPED_NOT_REAL_P06="SYNTHETIC_TYPED_NOT_REAL_P06";
    public static final String DEPENDENCY_NOT_APPLICABLE_FAMILY="NOT_APPLICABLE_FAMILY";

    private final boolean sourceAuthorityPresent;
    private final boolean sourceAuthorityCurrent;
    private final boolean routeConsequenceValid;
    private final String gateState;
    private final String permissionState;
    private final boolean consultationCdpMatch;
    private final String dependencyBindingState;
    private final boolean sourceSuperseded;
    private final boolean syntheticExecutionMarkerValid;

    public U06AdmissionEvidence(boolean sourceAuthorityPresent,
                                boolean sourceAuthorityCurrent,
                                boolean routeConsequenceValid,
                                String gateState,
                                String permissionState,
                                boolean consultationCdpMatch,
                                String dependencyBindingState,
                                boolean sourceSuperseded,
                                boolean syntheticExecutionMarkerValid) {
        this.sourceAuthorityPresent=sourceAuthorityPresent;
        this.sourceAuthorityCurrent=sourceAuthorityCurrent;
        this.routeConsequenceValid=routeConsequenceValid;
        this.gateState=req(gateState,"gateState");
        this.permissionState=req(permissionState,"permissionState");
        this.consultationCdpMatch=consultationCdpMatch;
        this.dependencyBindingState=req(dependencyBindingState,"dependencyBindingState");
        this.sourceSuperseded=sourceSuperseded;
        this.syntheticExecutionMarkerValid=syntheticExecutionMarkerValid;
    }

    public static U06AdmissionEvidence syntheticCurrentAllow() {
        return new U06AdmissionEvidence(true,true,true,GATE_ALLOW_CURRENT,PERMISSION_NOT_REQUIRED,
                true,DEPENDENCY_ACTIVE_SYNTHETIC,false,true);
    }

    public boolean isSourceAuthorityPresent(){return sourceAuthorityPresent;}
    public boolean isSourceAuthorityCurrent(){return sourceAuthorityCurrent;}
    public boolean isRouteConsequenceValid(){return routeConsequenceValid;}
    public String getGateState(){return gateState;}
    public String getPermissionState(){return permissionState;}
    public boolean isConsultationCdpMatch(){return consultationCdpMatch;}
    public String getDependencyBindingState(){return dependencyBindingState;}
    public boolean isSourceSuperseded(){return sourceSuperseded;}
    public boolean isSyntheticExecutionMarkerValid(){return syntheticExecutionMarkerValid;}

    public boolean isDependencyUsableForProfileB() {
        return DEPENDENCY_ACTIVE_SYNTHETIC.equals(dependencyBindingState)
                ||DEPENDENCY_SYNTHETIC_TYPED_NOT_REAL_P06.equals(dependencyBindingState)
                ||DEPENDENCY_NOT_APPLICABLE_FAMILY.equals(dependencyBindingState);
    }

    private static String req(String v,String n) {
        if(v==null||v.trim().isEmpty())throw new IllegalArgumentException(n+" is required");
        return v.trim();
    }
}
