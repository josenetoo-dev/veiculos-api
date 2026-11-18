package com.josenetoo_dev.veiculos_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Mapeamento do registro técnico de OTP usado pela validação de esquema JPA.
 * As operações atômicas de inserção são feitas por JDBC para capturar colisões
 * UNIQUE sem comprometer a transação Hibernate da solicitação.
 */
@Entity
@Table(name = "email_code_reservation")
public class EmailCodeReservation {
    @Id
    @Column(name = "code_hash", length = 64, nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected EmailCodeReservation() { }
}
