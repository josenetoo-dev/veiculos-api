package com.josenetoo_dev.veiculos_api.dto.comercial;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DeclararVendaRequest(@NotNull @DecimalMin(value = "0.01") BigDecimal valorFinal,
                                   @NotNull @PastOrPresent LocalDate dataVenda) {}
