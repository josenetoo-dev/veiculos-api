package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.model.AceiteContrato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AceiteContratoRepository extends JpaRepository<AceiteContrato, Long> {
    Optional<AceiteContrato> findByUsuarioIdAndContratoId(Long usuarioId, Long contratoId);
    List<AceiteContrato> findByUsuarioIdOrderByAceitoEmDesc(Long usuarioId);
}
