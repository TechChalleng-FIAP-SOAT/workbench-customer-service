package com.workbench.workshop.application.vehicle;

import com.workbench.workshop.application.vehicle.port.out.VehicleRepository;
import com.workbench.workshop.domain.vehicle.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Desativa o veículo (soft delete). */
@Service
@RequiredArgsConstructor
public class DeactivateVehicleUseCase {

    private final VehicleRepository vehicleRepository;

    @Transactional
    public void execute(Long id) {
        Vehicle vehicle = vehicleRepository.getById(id);
        vehicle.deactivate();
        vehicleRepository.save(vehicle);
    }
}
