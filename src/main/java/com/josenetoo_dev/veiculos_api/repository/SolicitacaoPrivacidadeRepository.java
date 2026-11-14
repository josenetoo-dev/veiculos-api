package com.josenetoo_dev.veiculos_api.repository;
import com.josenetoo_dev.veiculos_api.model.SolicitacaoPrivacidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
public interface SolicitacaoPrivacidadeRepository extends JpaRepository<SolicitacaoPrivacidade, Long> {
 Optional<SolicitacaoPrivacidade> findFirstByTitularIdAndTipoAndStatus(Long titularId, String tipo, String status);
 Page<SolicitacaoPrivacidade> findByTitularIdOrderByCriadoEmDesc(Long titularId, Pageable pageable);
}
