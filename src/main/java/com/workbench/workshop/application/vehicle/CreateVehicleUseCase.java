package com.workbench.workshop.application.vehicle;

import com.workbench.workshop.domain.vehicle.LicensePlate;
import com.workbench.workshop.domain.vehicle.VehicleType;
import com.workbench.workshop.application.client.port.out.ClientRepository;
import com.workbench.workshop.application.vehicle.port.out.VehicleRepository;
import com.workbench.workshop.domain.vehicle.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastra veículo de um cliente existente; a placa é única. */
@Service
@RequiredArgsConstructor
public class CreateVehicleUseCase {

    private final VehicleRepository vehicleRepository;
    private final ClientRepository clientRepository;

    @Transactional
    public Vehicle execute(Long clientId, String plate, String brand, String model,
                           int year, String color, VehicleType type) {
        clientRepository.getById(clientId); // todo veículo exige um cliente existente
        String normalizedPlate = new LicensePlate(plate).getValue();
        if (vehicleRepository.existsByPlate(normalizedPlate)) {
            throw new IllegalStateException("Já existe veículo com a placa informada.");
        }
        return vehicleRepository.save(Vehicle.create(clientId, plate, brand, model, year, color, type));
    }
}
