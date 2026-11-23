package com.josenetoo_dev.veiculos_api.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AbuseRateFilterTest {
    @Test
    void spoofedForwardedAddressCannotBypass429AndRetryAfter() throws Exception {
        var store=mock(AbuseRateStore.class);
        when(store.consume("ip:auth:10.0.0.1",80,60)).thenReturn(7L);
        var filter=new AbuseRateFilter(store,true,true,80,120,120);
        var request=new MockHttpServletRequest("POST","/auth/login");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For","203.0.113.99");
        var response=new MockHttpServletResponse();
        filter.doFilter(request,response,(req,res)->fail("Limite deve impedir a ação"));
        assertEquals(429,response.getStatus());
        assertEquals("7",response.getHeader("Retry-After"));
        verify(store).consume("ip:auth:10.0.0.1",80,60);
        verifyNoMoreInteractions(store);
    }

    @Test
    void sharedIpIsNotAOneAccountRule() throws Exception {
        var store=mock(AbuseRateStore.class);
        var filter=new AbuseRateFilter(store,true,true,80,120,120);
        var invoked=new AtomicInteger();
        for(int i=0;i<2;i++) {
            var request=new MockHttpServletRequest("POST","/auth/register");
            request.setRemoteAddr("192.0.2.1");
            var response=new MockHttpServletResponse();
            filter.doFilter(request,response,(req,res)->invoked.incrementAndGet());
            assertEquals(200,response.getStatus());
        }
        assertEquals(2,invoked.get());
        verify(store,times(2)).consume("ip:auth:192.0.2.1",80,60);
    }
}
