package com.josenetoo_dev.veiculos_api.recommendation.application;

import com.josenetoo_dev.veiculos_api.recommendation.domain.Ranking.*;
import jakarta.validation.constraints.*;
import java.time.*;

import java.util.*;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class Signals {
    public enum Kind {
        VIEW(1), DETAILS(3), SEARCH(4), FAVORITE(7), CONTACT(10);
        final int weight;
        Kind(int weight) { this.weight=weight; }
    }
    public record Event(@NotNull UUID eventId,@NotNull Kind kind,@Positive Long listingId,
                        @Size(max=80) String brand, @Positive Long dwellMillis, boolean consent) {}
    private final JdbcTemplate db;
    private final Environment config;
    public Signals(JdbcTemplate db, Environment config) { this.db=db; this.config=config; }

    @org.springframework.transaction.annotation.Transactional
    public void record(Event e, String session, Long user) {
        if (user!=null) lockUser(user);
        if (!e.consent()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Consentimento necessário");
        if (e.kind()==Kind.VIEW && (e.dwellMillis()==null || e.dwellMillis()<5000)) throw invalid();
        List<String> features;
        String target;
        if (e.kind()==Kind.SEARCH) {
            if (e.listingId()!=null || e.brand()==null || e.brand().isBlank()) throw invalid();
            var brands=db.queryForList("SELECT marca FROM anuncio WHERE status='ATIVO' AND marca=? LIMIT 1",
                    String.class,e.brand().strip());
            if (brands.isEmpty()) throw invalid();
            target=brands.getFirst();
            features=Arrays.asList(target,null,null,null,null);
        } else {
            if (e.listingId()==null || e.brand()!=null) throw invalid();
            if (e.kind()==Kind.FAVORITE && user==null) throw invalid();
            if (e.kind()==Kind.CONTACT && (user==null || db.queryForList(
                    "SELECT id FROM proposta WHERE comprador_id=? AND anunciante_id=? LIMIT 1",
                    Long.class,user,e.listingId()).isEmpty())) throw invalid();
            var rows=db.query("""
                    SELECT marca,modelo,categoria,preco,uf FROM anuncio
                    WHERE status='ATIVO' AND id=?
                    """,(r,i)->Arrays.asList(r.getString(1),r.getString(2),r.getString(3),
                    Long.toString(r.getBigDecimal(4).longValue()/10000),r.getString(5)),e.listingId());
            if (rows.isEmpty()) throw invalid();
            features=rows.getFirst();
            target=e.listingId().toString();
        }
        int weight=config.getProperty("recommendation.weights."+e.kind().name().toLowerCase(Locale.ROOT),
                Integer.class,e.kind().weight);
        var now=LocalDateTime.now(ZoneOffset.UTC);
        try {
            db.update("""
                INSERT INTO recommendation_signal(session_key,user_id,event_id,kind,target_key,event_day,
                created_at,strength,brand,model_name,category,price_band,region) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,session,user,e.eventId().toString(),e.kind().name(),target,now.toLocalDate(),now,
                    Math.clamp(weight,0,100),features.get(0),features.get(1),features.get(2),features.get(3),features.get(4));
        } catch (DuplicateKeyException alreadyRecorded) {
            // Mesmo resultado para reenvio ou repetição semântica no dia UTC.
        }
    }
    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Interação inválida");
    }
    public Profile profile(String session, Long user, boolean historical) {
        var now=LocalDateTime.now(ZoneOffset.UTC);
        int days=historical ? 180 : 7;
        double halfLife=config.getProperty(historical?"recommendation.history-half-life":"recommendation.session-half-life",
                Double.class,historical?30d:3d);
        if (!(halfLife>0 && Double.isFinite(halfLife))) throw new IllegalArgumentException("Meia-vida inválida");
        if ((historical && user==null) || (!historical && session==null)) return new Profile(List.of(),halfLife);
        String filter=historical?"user_id=?":"session_key=? AND "+(user==null?"user_id IS NULL":"user_id=?");
        List<Object> args=new ArrayList<>();
        args.add(historical?user:session);
        if (!historical && user!=null) args.add(user);
        args.add(now.minusDays(days));
        return new Profile(db.query("""
            SELECT brand,model_name,category,price_band,region,strength,created_at FROM recommendation_signal
            WHERE """+" "+filter+" AND created_at>=? ORDER BY created_at DESC,id DESC LIMIT 500",
                (r,i)->new Signal(Arrays.asList(r.getString(1),r.getString(2),r.getString(3),r.getString(4),
                        r.getString(5)),r.getDouble(6),Duration.between(r.getTimestamp(7).toLocalDateTime(),now)
                        .toSeconds()/86400d),args.toArray()),halfLife);
    }
    private void lockUser(long user) {
        db.queryForObject("SELECT id FROM usuario WHERE id=? FOR UPDATE",Long.class,user);
    }
    @org.springframework.transaction.annotation.Transactional
    public void delete(long user) {
        lockUser(user);
        db.update("DELETE FROM recommendation_signal WHERE user_id=?",user);
    }
    @Scheduled(fixedDelayString="PT6H",initialDelayString="PT5M")
    public void expire() {
        var now=LocalDateTime.now(ZoneOffset.UTC);
        db.update("DELETE FROM recommendation_signal WHERE created_at<? OR (user_id IS NULL AND created_at<?)",
                now.minusDays(180),now.minusDays(7));
        db.update("UPDATE recommendation_signal SET session_key=NULL WHERE session_key IS NOT NULL AND created_at<?",
                now.minusDays(7));
    }
}
