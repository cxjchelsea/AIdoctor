package com.aidoctor.diagnosis.controller;

import com.aidoctor.contracts.v1.StateTypes;
import com.aidoctor.diagnosis.runtime.u01.U01NaturalLanguageStartService;
import com.aidoctor.diagnosis.runtime.u01.U01RawStartCommand;
import com.aidoctor.diagnosis.runtime.u01.U01Result;
import com.aidoctor.diagnosis.runtime.u02.U02ExecutionResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class V1NonProductionClinicalStartHandoffTest {
    private static final String URL = "/internal/nonproduction/v1/clinical/start-through-u02";
    private static final String BODY = "{\"eventId\":\"synthetic-event\","
            + "\"idempotencyKey\":\"synthetic-idem\",\"userId\":\"synthetic-user\","
            + "\"rawText\":\"synthetic headache\"}";

    @Test
    void actualU01ResultMustLeadToCommittedU02WithoutClaimingCompleteCare() throws Exception {
        U01NaturalLanguageStartService u01 = mock(U01NaturalLanguageStartService.class);
        U01Result started = mock(U01Result.class);
        when(started.getNextUnit()).thenReturn("U02");
        when(started.getConsultationId()).thenReturn("consult_synthetic");
        when(started.getRunId()).thenReturn("run_synthetic");
        when(started.getCdpId()).thenReturn("cdp_synthetic");
        when(u01.start(any(U01RawStartCommand.class))).thenReturn(started);
        StateTypes.CommitResult commit = new StateTypes.CommitResult();
        commit.status = "COMMITTED";
        U02ExecutionResult fact = new U02ExecutionResult("COMMITTED", null, "synthetic-call",
                null, null, commit, null);
        java.util.concurrent.atomic.AtomicInteger committed = new java.util.concurrent.atomic.AtomicInteger();
        V1NonProductionClinicalStartHandoffController.TrustedFactCommit handoff =
                (consultation, run, raw) -> {
                    assertEquals("consult_synthetic", consultation);
                    assertEquals("run_synthetic", run);
                    assertEquals("synthetic headache", raw);
                    committed.incrementAndGet();
                    return fact;
                };
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new V1NonProductionClinicalStartHandoffController(u01, handoff)).build();
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consultationId").value("consult_synthetic"))
                .andExpect(jsonPath("$.entryStatus")
                        .value("U02_COMMITTED_DOWNSTREAM_NOT_EXECUTED"))
                .andExpect(jsonPath("$.completeClinicalE2e").value(false))
                .andExpect(jsonPath("$.patientDeliveryAllowed").value(false));
        assertEquals(1, committed.get());
    }

    @Test
    void unexpectedU01RouteNeverInvokesU02() {
        U01NaturalLanguageStartService u01 = mock(U01NaturalLanguageStartService.class);
        U01Result started = mock(U01Result.class);
        when(started.getNextUnit()).thenReturn("U04");
        when(u01.start(any(U01RawStartCommand.class))).thenReturn(started);
        V1NonProductionClinicalStartHandoffController controller =
                new V1NonProductionClinicalStartHandoffController(u01,
                        (consultation, run, raw) -> {
                            throw new AssertionError("U02 must not run on rejected U01 route");
                        });
        V1NonProductionClinicalStartHandoffController.StartRequest req =
                new V1NonProductionClinicalStartHandoffController.StartRequest();
        req.eventId = "synthetic-event";
        req.idempotencyKey = "synthetic-idem";
        req.userId = "synthetic-user";
        req.rawText = "synthetic headache";
        assertTrue(assertThrows(IllegalStateException.class, () -> controller.start(req))
                .getMessage().contains("ROUTE_NOT_AUTHORIZED"));
    }
}
