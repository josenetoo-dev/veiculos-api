package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.enums.Categoria;
import com.josenetoo_dev.veiculos_api.enums.StatusAnuncio;
import com.josenetoo_dev.veiculos_api.model.Anuncio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnuncioRepository extends JpaRepository<Anuncio, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Anuncio a where a.id = :id")
    Optional<Anuncio> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByCodigo(String codigo);

    Optional<Anuncio> findByCodigo(String codigo);

    Page<Anuncio> findByDestaqueTrue(Pageable pageable);

    Page<Anuncio> findByStatus(StatusAnuncio status, Pageable pageable);

    Page<Anuncio> findByCategoria(Categoria categoria, Pageable pageable);
}
