package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.model.DispensaBiometria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispensaBiometriaRepository extends JpaRepository<DispensaBiometria,Long> {
    boolean existsByVerificacaoIdAndDocumentoEvidenciaId(Long verificacaoId,Long evidenceId);
}
