package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.enums.*;
import com.josenetoo_dev.veiculos_api.model.Verificacao;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface VerificacaoRepository extends JpaRepository<Verificacao, Long> {
    Optional<Verificacao> findByUsuarioIdentidadeId(Long usuarioId);
    Optional<Verificacao> findByVeiculoId(Long veiculoId);
    Page<Verificacao> findBySolicitanteId(Long usuarioId, Pageable pageable);
    Page<Verificacao> findByStatus(StatusVerificacao status, Pageable pageable);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Verificacao v where v.id = :id")
    Optional<Verificacao> findByIdForUpdate(@Param("id") Long id);

    boolean existsByUsuarioIdentidadeIdAndStatus(Long id, StatusVerificacao status);
    boolean existsByVeiculoIdAndStatus(Long id, StatusVerificacao status);
}
