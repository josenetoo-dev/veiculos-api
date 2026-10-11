package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
      @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
      @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.id = :id")
      Optional<Usuario> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

      boolean existsByEmail(String email);
      boolean existsByEmailAndIdNot(String email, Long id);

      Optional<Usuario> findByEmail(String email);

      Page<Usuario> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}
