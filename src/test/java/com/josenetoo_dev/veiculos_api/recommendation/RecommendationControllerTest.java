package com.josenetoo_dev.veiculos_api.recommendation;

import com.josenetoo_dev.veiculos_api.recommendation.api.RecommendationController;
import com.josenetoo_dev.veiculos_api.recommendation.application.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecommendationControllerTest {
    @Test
    void eventQuotaRejectsBeforeWritingAndIsolatesAccounts() {
        var signals = mock(Signals.class);
        var controller = new RecommendationController(signals,mock(HomeService.class));
        var request = new MockHttpServletRequest();
        var event = new Signals.Event(UUID.randomUUID(),Signals.Kind.DETAILS,1L,null,null,true);
        var cookie = UUID.randomUUID();
        for (int i=0; i<60; i++) controller.event(event,()->"1",cookie,request);
        var failure = assertThrows(ResponseStatusException.class,
                ()->controller.event(event,()->"1",cookie,request));
        assertEquals(429,failure.getStatusCode().value());
        verify(signals,times(60)).record(event,cookie.toString(),1L);
        controller.event(event,()->"2",cookie,request);
        verify(signals).record(event,cookie.toString(),2L);
    }
}
