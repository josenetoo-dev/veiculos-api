package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.enums.Categoria;
import com.josenetoo_dev.veiculos_api.enums.StatusAnuncio;
import com.josenetoo_dev.veiculos_api.model.Anuncio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnuncioRepository extends JpaRepository<Anuncio, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Anuncio> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Anuncio a where a.id = :id")
    Optional<Anuncio> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    long countByAssinaturaPublicacaoId(Long assinaturaId);
    java.util.List<Anuncio> findByAssinaturaPublicacaoIdAndStatus(Long assinaturaId, StatusAnuncio status);

    long countByUsuarioIdAndStatus(Long usuarioId, StatusAnuncio status);
    java.util.List<Anuncio> findByUsuarioIdAndStatus(Long usuarioId, StatusAnuncio status);
    java.util.List<Anuncio> findByVeiculoIdAndStatus(Long veiculoId, StatusAnuncio status);

    @org.springframework.data.jpa.repository.Query("""
        select count(a) from Anuncio a where a.usuario.id = :usuario and a.nivelUmRevisado = true
        and exists (select v.id from Verificacao v where v.veiculo = a.veiculo and v.status = :aprovado)
        """)
    long contarVeiculosRevisados(Long usuario, com.josenetoo_dev.veiculos_api.enums.StatusVerificacao aprovado);

    boolean existsByCodigo(String codigo);

    Optional<Anuncio> findByCodigo(String codigo);

    Page<Anuncio> findByUsuarioId(Long usuarioId, Pageable pageable);
    Page<Anuncio> findByDestaqueTrueAndStatus(StatusAnuncio status, Pageable pageable);
    Page<Anuncio> findByCategoriaAndStatus(Categoria categoria, StatusAnuncio status, Pageable pageable);
    Page<Anuncio> findByStatusAndUsuarioId(StatusAnuncio status, Long usuarioId, Pageable pageable);

    Page<Anuncio> findByStatus(StatusAnuncio status, Pageable pageable);

    Page<Anuncio> findByCategoria(Categoria categoria, Pageable pageable);
}
