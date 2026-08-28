package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.model.Mensagem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MensagemRepository extends JpaRepository<Mensagem, Long> {
    Page<Mensagem> findByPropostaIdOrderByCriadoEmAsc(Long propostaId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Mensagem m WHERE m.proposta.anuncio.id = :anuncioId")
    void deleteByPropostaAnuncioId(Long anuncioId);
}
