package com.josenetoo_dev.veiculos_api.recommendation.application;

import com.josenetoo_dev.veiculos_api.recommendation.domain.Ranking.Listing;
import java.math.BigDecimal;
import java.util.List;

public interface ListingCatalog {
    List<Listing> load(BigDecimal min, BigDecimal max, String uf, boolean sponsored);
}
