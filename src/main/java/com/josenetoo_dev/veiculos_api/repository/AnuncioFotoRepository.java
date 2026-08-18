package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.model.AnuncioFoto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnuncioFotoRepository extends JpaRepository<AnuncioFoto, Long> {
    Page<AnuncioFoto> findByAnuncioId(Long anuncioId, Pageable pageable);
}
