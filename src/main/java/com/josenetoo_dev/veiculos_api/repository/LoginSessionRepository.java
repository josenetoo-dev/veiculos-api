package com.josenetoo_dev.veiculos_api.repository;

import com.josenetoo_dev.veiculos_api.model.LoginSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LoginSessionRepository extends JpaRepository<LoginSession, Long> {
    Optional<LoginSession> findByTokenHash(String tokenHash);
}
