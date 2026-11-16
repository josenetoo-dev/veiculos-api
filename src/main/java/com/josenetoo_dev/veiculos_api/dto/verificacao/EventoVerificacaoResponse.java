package com.josenetoo_dev.veiculos_api.dto.verificacao;

import com.josenetoo_dev.veiculos_api.enums.*;
import java.time.LocalDateTime;

/** Somente operadores autorizados; sem IDs de banco nem documentos. */
public record EventoVerificacaoResponse(String responsavel, StatusVerificacao resultado,
        CategoriaResultadoVerificacao categoria, String justificativa, LocalDateTime ocorridoEm) {}
