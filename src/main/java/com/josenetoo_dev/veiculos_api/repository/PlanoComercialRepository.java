package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.model.PlanoComercial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanoComercialRepository extends JpaRepository<PlanoComercial, Long> {
    List<PlanoComercial> findByAtivoTrueOrderByMensalidadeAsc();
    Optional<PlanoComercial> findByCodigoAndAtivoTrue(String codigo);
    Optional<PlanoComercial> findByCodigo(String codigo);
}
