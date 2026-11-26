package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.model.Proposta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PropostaRepository extends JpaRepository<Proposta, Long> {
    boolean existsByAnuncioId(Long anuncioId);
    Page<Proposta> findByCompradorIdOrAnuncioUsuarioId(Long compradorId, Long anuncianteId, Pageable pageable);

    java.util.Optional<Proposta> findFirstByCompradorIdAndAnuncioIdOrderByIdAsc(Long compradorId, Long anuncioId);

    java.util.List<Proposta> findAllByCompradorIdAndAnuncioIdOrderByIdAsc(Long compradorId, Long anuncioId);

    @Query("""
        select p from Proposta p where
        (p.comprador.id = :primeiro and p.anuncio.usuario.id = :segundo)
        or (p.comprador.id = :segundo and p.anuncio.usuario.id = :primeiro)
        order by p.id
        """)
    java.util.List<Proposta> entreParticipantes(Long primeiro, Long segundo, Pageable pagina);

    @Modifying
    @Query("DELETE FROM Proposta p WHERE p.anuncio.id = :anuncioId")
    void deleteByAnuncioId(Long anuncioId);
}
