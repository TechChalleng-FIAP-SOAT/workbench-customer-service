package com.workbench.workshop.infrastructure.persistence.client;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClientJpaRepository extends JpaRepository<ClientEntity, Long> {
    Optional<ClientEntity> findByDocument(String document);
    List<ClientEntity> findByActiveTrue();
    boolean existsByDocument(String document);
}
