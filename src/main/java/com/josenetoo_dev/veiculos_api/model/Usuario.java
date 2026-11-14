package com.josenetoo_dev.veiculos_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ToString.Include
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String senha;

    @Column(nullable = false)
    private String telefone;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "cpf_ciphertext", columnDefinition = "LONGTEXT")
    private String cpfCiphertext;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "cpf_lookup_hash", length = 64, unique = true)
    private String cpfLookupHash;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "cpf_final_digitos", length = 2)
    private String cpfFinalDigitos;

    @jakarta.persistence.Column(name = "nome_completo_vendedor_ciphertext", columnDefinition = "LONGTEXT")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String nomeCompletoVendedorCiphertext;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private com.josenetoo_dev.veiculos_api.enums.Role role = com.josenetoo_dev.veiculos_api.enums.Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    private com.josenetoo_dev.veiculos_api.enums.StatusUsuario status = com.josenetoo_dev.veiculos_api.enums.StatusUsuario.PENDING_CONTACT_VERIFICATION;

    @Column(nullable = false)
    private long tokenVersion = 0;

    // A troca de e-mail só é concluída após comprovar acesso ao endereço novo.
    @Column(name = "pending_email", length = 254)
    private String pendingEmail;

    @Column(name = "pending_email_token_hash", length = 64)
    private String pendingEmailTokenHash;

    @Column(name = "pending_email_expires_at")
    private LocalDateTime pendingEmailExpiresAt;

    @Column(name = "pending_email_requested_at")
    private LocalDateTime pendingEmailRequestedAt;

    // Contato verificado separadamente da análise manual de identidade.
    @Column(name = "contact_email_token_hash", length = 64)
    private String contactEmailTokenHash;

    @Column(name = "contact_email_expires_at")
    private LocalDateTime contactEmailExpiresAt;

    @Column(name = "contact_email_requested_at")
    private LocalDateTime contactEmailRequestedAt;

    @Column(name = "contact_email_verified_at")
    private LocalDateTime contactEmailVerifiedAt;

    // Token de recuperação: somente hash no banco, desafio de uso único.
    @Column(name = "password_reset_token_hash", length = 64)
    private String passwordResetTokenHash;

    @Column(name = "password_reset_expires_at")
    private LocalDateTime passwordResetExpiresAt;

    @Column(name = "password_reset_requested_at")
    private LocalDateTime passwordResetRequestedAt;

    @Column(name = "password_reset_failed_attempts", nullable = false)
    private int passwordResetFailedAttempts = 0;

    @Column(name = "termos_versao", length = 40)
    private String termosVersao;
    @Column(name = "privacidade_versao", length = 40)
    private String privacidadeVersao;
    @Column(name = "aceite_termos_em")
    private LocalDateTime aceiteTermosEm;

    private LocalDateTime criadoEm = LocalDateTime.now();

    /** Protege também criações internas que não passam pelo cadastro HTTP. */
    @PrePersist
    private void aplicarPadroesDeSeguranca() {
        if (role == null) {
            role = com.josenetoo_dev.veiculos_api.enums.Role.USER;
        }
        if (status == null) {
            status = com.josenetoo_dev.veiculos_api.enums.StatusUsuario.PENDING_CONTACT_VERIFICATION;
        }
    }
}
