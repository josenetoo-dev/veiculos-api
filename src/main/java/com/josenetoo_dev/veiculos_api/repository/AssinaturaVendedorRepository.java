package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.enums.StatusAssinatura;
import com.josenetoo_dev.veiculos_api.model.AssinaturaVendedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;

public interface AssinaturaVendedorRepository extends JpaRepository<AssinaturaVendedor, Long> {
    Page<AssinaturaVendedor> findByVendedorIdOrderByCriadoEmDesc(Long vendedorId, Pageable pageable);
    Optional<AssinaturaVendedor> findFirstByVendedorIdAndStatusOrderByCriadoEmDesc(Long vendedorId, StatusAssinatura status);
    List<AssinaturaVendedor> findByVendedorIdAndStatusOrderByCriadoEmDesc(Long vendedorId, StatusAssinatura status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AssinaturaVendedor s where s.id = :id")
    Optional<AssinaturaVendedor> findByIdForUpdate(@Param("id") Long id);
}
