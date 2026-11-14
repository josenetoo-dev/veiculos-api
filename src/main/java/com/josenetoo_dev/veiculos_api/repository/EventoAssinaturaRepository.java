package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.model.EventoAssinatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventoAssinaturaRepository extends JpaRepository<EventoAssinatura, Long> {
    List<EventoAssinatura> findByAssinaturaIdOrderByCriadoEmAsc(Long assinaturaId);
}
