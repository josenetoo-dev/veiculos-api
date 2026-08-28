package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
      boolean existsByEmail(String email);
      boolean existsByEmailAndIdNot(String email, Long id);

      Optional<Usuario> findByEmail(String email);

      Page<Usuario> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}
