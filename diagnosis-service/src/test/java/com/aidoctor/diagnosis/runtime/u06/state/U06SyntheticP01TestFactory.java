package com.aidoctor.diagnosis.runtime.u06.state;

import com.aidoctor.diagnosis.state.committer.SyntheticStateSnapshot;
import com.aidoctor.diagnosis.state.committer.SyntheticVersionedStateRepository;
import com.aidoctor.diagnosis.state.committer.ports.StateRepositoryPort;

import java.time.Clock;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class U06SyntheticP01TestFactory {
    private U06SyntheticP01TestFactory(){}

    public static U06SyntheticP01Runtime create(String storeRef,String consultationId,String cdpId,Clock clock){
        Map<String,Object> patient=new LinkedHashMap<String,Object>();
        patient.put("information_gaps",new LinkedHashMap<String,Object>());
        patient.put("questions",new LinkedHashMap<String,Object>());
        Map<String,Object> root=new LinkedHashMap<String,Object>();
        root.put("patient_state",patient);
        Map<String,SyntheticStateSnapshot> states=new HashMap<String,SyntheticStateSnapshot>();
        states.put(cdpId,new SyntheticStateSnapshot(0,root));
        final SyntheticVersionedStateRepository repository=new SyntheticVersionedStateRepository(states);
        U06SyntheticP01Runtime.Backend backend=new U06SyntheticP01Runtime.Backend(){
            public StateRepositoryPort repositoryPort(){return repository;}
            public U06SyntheticP01Runtime.StateView readCurrent(String id){
                SyntheticStateSnapshot s=repository.snapshot(id);
                return new U06SyntheticP01Runtime.StateView(s.version(),s.state());
            }
            public int mutationCount(){return repository.mutationCount();}
            public String readStoreRef(){return storeRef;}
            public String commitStoreRef(){return storeRef;}
        };
        return U06SyntheticP01Runtime.createInjected(storeRef,consultationId,cdpId,clock,backend);
    }
}
