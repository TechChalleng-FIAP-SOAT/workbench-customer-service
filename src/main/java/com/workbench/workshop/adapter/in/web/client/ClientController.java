package com.workbench.workshop.adapter.in.web.client;

import com.workbench.workshop.adapter.in.web.client.ClientDtos.ClientResponse;
import com.workbench.workshop.adapter.in.web.client.ClientDtos.CreateClientRequest;
import com.workbench.workshop.adapter.in.web.client.ClientDtos.UpdateClientRequest;
import com.workbench.workshop.application.client.ClientUseCases;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Clients", description = "Cadastro de clientes (CPF/CNPJ)")
public class ClientController {

    private final ClientUseCases useCases;

    @PostMapping
    @Operation(summary = "Criar cliente")
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody CreateClientRequest request) {
        var client = useCases.create(request.name(), request.document(), request.email(), request.phone());
        log.info("Client created: id={}", client.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ClientResponse.from(client));
    }

    @GetMapping
    @Operation(summary = "Listar clientes ativos")
    public ResponseEntity<List<ClientResponse>> findAll() {
        return ResponseEntity.ok(useCases.findAll().stream().map(ClientResponse::from).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID")
    public ResponseEntity<ClientResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ClientResponse.from(useCases.findById(id)));
    }

    @GetMapping("/document/{document}")
    @Operation(summary = "Buscar cliente por CPF/CNPJ")
    public ResponseEntity<ClientResponse> findByDocument(@PathVariable String document) {
        return ResponseEntity.ok(ClientResponse.from(useCases.findByDocument(document)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar cliente")
    public ResponseEntity<ClientResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateClientRequest request) {
        var client = useCases.update(id, request.name(), request.email(), request.phone());
        log.info("Client updated: id={}", id);
        return ResponseEntity.ok(ClientResponse.from(client));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar cliente")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        useCases.deactivate(id);
        log.info("Client deactivated: id={}", id);
        return ResponseEntity.noContent().build();
    }
}
