package com.workbench.workshop.domain.vehicle;

/** Veículo não encontrado (mapeada para HTTP 404). */
public class VehicleNotFoundException extends RuntimeException {
    public VehicleNotFoundException(String message) {
        super(message);
    }
}
