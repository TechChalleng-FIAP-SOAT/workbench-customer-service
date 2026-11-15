package com.workbench.workshop.application.vehicle.port.out;

import com.workbench.workshop.domain.vehicle.Vehicle;
import com.workbench.workshop.domain.vehicle.VehicleNotFoundException;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository {
    Vehicle save(Vehicle vehicle);
    Optional<Vehicle> findById(Long id);
    Optional<Vehicle> findByPlate(String normalizedPlate);
    List<Vehicle> findAllByClientId(Long clientId);
    boolean existsByPlate(String normalizedPlate);

    /** Veículo pelo id ou {@link VehicleNotFoundException} (404). */
    default Vehicle getById(Long id) {
        return findById(id).orElseThrow(() -> new VehicleNotFoundException("Veículo não encontrado: " + id));
    }
}
