package com.josenetoo_dev.veiculos_api.exception;

import com.josenetoo_dev.veiculos_api.exception.ex.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.util.LinkedHashMap;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private ProblemDetail problem(int status, String detail, String code) {
        var p = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
        p.setProperty("code", code);
        return p;
    }
    @ExceptionHandler({EmailJaCadastradoException.class, PropostaJaCanceladaException.class,
            PropostaJaAceitaException.class, PropostaJaNegadaException.class,
            NaoPodeCancelarAndNegarPropostaException.class, ContrapropostaJaRealizadaException.class,
            AnuncioIndisponivelException.class, VerificacaoIndisponivelException.class, DataIntegrityViolationException.class})
    public ProblemDetail conflict(Exception ex) { return problem(409, "Operação em conflito com o estado atual", "CONFLICT"); }
    @ExceptionHandler({UsuarioNaoEncontradoException.class, AnuncioNaoEncontradoException.class,
            PropostaNaoEncontradaException.class, FotoNaoEncontradaException.class, VerificacaoNaoEncontradaException.class, DenunciaNaoEncontradaException.class})
    public ProblemDetail notFound(Exception ex) { return problem(404, "Recurso não encontrado", "NOT_FOUND"); }
    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ProblemDetail unauthorized(Exception ex) { return problem(401, "Credenciais inválidas", "AUTHENTICATION_REQUIRED"); }
    @ExceptionHandler({AcessoNegadoException.class, AccessDeniedException.class})
    public ProblemDetail forbidden(Exception ex) { return problem(403, "Acesso negado", "ACCESS_DENIED"); }
    @ExceptionHandler(com.josenetoo_dev.veiculos_api.exception.ex.EmailChangeUnavailableException.class)
    public ProblemDetail mailUnavailable(Exception ex) {
        return problem(503, "Alteração de e-mail temporariamente indisponível", "EMAIL_CHANGE_UNAVAILABLE");
    }
    @ExceptionHandler(com.josenetoo_dev.veiculos_api.exception.ex.EmailChangeThrottledException.class)
    public ProblemDetail mailThrottled(Exception ex) {
        return problem(429, "Aguarde antes de pedir outro código", "EMAIL_CHANGE_RATE_LIMIT");
    }
    @ExceptionHandler(ArmazenamentoPrivadoIndisponivelException.class)
    public ProblemDetail privateEvidenceStorageUnavailable(Exception ex) {
        return problem(503, "Recebimento de documentos temporariamente indisponível", "PRIVATE_STORAGE_UNAVAILABLE");
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail invalid(Exception ex) { return problem(400, "Requisição inválida", "INVALID_REQUEST"); }
    @ExceptionHandler(Exception.class)
    public ProblemDetail internal(Exception ex) { return problem(500, "Falha interna ao processar requisição", "INTERNAL_ERROR"); }
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var body = problem(400, "Campos inválidos", "VALIDATION_ERROR");
        var fields = new LinkedHashMap<String, String>();
        ex.getBindingResult().getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(), "Valor inválido"));
        body.setProperty("fieldErrors", fields); // Nunca incluir rejectedValue ou mensagens que interpolam dados.
        return handleExceptionInternal(ex, body, headers, status, request);
    }
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (!(body instanceof ProblemDetail p) || p.getProperties() == null || !p.getProperties().containsKey("code")) {
            body = problem(status.value(), status.value() == 413 ? "Upload excede limite permitido" : "Requisição não pode ser processada", "HTTP_" + status.value());
        }
        return super.handleExceptionInternal(ex, body, headers, status, request);
    }
}
