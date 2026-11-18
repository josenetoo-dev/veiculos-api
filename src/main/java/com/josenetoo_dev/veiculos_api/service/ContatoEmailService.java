package com.josenetoo_dev.veiculos_api.service;

import com.josenetoo_dev.veiculos_api.dto.usuario_dto.ConfirmarContatoEmailRequest;
import com.josenetoo_dev.veiculos_api.enums.StatusUsuario;
import com.josenetoo_dev.veiculos_api.exception.ex.*;
import com.josenetoo_dev.veiculos_api.model.Usuario;
import com.josenetoo_dev.veiculos_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Confirma acesso ao e-mail cadastrado; não equivale à verificação de identidade.
 * Usa token forte de uso único, hash no banco, expiração e cooldown por usuário.
 */
@Service
@RequiredArgsConstructor
public class ContatoEmailService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UsuarioRepository usuarios;
    private final ContatoEmailMailer mailer;
    private final OneTimeEmailCodeService otp;
    private final com.josenetoo_dev.veiculos_api.security.JwtUtil jwtUtil;

    private Usuario userLocked() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new CredenciaisInvalidasException("Não autenticado");
        }
        try {
            return usuarios.findByIdForUpdate(Long.valueOf(auth.getName()))
                    .orElseThrow(() -> new CredenciaisInvalidasException("Conta inexistente"));
        } catch (NumberFormatException e) {
            throw new CredenciaisInvalidasException("Credenciais inválidas");
        }
    }

    public record Situacao(boolean codigoEnviado, long reenviarEmSegundos) {}

    @Transactional
    public Situacao situacao() {
        Usuario usuario = userLocked();
        var agora = LocalDateTime.now(ZoneOffset.UTC);
        boolean enviado = usuario.getContactEmailTokenHash() != null
                && usuario.getContactEmailExpiresAt() != null
                && usuario.getContactEmailExpiresAt().isAfter(agora);
        long restante = enviado && usuario.getContactEmailRequestedAt() != null
                ? Math.max(0, java.time.Duration.between(agora, usuario.getContactEmailRequestedAt().plusMinutes(1)).toSeconds() + 1) : 0;
        return new Situacao(enviado, restante);
    }

    @Transactional
    public void solicitar() {
        // Sem SMTP autorizado e configurado, o fluxo fica indisponível.
        if (!mailer.available()) throw new EmailChangeUnavailableException();
        solicitarPara(userLocked());
    }

    @Transactional
    public void solicitarCadastro(Long id) {
        if (!mailer.available()) return;
        solicitarPara(usuarios.findByIdForUpdate(id).orElseThrow());
    }

    private void solicitarPara(Usuario usuario) {
        if (usuario.getStatus() != StatusUsuario.PENDING_CONTACT_VERIFICATION) {
            throw new VerificacaoIndisponivelException("Confirmação de contato não disponível neste estado");
        }
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (usuario.getContactEmailRequestedAt() != null
                && usuario.getContactEmailRequestedAt().isAfter(now.minusMinutes(1))) {
            throw new EmailChangeThrottledException();
        }
        String token = otp.reserve();
        usuario.setContactEmailFailedAttempts(0);
        usuario.setContactEmailTokenHash(otp.hash(token));
        usuario.setContactEmailExpiresAt(now.plusMinutes(15));
        usuario.setContactEmailRequestedAt(now);
        usuarios.saveAndFlush(usuario);
        // Falha SMTP reverte o desafio; nenhum código é revelado na API.
        mailer.send(usuario.getEmail(), token);
    }

    @Transactional(noRollbackFor = EmailVerificationCodeException.class)
    public String confirmar(ConfirmarContatoEmailRequest request) {
        Usuario usuario = userLocked();
        if (usuario.getStatus() != StatusUsuario.PENDING_CONTACT_VERIFICATION
                || usuario.getContactEmailTokenHash() == null || usuario.getContactEmailExpiresAt() == null
                || !usuario.getContactEmailExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new EmailVerificationCodeException();
        }
        byte[] expected = usuario.getContactEmailTokenHash().getBytes(StandardCharsets.US_ASCII);
        byte[] actual = otp.hash(request.token()).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(expected, actual)) {
            usuario.setContactEmailFailedAttempts(usuario.getContactEmailFailedAttempts() + 1);
            if (usuario.getContactEmailFailedAttempts() >= 5) {
                usuario.setContactEmailTokenHash(null);
                usuario.setContactEmailExpiresAt(null);
            }
            usuarios.saveAndFlush(usuario);
            throw new EmailVerificationCodeException();
        }
        usuario.setStatus(StatusUsuario.ACTIVE);
        usuario.setContactEmailVerifiedAt(LocalDateTime.now(ZoneOffset.UTC));
        usuario.setContactEmailTokenHash(null);
        usuario.setContactEmailExpiresAt(null);
        usuario.setContactEmailRequestedAt(null);
        usuario.setTokenVersion(usuario.getTokenVersion() + 1); // revoga JWT anterior
        usuarios.saveAndFlush(usuario);
        return jwtUtil.gerarToken(String.valueOf(usuario.getId()));
    }

}
