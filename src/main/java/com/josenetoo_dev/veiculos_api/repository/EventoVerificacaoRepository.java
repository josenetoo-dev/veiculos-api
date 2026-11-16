package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.model.EventoVerificacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoVerificacaoRepository extends JpaRepository<EventoVerificacao, Long> {
    org.springframework.data.domain.Page<EventoVerificacao> findByVerificacaoIdOrderByCriadoEmDescIdDesc(
            Long verificacaoId, org.springframework.data.domain.Pageable pageable);
}
