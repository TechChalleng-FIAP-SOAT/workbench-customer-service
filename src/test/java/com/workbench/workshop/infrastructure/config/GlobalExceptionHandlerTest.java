package com.workbench.workshop.infrastructure.config;

import com.workbench.workshop.domain.client.ClientNotFoundException;
import com.workbench.workshop.domain.vehicle.VehicleNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFoundMapsTo404() {
        assertEquals(404, handler.handleNotFound(new ClientNotFoundException("x")).getStatusCode().value());
        assertEquals(404, handler.handleNotFound(new VehicleNotFoundException("x")).getStatusCode().value());
    }

    @Test
    void illegalArgumentMapsTo400() {
        assertEquals(400, handler.handleIllegalArgument(new IllegalArgumentException("bad")).getStatusCode().value());
    }

    @Test
    void illegalStateMapsTo409() {
        assertEquals(409, handler.handleIllegalState(new IllegalStateException("dup")).getStatusCode().value());
    }

    @Test
    void dataIntegrityDuplicateMapsTo409() {
        var ex = new DataIntegrityViolationException("duplicate key value violates unique constraint");
        assertEquals(409, handler.handleDataIntegrity(ex).getStatusCode().value());
    }

    @Test
    void dataIntegrityGenericMapsTo409() {
        var ex = new DataIntegrityViolationException("some other constraint");
        assertEquals(409, handler.handleDataIntegrity(ex).getStatusCode().value());
    }

    @Test
    void runtimeMapsTo422() {
        assertEquals(422, handler.handleRuntime(new RuntimeException("boom")).getStatusCode().value());
    }
}
