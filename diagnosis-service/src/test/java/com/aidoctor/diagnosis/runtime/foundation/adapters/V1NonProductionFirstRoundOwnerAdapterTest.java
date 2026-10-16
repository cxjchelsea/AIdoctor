package com.aidoctor.diagnosis.runtime.foundation.adapters;

import com.aidoctor.diagnosis.controller.V1NonProductionFirstRoundWaitController;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class V1NonProductionFirstRoundOwnerAdapterTest {
    private V1NonProductionFirstRoundOwnerAdapter owner(
            V1NonProductionFirstRoundOwnerAdapter.AuthorizedQuestionContent content) {
        return new V1NonProductionFirstRoundOwnerAdapter(
                mock(V1NonProductionFirstRoundOwnerAdapter.AuthorizedStateOwner.class),
                mock(V1NonProductionFirstRoundOwnerAdapter.DeliveryOwnerAuthority.class),
                new V1NonProductionDeliveredQuestionReadback(
                        mock(com.aidoctor.diagnosis.runtime.foundation.RuntimeWaitCheckpointRepository.class),
                        mock(com.aidoctor.diagnosis.repository.CDPRepository.class)),
                content);
    }

    @Test void textMustComeFromBoundTrustedOwner() {
        V1NonProductionFirstRoundOwnerAdapter.AuthorizedQuestionContent content =
                mock(V1NonProductionFirstRoundOwnerAdapter.AuthorizedQuestionContent.class);
        V1NonProductionFirstRoundWaitController.DeliveredQuestion question =
                new V1NonProductionFirstRoundWaitController.DeliveredQuestion(
                        "question-fixture", "fixture-content-ref");
        V1NonProductionFirstRoundOwnerAdapter adapter = owner(content);
        assertThrows(IllegalStateException.class, () ->
                adapter.requireAuthorizedQuestionText("consult", "run", question));
        when(content.requireText("consult", "run",
                "question-fixture", "fixture-content-ref"))
                .thenReturn("Fixture-only follow-up question");
        assertEquals("Fixture-only follow-up question",
                adapter.requireAuthorizedQuestionText("consult", "run", question));
        verify(content, times(2)).requireText(
                "consult", "run", "question-fixture", "fixture-content-ref");
    }
}
