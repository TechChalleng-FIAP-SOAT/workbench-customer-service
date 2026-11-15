package com.workbench.workshop.application.vehicle;

import com.workbench.workshop.application.vehicle.port.out.VehicleRepository;
import com.workbench.workshop.domain.vehicle.LicensePlate;
import com.workbench.workshop.domain.vehicle.Vehicle;
import com.workbench.workshop.domain.vehicle.VehicleType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Atualiza os dados do veículo; a nova placa continua única. */
@Service
@RequiredArgsConstructor
public class UpdateVehicleUseCase {

    private final VehicleRepository vehicleRepository;

    @Transactional
    public Vehicle execute(Long id, String plate, String brand, String model,
                           int year, String color, VehicleType type) {
        Vehicle vehicle = vehicleRepository.getById(id);
        String newPlate = new LicensePlate(plate).getValue();
        if (!newPlate.equals(vehicle.getPlate().getValue()) && vehicleRepository.existsByPlate(newPlate)) {
            throw new IllegalStateException("Já existe veículo com a placa informada.");
        }
        vehicle.update(plate, brand, model, year, color, type);
        return vehicleRepository.save(vehicle);
    }
}
