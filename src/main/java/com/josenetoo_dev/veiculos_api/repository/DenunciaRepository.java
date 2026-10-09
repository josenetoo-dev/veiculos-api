package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.enums.StatusDenuncia;
import com.josenetoo_dev.veiculos_api.model.Denuncia;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface DenunciaRepository extends JpaRepository<Denuncia, Long> {
    boolean existsByAnuncioIdAndDenuncianteId(Long anuncioId, Long userId);
    Page<Denuncia> findByDenuncianteId(Long userId, Pageable pageable);
    Page<Denuncia> findByStatus(StatusDenuncia status, Pageable pageable);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Denuncia d where d.id = :id")
    Optional<Denuncia> findByIdForUpdate(@Param("id") Long id);
}
