package com.workbench.workshop.adapter.in.web.client;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.workbench.workshop.domain.client.Client;
import com.workbench.workshop.infrastructure.config.UpperCase;
import com.workbench.workshop.infrastructure.config.UpperCaseDeserializer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/** DTOs de entrada/saída do cliente. */
public final class ClientDtos {

    private ClientDtos() {}

    public record CreateClientRequest(
            @NotBlank @UpperCase @JsonDeserialize(using = UpperCaseDeserializer.class) String name,
            @NotBlank String document,
            @Email String email,
            String phone) {}

    public record UpdateClientRequest(
            @NotBlank @UpperCase @JsonDeserialize(using = UpperCaseDeserializer.class) String name,
            @Email String email,
            String phone) {}

    public record ClientResponse(Long id, String name, String document, String email, String phone,
                                 boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        public static ClientResponse from(Client c) {
            return new ClientResponse(c.getId(), c.getName(), c.getDocument().getValue(),
                    c.getEmail(), c.getPhone(), c.isActive(), c.getCreatedAt(), c.getUpdatedAt());
        }
    }
}
