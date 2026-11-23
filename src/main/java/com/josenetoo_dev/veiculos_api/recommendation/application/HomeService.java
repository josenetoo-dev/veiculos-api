package com.josenetoo_dev.veiculos_api.recommendation.application;

import com.josenetoo_dev.veiculos_api.recommendation.domain.Ranking;
import com.josenetoo_dev.veiculos_api.recommendation.domain.Ranking.*;
import org.springframework.stereotype.Service;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.math.BigDecimal;

@Service
public class HomeService {
    public record Carousel(String brand,List<Listing> listings) {}
    public record Home(List<Listing> sponsored,List<String> brands,List<Listing> recommendations,
                       List<Carousel> carousels,List<String> categories) {}
    private final ListingCatalog catalog;
    private final Signals signals;
    @org.springframework.beans.factory.annotation.Value("${app.recommendations.sponsored-share:0.30}")
    private double sponsoredShare = 0.30;
    public HomeService(ListingCatalog catalog, Signals signals) {this.catalog=catalog; this.signals=signals;}
    public Home home(BigDecimal min,BigDecimal max,String uf,String session,Long user) {
        var ads=catalog.load(min,max,uf,true);
        var inventory=catalog.load(min,max,uf,false);
        List<Listing> ranked;
        List<Listing> paidRanked;
        try {
            var recent=signals.profile(session,user,false);
            var history=signals.profile(session,user,true);
            ranked=Ranking.order(inventory,recent,history);
            paidRanked=Ranking.order(ads,recent,history);
        } catch (RuntimeException failure) {
            LoggerFactory.getLogger(HomeService.class).warn("Personalização indisponível",failure);
            var empty=new Profile(List.of(),1);
            ranked=Ranking.order(inventory,empty,empty);
            paidRanked=Ranking.order(ads,empty,empty);
        }
        var seen=new HashSet<Long>();
        double share=Math.max(0,Math.min(.5,sponsoredShare));
        int slots=ranked.isEmpty()?0:Math.min(8,(int)Math.floor(Math.min(16,ranked.size())*share/(1-share)));
        var sponsored=rotate(paidRanked,slots,java.time.LocalDate.now(java.time.ZoneOffset.UTC).toEpochDay())
                .stream().filter(a->seen.add(a.vehicle())).toList();
        var organic=ranked.stream().filter(a->seen.add(a.vehicle())).toList();
        var byBrand=new LinkedHashMap<String,List<Listing>>();
        for (var a:organic) byBrand.computeIfAbsent(a.marca(),ignored->new ArrayList<>()).add(a);
        var brands=byBrand.keySet().stream().limit(12).toList();
        var sections=brands.stream().filter(b->byBrand.get(b).size()>=2).limit(3)
                .map(b->new Carousel(b,byBrand.get(b).stream().limit(8).toList())).toList();
        var brandCount=new HashMap<String,Integer>();
        var picks=new LinkedHashSet<Listing>();
        for (var a:organic) {
            if (brandCount.merge(a.marca(),1,Integer::sum)<=3) picks.add(a);
            if (picks.size()==16) break;
        }
        for (var a:organic) {
            if (picks.size()==16) break;
            picks.add(a);
        }
        return new Home(sponsored,brands,List.copyOf(picks),sections,
                organic.stream().map(Listing::categoria).distinct().sorted().toList());
    }

    /** Round-robin sellers after relevance ranking; daily offset changes the first seller. */
    public static List<Listing> rotate(List<Listing> ranked, int slots, long day) {
        if (slots<=0) return List.of();
        var groups=new LinkedHashMap<Long,ArrayDeque<Listing>>();
        for (var ad:ranked) groups.computeIfAbsent(ad.seller(),ignored->new ArrayDeque<>()).add(ad);
        var sellers=new ArrayList<>(groups.keySet());
        if (sellers.isEmpty()) return List.of();
        Collections.rotate(sellers,(int)Math.floorMod(day,sellers.size()));
        var result=new ArrayList<Listing>();
        while (result.size()<slots) {
            boolean added=false;
            for (long seller:sellers) {
                var next=groups.get(seller).poll();
                if (next!=null) {result.add(next);added=true;}
                if (result.size()==slots) break;
            }
            if (!added) break;
        }
        return result;
    }
}
