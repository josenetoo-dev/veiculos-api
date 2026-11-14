package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.model.VendaDeclarada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VendaDeclaradaRepository extends JpaRepository<VendaDeclarada, Long> {
    Optional<VendaDeclarada> findByAnuncioId(Long anuncioId);
    Page<VendaDeclarada> findByVendedorIdOrderByDeclaradoEmDesc(Long vendedorId, Pageable pageable);
}
