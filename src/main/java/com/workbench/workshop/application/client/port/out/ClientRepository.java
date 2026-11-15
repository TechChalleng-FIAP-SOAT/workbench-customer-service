package com.workbench.workshop.application.client.port.out;

import com.workbench.workshop.domain.client.Client;
import com.workbench.workshop.domain.client.ClientNotFoundException;

import java.util.List;
import java.util.Optional;

public interface ClientRepository {
    Client save(Client client);
    Optional<Client> findById(Long id);
    Optional<Client> findByDocument(String normalizedDocument);
    List<Client> findAllActive();
    boolean existsByDocument(String normalizedDocument);

    /** Cliente pelo id ou {@link ClientNotFoundException} (404). */
    default Client getById(Long id) {
        return findById(id).orElseThrow(() -> new ClientNotFoundException("Cliente não encontrado: " + id));
    }
}
