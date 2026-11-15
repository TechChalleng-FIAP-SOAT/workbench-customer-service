package com.workbench.workshop.application.client;

import com.workbench.workshop.application.Requester;
import com.workbench.workshop.application.client.port.out.ClientRepository;
import com.workbench.workshop.domain.client.Client;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cliente pelo id; um token de cliente só lê o próprio cadastro (403 caso contrário). */
@Service
@RequiredArgsConstructor
public class FindClientByIdUseCase {

    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public Client execute(Long id, Requester requester) {
        Client client = clientRepository.getById(id);
        requester.requireAccessTo(client);
        return client;
    }
}
