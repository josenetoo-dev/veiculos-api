package com.josenetoo_dev.veiculos_api.recommendation;

import com.josenetoo_dev.veiculos_api.recommendation.application.*;
import com.josenetoo_dev.veiculos_api.recommendation.domain.Ranking.Listing;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HomeServiceTest {
    @Test
    void brokenProfileFallsBackAndKeepsDiversityAndAdvertisingBoundaries() {
        var catalog = mock(ListingCatalog.class);
        var signals = mock(Signals.class);
        var min = BigDecimal.valueOf(10000);
        var max = BigDecimal.valueOf(60000);
        var sponsored = new Listing(1,"Patrocinado","A","M","SEMINOVOS",min,"MG",1,0,true);
        var inventory = new ArrayList<Listing>();
        for (int i=1; i<=24; i++) {
            inventory.add(new Listing(i,"Anúncio","Marca"+i%4,"M","SEMINOVOS",min,"MG",i,0,false));
        }
        when(catalog.load(min,max,"MG",true)).thenReturn(List.of(sponsored));
        when(catalog.load(min,max,"MG",false)).thenReturn(inventory);
        when(signals.profile("session",null,false)).thenThrow(new IllegalStateException("Simulated"));
        var home = new HomeService(catalog,signals).home(min,max,"MG","session",null);
        assertEquals(List.of(sponsored),home.sponsored());
        assertEquals(16,home.recommendations().size());
        assertTrue(home.recommendations().stream().noneMatch(a->a.vehicle()==1 || a.sponsored()));
        assertEquals(16,home.recommendations().stream().map(Listing::vehicle).distinct().count());
        assertEquals(4,home.brands().size());
        assertEquals(3,home.carousels().size());
        assertEquals(4,home.recommendations().subList(0,12).stream().map(Listing::marca).distinct().count());
        verify(catalog).load(min,max,"MG",true);
        verify(catalog).load(min,max,"MG",false);
        verifyNoMoreInteractions(catalog);
    }
}
