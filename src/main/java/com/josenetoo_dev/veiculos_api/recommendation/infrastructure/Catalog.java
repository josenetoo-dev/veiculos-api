package com.josenetoo_dev.veiculos_api.recommendation.infrastructure;

import com.josenetoo_dev.veiculos_api.recommendation.domain.Ranking.Listing;
import com.josenetoo_dev.veiculos_api.recommendation.application.ListingCatalog;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class Catalog implements ListingCatalog {
    private final JdbcClient db;
    public Catalog(JdbcClient db) { this.db = db; }
    private static final String BENEFIT = """
        EXISTS (SELECT 1 FROM assinatura_vendedor s JOIN recommendation_home_benefit b ON b.subscription_id=s.id
        WHERE s.id=a.assinatura_publicacao_id AND s.vendedor_id=a.usuario_id AND a.destaque=true
        AND s.status='ATIVA' AND s.situacao_financeira='CONFIRMADO_EXTERNAMENTE'
        AND s.inicio_vigencia<=:now AND s.vencimento_em>:now)
        """;
    public List<Listing> load(BigDecimal min, BigDecimal max, String uf, boolean sponsored) {
        String sql = """
            SELECT a.id,a.titulo,a.marca,a.modelo,a.categoria,a.preco,a.uf,a.veiculo_id,
              (SELECT COUNT(*) FROM proposta p WHERE p.anunciante_id=a.id) popularity
            FROM anuncio a WHERE a.status='ATIVO' AND
            """ + (sponsored ? "" : "NOT ") + BENEFIT;
        var params = new HashMap<String,Object>();
        params.put("now", java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        if (min != null) { sql += " AND a.preco>=:min"; params.put("min", min); }
        if (max != null) { sql += " AND a.preco<=:max"; params.put("max", max); }
        if (uf != null) { sql += " AND a.uf=:uf"; params.put("uf", uf); }
        return db.sql(sql+" ORDER BY a.criado_em DESC,a.id DESC LIMIT "+(sponsored ? 8 : 240))
                .params(params).query((r,i)->new Listing(r.getLong(1), r.getString(2), r.getString(3),
                r.getString(4),r.getString(5),r.getBigDecimal(6),r.getString(7),r.getLong(8),
                r.getInt(9),sponsored)).list();
    }
}
