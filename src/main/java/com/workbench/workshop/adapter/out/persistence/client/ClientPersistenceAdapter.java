package com.workbench.workshop.adapter.out.persistence.client;

import com.workbench.workshop.application.client.port.out.ClientRepository;
import com.workbench.workshop.domain.client.Client;
import com.workbench.workshop.infrastructure.persistence.client.ClientEntity;
import com.workbench.workshop.infrastructure.persistence.client.ClientJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ClientPersistenceAdapter implements ClientRepository {

    private final ClientJpaRepository jpaRepository;

    @Override
    public Client save(Client client) {
        ClientEntity entity = (client.getId() != null)
                ? jpaRepository.findById(client.getId()).orElseGet(ClientEntity::new)
                : new ClientEntity();
        entity.setName(client.getName());
        entity.setDocument(client.getDocument().getValue());
        entity.setEmail(client.getEmail());
        entity.setPhone(client.getPhone());
        entity.setActive(client.isActive());
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Client> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Client> findByDocument(String normalizedDocument) {
        return jpaRepository.findByDocument(normalizedDocument).map(this::toDomain);
    }

    @Override
    public List<Client> findAllActive() {
        return jpaRepository.findByActiveTrue().stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByDocument(String normalizedDocument) {
        return jpaRepository.existsByDocument(normalizedDocument);
    }

    private Client toDomain(ClientEntity e) {
        return Client.reconstitute(e.getId(), e.getName(), e.getDocument(), e.getEmail(),
                e.getPhone(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
