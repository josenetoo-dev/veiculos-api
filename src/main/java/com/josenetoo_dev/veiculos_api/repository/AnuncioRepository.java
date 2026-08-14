package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.enums.Categoria;
import com.josenetoo_dev.veiculos_api.enums.StatusAnuncio;
import com.josenetoo_dev.veiculos_api.model.Anuncio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnuncioRepository extends JpaRepository<Anuncio, Long> {
    boolean existsByCodigo(String codigo);

    Optional<Anuncio> findByCodigo(String codigo);

    Page<Anuncio> findByDestaqueTrue(Pageable pageable);

    Page<Anuncio> findByStatus(StatusAnuncio status, Pageable pageable);

    Page<Anuncio> findByCategoria(Categoria categoria, Pageable pageable);
}
