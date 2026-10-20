package com.workbench.workshop.domain.client;

/** Cliente não encontrado (mapeada para HTTP 404). */
public class ClientNotFoundException extends RuntimeException {
    public ClientNotFoundException(String message) {
        super(message);
    }
}
