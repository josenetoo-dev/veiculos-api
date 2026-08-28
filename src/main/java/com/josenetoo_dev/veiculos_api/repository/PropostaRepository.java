package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.model.Proposta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PropostaRepository extends JpaRepository<Proposta, Long> {
    Page<Proposta> findByCompradorIdOrAnuncioUsuarioId(Long compradorId, Long anuncianteId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Proposta p WHERE p.anuncio.id = :anuncioId")
    void deleteByAnuncioId(Long anuncioId);
}