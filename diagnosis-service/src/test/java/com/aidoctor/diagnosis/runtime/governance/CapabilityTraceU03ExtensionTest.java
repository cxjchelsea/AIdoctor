package com.aidoctor.diagnosis.runtime.governance;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CapabilityTraceU03ExtensionTest {
    @Test
    void traceCanBindRuleAndKnowledgeReleaseBeforeSuccess() {
        LocalDateTime now = LocalDateTime.now();
        CapabilityCallTraceRecord trace = new CapabilityCallTraceRecord(
                "call-1", "consult-1", "thread-1", "run-1", "event-1",
                "U03", "C02", "binding-1", Integer.valueOf(4), now);

        trace.bindReleases("rules-v1", "knowledge-v1");
        trace.succeed("result-1", "decision-1", "proposal-1", "commit-1", Integer.valueOf(5), now.plusSeconds(1));

        assertEquals("rules-v1", trace.getRuleReleaseRef());
        assertEquals("knowledge-v1", trace.getKnowledgeReleaseRef());
        assertEquals(CapabilityCallTraceRecord.SUCCEEDED, trace.getCallStatus());
        assertEquals(Integer.valueOf(5), trace.getClinicalStateVersionAfter());
    }

    @Test
    void failedCapabilityCanStillTraceCommittedFailureOutcomeWithoutFakeReleaseRefs() {
        LocalDateTime now = LocalDateTime.now();
        CapabilityCallTraceRecord trace = new CapabilityCallTraceRecord(
                "call-2", "consult-1", "thread-1", "run-1", "event-2",
                "U03", "C02", "binding-1", Integer.valueOf(4), now);

        trace.failWithOutcome(
                "U03_GOVERNED_INVOCATION_FAILED",
                "decision-2",
                "proposal-2",
                "commit-2",
                Integer.valueOf(5),
                now.plusSeconds(1));

        assertEquals(CapabilityCallTraceRecord.FAILED, trace.getCallStatus());
        assertEquals("decision-2", trace.getDecisionRef());
        assertEquals("proposal-2", trace.getProposalRef());
        assertEquals("commit-2", trace.getCommitRef());
        assertNull(trace.getRuleReleaseRef());
        assertNull(trace.getKnowledgeReleaseRef());
    }
}
