package com.aidoctor.diagnosis.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 证据追踪器
 * 用于追踪诊断过程中的证据链
 */
public class EvidenceTracer {
    
    private List<EvidenceRecord> evidenceChain;
    
    public EvidenceTracer() {
        this.evidenceChain = new ArrayList<>();
    }
    
    /**
     * 添加证据记录
     */
    public void addEvidence(EvidenceRecord record) {
        evidenceChain.add(record);
    }
    
    /**
     * 获取证据链
     */
    public List<EvidenceRecord> getEvidenceChain() {
        return new ArrayList<>(evidenceChain);
    }
    
    /**
     * 清除证据链
     */
    public void clear() {
        evidenceChain.clear();
    }
    
    /**
     * 证据记录内部类
     */
    public static class EvidenceRecord {
        private String source;
        private String content;
        private Long timestamp;
        
        public EvidenceRecord(String source, String content) {
            this.source = source;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getSource() {
            return source;
        }
        
        public String getContent() {
            return content;
        }
        
        public Long getTimestamp() {
            return timestamp;
        }
    }
}

