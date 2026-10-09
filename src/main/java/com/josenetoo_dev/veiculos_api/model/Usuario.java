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

    private LocalDateTime criadoEm = LocalDateTime.now();
}
