package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.model.EvidenciaVerificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EvidenciaVerificacaoRepository extends JpaRepository<EvidenciaVerificacao, Long> {
    List<EvidenciaVerificacao> findByVerificacaoId(Long id);
    boolean existsByVerificacaoIdAndTipo(Long id, com.josenetoo_dev.veiculos_api.enums.TipoEvidencia tipo);
}
