package com.aidoctor.diagnosis.agent;

import com.aidoctor.diagnosis.entity.CDP;
import com.aidoctor.diagnosis.service.cdp.CDPManager;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ToolCallerTest {

    @Test
    void sessionLookupUsesCanonicalCdpManagerMethod() {
        CDPManager manager = mock(CDPManager.class);
        ToolCaller caller = new ToolCaller();
        CDP cdp = CDP.builder().id("cdp-1").sessionId("session-1").build();
        when(manager.getCDPById("cdp-1")).thenReturn(Optional.of(cdp));
        ReflectionTestUtils.setField(caller, "cdpManager", manager);

        String sessionId = ReflectionTestUtils.invokeMethod(caller, "getSessionIdFromCdp", "cdp-1");

        assertEquals("session-1", sessionId);
        verify(manager).getCDPById("cdp-1");
    }

    @Test
    void cdpFieldCompatibilityReadKeepsDdxAsList() {
        ToolCaller caller = new ToolCaller();
        CDP cdp = new CDP();
        cdp.setDdx(Collections.singletonList(
            Collections.<String, Object>singletonMap("disease_name", "candidate")));

        Object value = ReflectionTestUtils.invokeMethod(caller, "getCdpFieldValue", cdp, "cdp.ddx");

        assertEquals(cdp.getDdx(), value);
    }
}
