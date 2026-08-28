package com.josenetoo_dev.veiculos_api.exception;

import com.josenetoo_dev.veiculos_api.exception.ex.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            EmailJaCadastradoException.class,
            PropostaJaCanceladaException.class,
            PropostaJaAceitaException.class,
            PropostaJaNegadaException.class,
            NaoPodeCancelarAndNegarPropostaException.class,
            ContrapropostaJaRealizadaException.class,
            AnuncioIndisponivelException.class
    }) public ResponseEntity<ErroResponse> tratarConflitos(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErroResponse(ex.getMessage(), 409));
    }

    @ExceptionHandler({
            UsuarioNaoEncontradoException.class,
            AnuncioNaoEncontradoException.class,
            PropostaNaoEncontradaException.class,
            FotoNaoEncontradaException.class
    }) public ResponseEntity<  ErroResponse> naoEncontrado(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErroResponse(ex.getMessage(), 404));
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ErroResponse> credenciaisInvalidas(CredenciaisInvalidasException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErroResponse(ex.getMessage(), 401));
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResponse> acessoNegado(AcessoNegadoException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErroResponse(ex.getMessage(), 403));
    }
}
