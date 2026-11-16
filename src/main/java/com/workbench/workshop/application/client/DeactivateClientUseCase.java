package com.workbench.workshop.application.client;

import com.workbench.workshop.application.client.port.out.ClientRepository;
import com.workbench.workshop.domain.client.Client;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Desativa o cliente (soft delete). */
@Service
@RequiredArgsConstructor
public class DeactivateClientUseCase {

    private final ClientRepository clientRepository;

    @Transactional
    public void execute(Long id) {
        Client client = clientRepository.getById(id);
        client.deactivate();
        clientRepository.save(client);
    }
}
