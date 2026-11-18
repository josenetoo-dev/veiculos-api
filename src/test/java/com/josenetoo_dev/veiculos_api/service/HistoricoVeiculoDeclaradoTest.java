package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.anuncio_dto.AnuncioRequest;
import com.josenetoo_dev.veiculos_api.enums.RespostaHistorico;
import com.josenetoo_dev.veiculos_api.model.HistoricoVeiculoDeclarado;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HistoricoVeiculoDeclaradoTest {
    @Test void allSevenAnswersAreRequired() {
        try(var factory=Validation.buildDefaultValidatorFactory()){
            Validator validator=factory.getValidator();
            AnuncioRequest request=new AnuncioRequest();
            HistoricoVeiculoDeclarado history=new HistoricoVeiculoDeclarado();
            history.setColisao(RespostaHistorico.NAO);
            history.setRepintura(RespostaHistorico.NAO);
            history.setPecasAcidente(RespostaHistorico.NAO);
            history.setLeilao(RespostaHistorico.NAO);
            history.setAlagamento(RespostaHistorico.NAO);
            history.setProblemaMecanico(RespostaHistorico.NAO);
            request.setHistoricoVeiculo(history);

            assertTrue(validator.validate(request).stream()
                    .anyMatch(error->error.getPropertyPath().toString().equals("historicoVeiculo.sinistro")));

            history.setSinistro(RespostaHistorico.NAO_SEI_INFORMAR);
            assertFalse(validator.validate(history).stream()
                    .anyMatch(error->error.getPropertyPath().toString().equals("sinistro")));
        }
    }
}
