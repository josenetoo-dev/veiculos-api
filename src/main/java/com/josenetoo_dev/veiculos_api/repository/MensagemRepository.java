package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.model.Mensagem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MensagemRepository extends JpaRepository<Mensagem, Long> {
    Page<Mensagem> findByPropostaIdOrderByCriadoEmAsc(Long propostaId, Pageable pageable);

    Page<Mensagem> findByPropostaIdAndExcluidaEmIsNullOrderByCriadoEmAsc(Long propostaId, Pageable pageable);

    boolean existsByPropostaIdAndEnvioPropostaId(Long propostaId, String envioPropostaId);

    interface Pendencia {
        Long getPropostaId();
        long getQuantidade();
    }

    @Query("""
        select m.proposta.id as propostaId, count(m) as quantidade from Mensagem m
        where m.acompanharLeitura = true and m.excluidaEm is null and m.lidaEm is null and m.remetente.id <> :usuario
        and (m.proposta.comprador.id = :usuario or m.proposta.anuncio.usuario.id = :usuario)
        group by m.proposta.id
        """)
    java.util.List<Pendencia> pendencias(Long usuario);

    @Modifying
    @Query("""
        update Mensagem m set m.lidaEm = :agora where m.proposta.id = :proposta
        and m.id in :ids and m.remetente.id <> :usuario and m.lidaEm is null and m.excluidaEm is null
        """)
    int marcarLidas(Long proposta, Long usuario, java.util.List<Long> ids, java.time.LocalDateTime agora);

    @Modifying
    @Query("DELETE FROM Mensagem m WHERE m.proposta.anuncio.id = :anuncioId")
    void deleteByPropostaAnuncioId(Long anuncioId);
}
