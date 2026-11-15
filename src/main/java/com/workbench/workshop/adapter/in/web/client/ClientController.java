package com.workbench.workshop.adapter.in.web.client;

import com.workbench.workshop.adapter.in.web.client.ClientDtos.ClientResponse;
import com.workbench.workshop.adapter.in.web.client.ClientDtos.CreateClientRequest;
import com.workbench.workshop.adapter.in.web.client.ClientDtos.UpdateClientRequest;
import com.workbench.workshop.adapter.in.web.RequesterResolver;
import com.workbench.workshop.application.client.CreateClientUseCase;
import com.workbench.workshop.application.client.DeactivateClientUseCase;
import com.workbench.workshop.application.client.FindClientByDocumentUseCase;
import com.workbench.workshop.application.client.FindClientByIdUseCase;
import com.workbench.workshop.application.client.ListClientsUseCase;
import com.workbench.workshop.application.client.UpdateClientUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Clients", description = "Cadastro de clientes (CPF/CNPJ)")
public class ClientController {

    private final CreateClientUseCase createClient;
    private final ListClientsUseCase listClients;
    private final FindClientByIdUseCase findClientById;
    private final FindClientByDocumentUseCase findClientByDocument;
    private final UpdateClientUseCase updateClient;
    private final DeactivateClientUseCase deactivateClient;

    @PostMapping
    @Operation(summary = "Criar cliente")
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody CreateClientRequest request) {
        var client = createClient.execute(request.name(), request.document(), request.email(), request.phone());
        log.info("Client created: id={}", client.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ClientResponse.from(client));
    }

    @GetMapping
    @Operation(summary = "Listar clientes ativos")
    public ResponseEntity<List<ClientResponse>> findAll() {
        return ResponseEntity.ok(listClients.execute().stream().map(ClientResponse::from).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID")
    public ResponseEntity<ClientResponse> findById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ClientResponse.from(findClientById.execute(id, RequesterResolver.from(authentication))));
    }

    @GetMapping("/document/{document}")
    @Operation(summary = "Buscar cliente por CPF/CNPJ")
    public ResponseEntity<ClientResponse> findByDocument(@PathVariable String document, Authentication authentication) {
        return ResponseEntity.ok(ClientResponse.from(findClientByDocument.execute(document, RequesterResolver.from(authentication))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar cliente")
    public ResponseEntity<ClientResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateClientRequest request) {
        var client = updateClient.execute(id, request.name(), request.email(), request.phone());
        log.info("Client updated: id={}", id);
        return ResponseEntity.ok(ClientResponse.from(client));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar cliente")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        deactivateClient.execute(id);
        log.info("Client deactivated: id={}", id);
        return ResponseEntity.noContent().build();
    }
}
