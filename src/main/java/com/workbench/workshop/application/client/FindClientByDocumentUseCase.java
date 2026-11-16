package com.workbench.workshop.application.client;

import com.workbench.workshop.application.Requester;
import com.workbench.workshop.domain.client.ClientNotFoundException;
import com.workbench.workshop.domain.client.Document;
import com.workbench.workshop.application.client.port.out.ClientRepository;
import com.workbench.workshop.domain.client.Client;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cliente pelo CPF/CNPJ, com a mesma regra de dono. */
@Service
@RequiredArgsConstructor
public class FindClientByDocumentUseCase {

    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public Client execute(String rawDocument, Requester requester) {
        String normalized = Document.of(rawDocument).getValue();
        Client client = clientRepository.findByDocument(normalized)
                .orElseThrow(() -> new ClientNotFoundException("Cliente não encontrado para o documento informado."));
        requester.requireAccessTo(client);
        return client;
    }
}
