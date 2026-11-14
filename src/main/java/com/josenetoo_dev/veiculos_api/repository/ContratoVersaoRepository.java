package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.model.ContratoVersao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContratoVersaoRepository extends JpaRepository<ContratoVersao, Long> {
    Optional<ContratoVersao> findFirstByPublicadoTrueOrderByCriadoEmDesc();
    Optional<ContratoVersao> findFirstByOrderByCriadoEmDesc();
}
