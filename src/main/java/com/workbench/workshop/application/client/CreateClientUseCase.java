package com.workbench.workshop.application.client;

import com.workbench.workshop.domain.client.Document;
import com.workbench.workshop.application.client.port.out.ClientRepository;
import com.workbench.workshop.domain.client.Client;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastra cliente; o documento (CPF/CNPJ) é único. */
@Service
@RequiredArgsConstructor
public class CreateClientUseCase {

    private final ClientRepository clientRepository;

    @Transactional
    public Client execute(String name, String rawDocument, String email, String phone) {
        // Normaliza o documento (só dígitos) para casar com o formato guardado no banco.
        String normalized = Document.of(rawDocument).getValue();
        if (clientRepository.existsByDocument(normalized)) {
            throw new IllegalStateException("Já existe cliente com o documento informado.");
        }
        return clientRepository.save(Client.create(name, rawDocument, email, phone));
    }
}
