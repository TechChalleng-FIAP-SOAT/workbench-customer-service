package com.workbench.workshop.application.client;

import com.workbench.workshop.application.client.port.out.ClientRepository;
import com.workbench.workshop.domain.client.Client;
import com.workbench.workshop.domain.client.ClientNotFoundException;
import com.workbench.workshop.domain.client.Document;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Casos de uso de cliente (CRUD). As regras de negócio vêm do agregado {@link Client}
 * e dos value objects de documento; aqui ficam unicidade e orquestração de persistência.
 */
@Service
@RequiredArgsConstructor
public class ClientUseCases {

    private final ClientRepository clientRepository;

    @Transactional
    public Client create(String name, String rawDocument, String email, String phone) {
        // Normaliza o documento (só dígitos) para casar com o formato guardado no banco.
        String normalized = Document.of(rawDocument).getValue();
        if (clientRepository.existsByDocument(normalized)) {
            throw new IllegalStateException("Já existe cliente com o documento informado.");
        }
        return clientRepository.save(Client.create(name, rawDocument, email, phone));
    }

    @Transactional(readOnly = true)
    public Client findById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("Cliente não encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public Client findByDocument(String rawDocument) {
        String normalized = Document.of(rawDocument).getValue();
        return clientRepository.findByDocument(normalized)
                .orElseThrow(() -> new ClientNotFoundException("Cliente não encontrado para o documento informado."));
    }

    @Transactional(readOnly = true)
    public List<Client> findAll() {
        return clientRepository.findAllActive();
    }

    @Transactional
    public Client update(Long id, String name, String email, String phone) {
        Client client = findById(id);
        client.update(name, email, phone);
        return clientRepository.save(client);
    }

    @Transactional
    public void deactivate(Long id) {
        Client client = findById(id);
        client.deactivate();
        clientRepository.save(client);
    }
}
