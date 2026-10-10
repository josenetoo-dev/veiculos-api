package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.enums.StatusSessaoBiometria;
import com.josenetoo_dev.veiculos_api.model.SessaoBiometria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface SessaoBiometriaRepository extends JpaRepository<SessaoBiometria, String> {
    Optional<SessaoBiometria> findByIdAndVerificacaoId(String id, Long verificacaoId);
    Optional<SessaoBiometria> findFirstByVerificacaoIdOrderByCriadoEmDesc(Long verificacaoId);
    long countByVerificacaoIdAndCriadoEmAfter(Long verificacaoId, LocalDateTime limit);
    boolean existsByVerificacaoIdAndDocumentoEvidenciaIdAndStatusAndFinalizadoEmAfter(
            Long verificacaoId, Long documentoEvidenciaId, StatusSessaoBiometria status,
            LocalDateTime minFinalizadoEm);
}
