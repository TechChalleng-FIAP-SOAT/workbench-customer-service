package com.workbench.workshop.application.client.port.out;

import com.workbench.workshop.domain.client.Client;

import java.util.List;
import java.util.Optional;

public interface ClientRepository {
    Client save(Client client);
    Optional<Client> findById(Long id);
    Optional<Client> findByDocument(String normalizedDocument);
    List<Client> findAllActive();
    boolean existsByDocument(String normalizedDocument);
}
