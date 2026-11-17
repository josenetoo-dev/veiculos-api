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
    public HomeService(ListingCatalog catalog, Signals signals) {this.catalog=catalog; this.signals=signals;}
    public Home home(BigDecimal min,BigDecimal max,String uf,String session,Long user) {
        var ads=catalog.load(min,max,uf,true);
        var inventory=catalog.load(min,max,uf,false);
        List<Listing> ranked;
        try {
            ranked=Ranking.order(inventory,signals.profile(session,user,false),signals.profile(session,user,true));
        } catch (RuntimeException failure) {
            LoggerFactory.getLogger(HomeService.class).warn("Personalização indisponível",failure);
            var empty=new Profile(List.of(),1);
            ranked=Ranking.order(inventory,empty,empty);
        }
        var seen=new HashSet<Long>();
        var sponsored=ads.stream().filter(a->seen.add(a.vehicle())).toList();
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
}
