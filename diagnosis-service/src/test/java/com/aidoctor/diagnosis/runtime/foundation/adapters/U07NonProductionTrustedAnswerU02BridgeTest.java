package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.runtime.foundation.*;
import com.aidoctor.diagnosis.runtime.u02.U02ExecutionCommand;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class U07NonProductionTrustedAnswerU02BridgeTest {
    @Test
    public void blocksMissingConsumptionAndRejectsWaitingRunReuse() {
        ClinicalRunRepository runs = mock(ClinicalRunRepository.class);
        CanonicalBusinessEventRepository events = mock(CanonicalBusinessEventRepository.class);
        U02ExecutionCommand candidate = candidate();
        ClinicalRunRecord newRun = new ClinicalRunRecord(
                "new-run", "thread-1", "consult-1", "answer-1", 10,
                LocalDateTime.of(2026,10,14,0,0));
        CanonicalBusinessEventRecord event = new CanonicalBusinessEventRecord(
                "answer-1","consult-1","PATIENT_ANSWER","answer-idem",
                "digest",LocalDateTime.of(2026,10,14,0,0));
        when(runs.findById("new-run")).thenReturn(Optional.of(newRun));
        when(events.findById("answer-1")).thenReturn(Optional.of(event));
        U07NonProductionTrustedAnswerU02Bridge denied =
                new U07NonProductionTrustedAnswerU02Bridge(runs, events,
                        (c,waiting,cp,next,answer) -> false);
        assertTrue(assertThrows(IllegalStateException.class, () ->
                denied.requireAuthorizedNewRun(candidate,"checkpoint-1","new-run"))
                .getMessage().contains("F8_DURABLE_CONSUMPTION_NOT_CONFIRMED"));
        assertTrue(assertThrows(IllegalStateException.class, () ->
                denied.requireAuthorizedNewRun(candidate,"checkpoint-1","waiting-run"))
                .getMessage().contains("WAITING_RUN_REUSE"));
        U07NonProductionTrustedAnswerU02Bridge admitted =
                new U07NonProductionTrustedAnswerU02Bridge(runs, events,
                        (c,waiting,cp,next,answer) -> "consult-1".equals(c)
                                && "waiting-run".equals(waiting)
                                && "checkpoint-1".equals(cp)
                                && "new-run".equals(next)
                                && "answer-1".equals(answer));
        U02ExecutionCommand output =
                admitted.requireAuthorizedNewRun(candidate,"checkpoint-1","new-run");
        assertEquals("new-run",output.runId);
        assertEquals(10,output.baseClinicalStateVersion);
        assertEquals("PATIENT_REPORTED",output.sourceType);
    }

    private static U02ExecutionCommand candidate() {
        return new U02ExecutionCommand("patient-1", "consult-1", "thread-1",
                "waiting-run", "answer-1", "cdp-1", "No fever",
                "PATIENT_ANSWER", 10, "corr-1", "trace-1",
                Collections.emptyList());
    }
}
