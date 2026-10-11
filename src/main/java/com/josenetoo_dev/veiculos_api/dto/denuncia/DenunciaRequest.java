package com.josenetoo_dev.veiculos_api.dto.denuncia;

import com.josenetoo_dev.veiculos_api.enums.CategoriaDenuncia;
import jakarta.validation.constraints.*;

public record DenunciaRequest(
    @NotNull @Positive Long anuncioId,
    @NotNull CategoriaDenuncia categoria,
    @NotBlank @Size(min=10,max=1000) String relato
) {}
