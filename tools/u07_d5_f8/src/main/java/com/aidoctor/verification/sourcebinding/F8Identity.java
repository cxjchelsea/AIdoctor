package com.aidoctor.verification.sourcebinding;
import java.nio.charset.StandardCharsets;
import java.util.*;
final class F8Identity {
 static final String CONTRACT="synthetic-f8-final-v1", ISSUER="synthetic-f8-owner-v1", POLICY="synthetic-f8-policy-v1";
 static final String MANIFEST=BindingCodec.hash("synthetic-f8-owner-manifest-v1".getBytes(StandardCharsets.UTF_8));
 static String digest(String prefix,String...v){String[] names=new String[v.length];for(int i=0;i<v.length;i++)names[i]="f"+i;return BindingCodec.hash(BindingCodec.frame(prefix,names,v));}
 final SourceBindingVerifier.VerifiedCanonical source;final String decisionId,waitKey,scopeKey;
 F8Identity(SourceBindingVerifier.VerifiedCanonical s){source=s;decisionId=digest("f8-decision",s.canonicalId,CONTRACT);waitKey=digest("f8-wait",s.field(2),s.field(3),s.field(4),s.field(5),s.field(7),s.field(9));scopeKey=digest("f8-scope",s.field(4),s.field(5),s.field(6));}
 boolean scope(Map<String,Object> r){return Objects.equals(r.get("env"),source.field(2))&&Objects.equals(r.get("profile"),source.field(3))&&Objects.equals(r.get("tenant"),source.field(4))&&Objects.equals(r.get("consultation"),source.field(5))&&Objects.equals(r.get("question"),source.field(7))&&Objects.equals(r.get("parent_wait"),source.field(9))&&Objects.equals(r.get("wait_key"),waitKey);}
}
