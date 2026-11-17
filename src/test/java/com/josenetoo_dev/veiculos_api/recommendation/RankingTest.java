package com.josenetoo_dev.veiculos_api.recommendation;

import com.josenetoo_dev.veiculos_api.recommendation.domain.Ranking;
import com.josenetoo_dev.veiculos_api.recommendation.domain.Ranking.*;
import java.util.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RankingTest {
    final Listing fiat=new Listing(1,"Uno","Fiat","Uno","A",BigDecimal.valueOf(10000),"MG",1,0,false);
    final Listing ford=new Listing(2,"Ka","Ford","Ka","B",BigDecimal.valueOf(50000),"SP",2,0,false);
    final List<Listing> inventory=List.of(fiat,ford);
    final Profile empty=new Profile(List.of(),3);
    Profile interest(Listing item,double days,int halfLife) {
        return new Profile(Collections.nCopies(5,new Signal(item.features(),3,days)),halfLife);
    }
    @Test void coldStartUsesRecency() {assertEquals(inventory,Ranking.order(inventory,empty,empty));}
    @Test void historySurvivesNewSession() {
        assertEquals(ford,Ranking.order(inventory,empty,interest(ford,10,30)).getFirst());
    }
    @Test void recentEvidenceOvertakesHistoryThenFades() {
        var old=interest(fiat,0,30);
        assertEquals(ford,Ranking.order(inventory,interest(ford,0,3),old).getFirst());
        assertEquals(fiat,Ranking.order(inventory,interest(ford,12,3),old).getFirst());
    }
    @Test void anonymousSessionPersonalizes() {
        assertEquals(ford,Ranking.order(inventory,interest(ford,1,3),empty).getFirst());
    }
    @Test void halfLifeIsExponential() {
        assertEquals(2.5,Ranking.decay(10,6,3),1e-12);
        assertEquals(5,Ranking.decay(10,30,30),1e-12);
    }
}
