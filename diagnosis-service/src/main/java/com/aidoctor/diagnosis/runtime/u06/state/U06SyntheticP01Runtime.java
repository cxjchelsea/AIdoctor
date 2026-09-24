package com.aidoctor.diagnosis.runtime.u06.state;
import com.aidoctor.contracts.v1.ContractVersion;import com.aidoctor.contracts.v1.FoundationTypes;import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.runtime.u06.U06Ids;import com.aidoctor.diagnosis.state.committer.*;import com.aidoctor.diagnosis.state.committer.ports.*;
import java.time.Clock;import java.util.*;
public final class U06SyntheticP01Runtime {
    public static final String PRODUCER="u06-runtime",CAPABILITY_ID="u06-state-writer",CAPABILITY_VERSION="1.0.0",SOURCE="RULE_DERIVED",SENSITIVITY="INTERNAL_SENSITIVE";
    private final String storeRef,consultationId,cdpId;private final Backend backend;private final StateCommitter committer;private final Map<String,StablePatch> stablePatches=new LinkedHashMap<String,StablePatch>();private boolean readBackMismatchOnce;
    private U06SyntheticP01Runtime(String storeRef,String consultationId,String cdpId,Backend backend,StateCommitter committer){this.storeRef=req(storeRef);this.consultationId=req(consultationId);this.cdpId=req(cdpId);this.backend=backend;this.committer=committer;}
    public static U06SyntheticP01Runtime createInjected(String storeRef,String consultationId,String cdpId,Clock clock,Backend backend){
        if(backend==null)throw new IllegalArgumentException("backend is required");
        StateRepositoryPort port=backend.repositoryPort();
        if(port==null)throw new IllegalArgumentException("backend repository port is required");
        StateCommitter c=new StateCommitter(port,new Cap(),new Field(),new Consent(),new Source(),new Idem(),new Audit(clock),new Events(),clock);
        return new U06SyntheticP01Runtime(storeRef,consultationId,cdpId,backend,c);
    }
    public synchronized CommitEvidence commit(String effect,String proposal,List<OperationIntent>intents,List<String>evidence,String corr,String trace,String createdAt){
        return commitAtBaseVersion(effect,proposal,intents,evidence,corr,trace,createdAt,readCurrent().version);
    }
    public synchronized CommitEvidence commitAtBaseVersion(String effect,String proposal,List<OperationIntent>intents,List<String>evidence,String corr,String trace,String createdAt,int baseVersion){
        if(baseVersion<0)throw new IllegalArgumentException("baseVersion must be non-negative");
        String fp=fingerprint(intents);StablePatch stable=stablePatches.get(effect);if(stable!=null&&!stable.fp.equals(fp))throw new IllegalStateException("U06_STATE_EFFECT_REPLAY_CONFLICT");
        if(stable==null){StateTypes.StatePatch p=build(effect,proposal,intents,evidence,corr,trace,createdAt,baseVersion);stable=new StablePatch(fp,p);stablePatches.put(effect,stable);}
        StateTypes.CommitResult result=committer.commit(stable.patch);
        StateView readBack=readCurrent();
        if(readBackMismatchOnce&&"COMMITTED".equals(result.status)){readBackMismatchOnce=false;readBack=corruptReadBack(readBack);}
        return new CommitEvidence(result,readBack,backend.mutationCount(),storeRef);
    }
    public synchronized void injectReadBackMismatchOnce(){readBackMismatchOnce=true;}
    @SuppressWarnings("unchecked")
    private StateView corruptReadBack(StateView original){
        Map<String,Object> root=deepCopyMap(original.state);
        Object raw=root.get("patient_state");
        if(raw instanceof Map)((Map<String,Object>)raw).remove("f3_gap_assessment");
        return new StateView(original.version,root);
    }
    @SuppressWarnings("unchecked")
    private static Map<String,Object> deepCopyMap(Map<String,Object> source){
        Map<String,Object> out=new LinkedHashMap<String,Object>();
        for(Map.Entry<String,Object>e:source.entrySet()){
            Object v=e.getValue();
            if(v instanceof Map)v=deepCopyMap((Map<String,Object>)v);
            else if(v instanceof List)v=new ArrayList<Object>((List<Object>)v);
            out.put(e.getKey(),v);
        }
        return out;
    }
    public StateView readCurrent(){return backend.readCurrent(cdpId);}
    public OperationIntent upsert(String path,Map<String,Object>value){return new OperationIntent(readCurrent().exists(path)?"REPLACE":"ADD",path,value);}
    public String getStoreRef(){return storeRef;}public String getReadStoreRef(){return backend.readStoreRef();}public String getCommitStoreRef(){return backend.commitStoreRef();}public int getMutationCount(){return backend.mutationCount();}
    private StateTypes.StatePatch build(String effect,String proposal,List<OperationIntent>intents,List<String>evidence,String corr,String trace,String createdAt,int base){
        StateTypes.StatePatch p=new StateTypes.StatePatch();p.contractVersion=ContractVersion.CONTRACT_VERSION;p.envelope=new FoundationTypes.ContractEnvelope();p.envelope.contractName="StatePatch";p.envelope.contractVersion=ContractVersion.CONTRACT_VERSION;
        p.envelope.messageId=U06Ids.hash("u06msg",effect,proposal);p.envelope.correlationId=req(corr);p.envelope.traceId=req(trace);p.envelope.createdAt=req(createdAt);p.envelope.producer=PRODUCER;p.envelope.capabilityId=CAPABILITY_ID;p.envelope.capabilityVersion=CAPABILITY_VERSION;
        p.cdpId=cdpId;p.baseVersion=Integer.valueOf(base);p.patchId=U06Ids.hash("u06patch",effect,proposal);p.idempotencyKey=U06Ids.hash("u06idem",effect,proposal,"1");p.operations=new ArrayList<StateTypes.StatePatchOperation>();
        for(OperationIntent i:intents){authorize(i.path);StateTypes.StatePatchOperation o=new StateTypes.StatePatchOperation();o.op=i.op;o.path=i.path;o.value=i.value;o.expectedCurrentValue=null;o.source=SOURCE;o.sensitivity=SENSITIVITY;p.operations.add(o);}
        p.reasonCode="U06_STATE_WRITE";p.evidenceRefs=evidence==null?new ArrayList<String>():new ArrayList<String>(evidence);p.producer=PRODUCER;p.createdAt=createdAt;return p;}
    private static void authorize(String path){if("/patient_state/f3_gap_assessment".equals(path)||"/patient_state/pending_question".equals(path)||path.matches("^/patient_state/information_gaps/[A-Za-z0-9][A-Za-z0-9._:-]*$")||path.matches("^/patient_state/questions/[A-Za-z0-9][A-Za-z0-9._:-]*$"))return;throw new IllegalArgumentException("U06_FIELD_PERMISSION_DENIED");}
    private static String fingerprint(List<OperationIntent>i){List<String>v=new ArrayList<String>();for(OperationIntent x:i){v.add(x.path);v.add(String.valueOf(x.value));}return U06Ids.hash("u06statefp",v.toArray(new String[v.size()]));}
    public interface Backend{
        StateRepositoryPort repositoryPort();
        StateView readCurrent(String cdpId);
        int mutationCount();
        String readStoreRef();
        String commitStoreRef();
    }
    public static final class OperationIntent{final String op,path;final Map<String,Object>value;public OperationIntent(String op,String path,Map<String,Object>value){if(!"ADD".equals(op)&&!"REPLACE".equals(op)&&!"REMOVE".equals(op))throw new IllegalArgumentException("unsupported op");this.op=op;this.path=req(path);this.value=value;}}
    public static final class StateView{private final int version;private final Map<String,Object>state;StateView(int v,Map<String,Object>s){version=v;state=s;}public int getVersion(){return version;}public Map<String,Object>getState(){return state;}
        @SuppressWarnings("unchecked") public boolean exists(String pointer){return value(pointer)!=null;}
        @SuppressWarnings("unchecked") public Object value(String pointer){if(pointer==null||!pointer.startsWith("/"))return null;Object cur=state;for(String token:pointer.substring(1).split("/")){if(!(cur instanceof Map))return null;Map<String,Object>m=(Map<String,Object>)cur;if(!m.containsKey(token))return null;cur=m.get(token);}return cur;}
        @SuppressWarnings("unchecked") public String mapString(String pointer,String key){Object v=value(pointer);if(!(v instanceof Map))return null;Object x=((Map<String,Object>)v).get(key);return x==null?null:String.valueOf(x);}}
    public static final class CommitEvidence{private final StateTypes.CommitResult result;private final StateView readBack;private final int mutationCount;private final String storeRef;CommitEvidence(StateTypes.CommitResult r,StateView v,int m,String s){result=r;readBack=v;mutationCount=m;storeRef=s;}public StateTypes.CommitResult getResult(){return result;}public StateView getReadBack(){return readBack;}public int getMutationCount(){return mutationCount;}public String getStoreRef(){return storeRef;}}
    private static final class StablePatch{final String fp;final StateTypes.StatePatch patch;StablePatch(String fp,StateTypes.StatePatch p){this.fp=fp;patch=p;}}
    private static final class Cap implements CapabilityPolicyPort{public CapabilityDecision evaluate(String id,String v){return CAPABILITY_ID.equals(id)&&CAPABILITY_VERSION.equals(v)?CapabilityDecision.authorized():CapabilityDecision.denied();}}
    private static final class Field implements FieldPermissionPort{public FieldPermissionDecision evaluate(String p,String id){try{authorize(p);return CAPABILITY_ID.equals(id)?FieldPermissionDecision.authorized():FieldPermissionDecision.denied();}catch(RuntimeException e){return FieldPermissionDecision.denied();}}}
    private static final class Consent implements ConsentPolicyPort{public ConsentDecision evaluate(String cdp,String id){return CAPABILITY_ID.equals(id)?ConsentDecision.authorized():ConsentDecision.denied();}}
    private static final class Source implements SourceValidationPort{public SourceDecision evaluate(String s){return SOURCE.equals(s)?SourceDecision.authorized():SourceDecision.denied();}}
    private static final class Idem implements IdempotencyPort{private final Map<String,E>m=new HashMap<String,E>();public synchronized Decision inspect(String k,String f){E e=m.get(k);if(e==null)return Decision.absent();if(!e.f.equals(f))return Decision.mismatch();return e.r==null?Decision.reservedSameFingerprint():Decision.completedSameFingerprint(e.r);}
        public synchronized Decision reserve(String k,String f){E e=m.get(k);if(e!=null){if(!e.f.equals(f))return Decision.mismatch();return e.r==null?Decision.reservedSameFingerprint():Decision.completedSameFingerprint(e.r);}m.put(k,new E(f,null));return Decision.acquired();}
        public synchronized void complete(String k,String f,StateTypes.CommitResult r){E e=m.get(k);if(e==null||!e.f.equals(f))throw new IllegalStateException("idempotency mismatch");m.put(k,new E(f,r));}public synchronized void release(String k,String f){E e=m.get(k);if(e!=null&&e.r==null&&e.f.equals(f))m.remove(k);}private static final class E{final String f;final StateTypes.CommitResult r;E(String f,StateTypes.CommitResult r){this.f=f;this.r=r;}}}
    private static final class Audit implements AuditPort{private final Clock clock;Audit(Clock c){clock=c;}public FoundationTypes.AuditRef record(AuditCommand c){FoundationTypes.AuditRef r=new FoundationTypes.AuditRef();r.contractVersion=ContractVersion.CONTRACT_VERSION;r.auditId=U06Ids.hash("u06audit",c.patchId,c.status,c.reasonCode);r.auditType=c.auditType;r.auditVersion=Integer.valueOf(1);r.createdAt=clock.instant().toString();r.accessLevel="INTERNAL";r.phiCapable=Boolean.FALSE;return r;}}
    private static final class Events implements CommitEventEvidencePort{public void record(InternalCommitEventEvidence e){}}
    private static String req(String v){if(v==null||v.trim().isEmpty())throw new IllegalArgumentException("value required");return v.trim();}
}
