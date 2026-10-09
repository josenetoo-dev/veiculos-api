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

    private LocalDateTime criadoEm = LocalDateTime.now();
}
