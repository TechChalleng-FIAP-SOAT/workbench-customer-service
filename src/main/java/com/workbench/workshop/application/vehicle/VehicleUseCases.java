package com.workbench.workshop.application.vehicle;

import com.workbench.workshop.application.client.port.out.ClientRepository;
import com.workbench.workshop.application.vehicle.port.out.VehicleRepository;
import com.workbench.workshop.domain.client.ClientNotFoundException;
import com.workbench.workshop.domain.vehicle.LicensePlate;
import com.workbench.workshop.domain.vehicle.Vehicle;
import com.workbench.workshop.domain.vehicle.VehicleNotFoundException;
import com.workbench.workshop.domain.vehicle.VehicleType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Casos de uso de veículo (CRUD). Garante o vínculo dono-veículo: todo veículo
 * exige um cliente existente (regra do ADR/monólito).
 */
@Service
@RequiredArgsConstructor
public class VehicleUseCases {

    private final VehicleRepository vehicleRepository;
    private final ClientRepository clientRepository;

    @Transactional
    public Vehicle create(Long clientId, String plate, String brand, String model,
                          int year, String color, VehicleType type) {
        requireClient(clientId);
        String normalizedPlate = new LicensePlate(plate).getValue();
        if (vehicleRepository.existsByPlate(normalizedPlate)) {
            throw new IllegalStateException("Já existe veículo com a placa informada.");
        }
        return vehicleRepository.save(Vehicle.create(clientId, plate, brand, model, year, color, type));
    }

    @Transactional(readOnly = true)
    public Vehicle findById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException("Veículo não encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public Vehicle findByPlate(String plate) {
        String normalizedPlate = new LicensePlate(plate).getValue();
        return vehicleRepository.findByPlate(normalizedPlate)
                .orElseThrow(() -> new VehicleNotFoundException("Veículo não encontrado para a placa informada."));
    }

    @Transactional(readOnly = true)
    public List<Vehicle> findByClient(Long clientId) {
        requireClient(clientId);
        return vehicleRepository.findAllByClientId(clientId);
    }

    @Transactional
    public Vehicle update(Long id, String plate, String brand, String model,
                          int year, String color, VehicleType type) {
        Vehicle vehicle = findById(id);
        String newPlate = new LicensePlate(plate).getValue();
        if (!newPlate.equals(vehicle.getPlate().getValue()) && vehicleRepository.existsByPlate(newPlate)) {
            throw new IllegalStateException("Já existe veículo com a placa informada.");
        }
        vehicle.update(plate, brand, model, year, color, type);
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public void deactivate(Long id) {
        Vehicle vehicle = findById(id);
        vehicle.deactivate();
        vehicleRepository.save(vehicle);
    }

    private void requireClient(Long clientId) {
        if (clientRepository.findById(clientId).isEmpty()) {
            throw new ClientNotFoundException("Cliente não encontrado: " + clientId);
        }
    }
}
