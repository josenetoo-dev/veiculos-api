package com.josenetoo_dev.veiculos_api.recommendation.domain;

import java.util.*;
import java.math.BigDecimal;

public final class Ranking {
    private Ranking() {}
    public record Listing(long id, String titulo, String marca, String modelo, String categoria,
                          BigDecimal preco, String uf, long vehicle, int popularity, boolean sponsored, long seller) {
        public Listing(long id, String titulo, String marca, String modelo, String categoria,
                       BigDecimal preco, String uf, long vehicle, int popularity, boolean sponsored) {
            this(id,titulo,marca,modelo,categoria,preco,uf,vehicle,popularity,sponsored,-1);
        }
        public List<String> features() {
            return Arrays.asList(marca, modelo, categoria, Long.toString(preco.longValue()/10000), uf);
        }
    }
    public record Signal(List<String> features, double strength, double days) {}
    public static double decay(double initial, double days, double halfLife) {
        return initial * Math.pow(2, -Math.max(0, days) / halfLife);
    }
    public static final class Profile {
        private final List<Map<String, Double>> dimensions = new ArrayList<>();
        private final double[] maxima = new double[5];
        private final double confidence;
        public Profile(List<Signal> signals, double halfLife) {
            for (int i=0; i<5; i++) dimensions.add(new HashMap<>());
            double evidence = 0;
            for (var signal : signals) {
                double age = decay(1, signal.days, halfLife);
                evidence += age;
                for (int i=0; i<5; i++) {
                    String value = signal.features.get(i);
                    if (value == null) continue;
                    double total = dimensions.get(i).merge(value, age * signal.strength, Double::sum);
                    maxima[i] = Math.max(maxima[i], total);
                }
            }
            confidence = Math.min(1, evidence/4);
        }
        double score(List<String> features) {
            double[] weights = {.20,.20,.15,.30,.15};
            double score = 0;
            for (int i=0; i<5; i++) {
                String value = features.get(i);
                if (value == null || maxima[i] == 0) continue;
                var counts = dimensions.get(i);
                double match = counts.getOrDefault(value,0d);
                if (i == 3) {
                    long band = Long.parseLong(value);
                    match = Math.max(match, .5 * Math.max(counts.getOrDefault(""+(band-1),0d),
                            counts.getOrDefault(""+(band+1),0d)));
                }
                score += weights[i]*match/maxima[i];
            }
            return score;
        }
    }
    public static List<Listing> order(List<Listing> inventory, Profile session, Profile history) {
        double recent = session.confidence * (1 - .25 * history.confidence);
        double old = (1-recent)*history.confidence;
        var scores = new HashMap<Long,Double>();
        for (int i=0; i<inventory.size(); i++) {
            var item = inventory.get(i);
            double baseline = .6/(1+ i/20d) + .4*item.popularity/(5d+item.popularity);
            scores.put(item.id, recent*session.score(item.features()) + old*history.score(item.features())
                    + (1-recent-old)*baseline);
        }
        return inventory.stream().sorted(Comparator.comparingDouble((Listing a)->scores.get(a.id))
                .reversed().thenComparing(Comparator.comparingLong(Listing::id).reversed())).toList();
    }
}
