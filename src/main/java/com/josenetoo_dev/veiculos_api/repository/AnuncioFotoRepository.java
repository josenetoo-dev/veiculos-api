package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.model.AnuncioFoto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnuncioFotoRepository extends JpaRepository<AnuncioFoto, Long> {
    // Suporte a URLs de diferentes bases de host usadas por versões antigas.
    Optional<AnuncioFoto> findFirstByUrlEndingWith(String suffix);
    long countByAnuncioId(Long anuncioId);
    Page<AnuncioFoto> findByAnuncioId(Long anuncioId, Pageable pageable);
    Optional<AnuncioFoto> findFirstByAnuncioIdOrderByOrdemAsc(Long anuncioId);
}