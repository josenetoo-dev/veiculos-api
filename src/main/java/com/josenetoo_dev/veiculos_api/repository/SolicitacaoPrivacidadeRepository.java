package com.josenetoo_dev.veiculos_api.repository;
import com.josenetoo_dev.veiculos_api.model.SolicitacaoPrivacidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
public interface SolicitacaoPrivacidadeRepository extends JpaRepository<SolicitacaoPrivacidade, Long> {
 Optional<SolicitacaoPrivacidade> findFirstByTitularIdAndTipoAndStatus(Long titularId, String tipo, String status);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select s from SolicitacaoPrivacidade s where s.id = :id")
 Optional<SolicitacaoPrivacidade> buscarParaAtendimento(Long id);
 Optional<SolicitacaoPrivacidade> findFirstByTitularIdAndTipoAndStatusIn(Long titularId, String tipo, java.util.List<String> estados);
 Page<SolicitacaoPrivacidade> findByTitularIdOrderByCriadoEmDesc(Long titularId, Pageable pageable);
}
