package com.workbench.workshop.application.vehicle;

import com.workbench.workshop.application.Requester;
import com.workbench.workshop.domain.vehicle.LicensePlate;
import com.workbench.workshop.domain.vehicle.VehicleNotFoundException;
import com.workbench.workshop.application.client.port.out.ClientRepository;
import com.workbench.workshop.application.vehicle.port.out.VehicleRepository;
import com.workbench.workshop.domain.vehicle.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Veículo pela placa, com a mesma regra de dono. */
@Service
@RequiredArgsConstructor
public class FindVehicleByPlateUseCase {

    private final VehicleRepository vehicleRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public Vehicle execute(String plate, Requester requester) {
        String normalizedPlate = new LicensePlate(plate).getValue();
        Vehicle vehicle = vehicleRepository.findByPlate(normalizedPlate)
                .orElseThrow(() -> new VehicleNotFoundException("Veículo não encontrado para a placa informada."));
        requireOwner(vehicle.getClientId(), requester);
        return vehicle;
    }

    // Só um token de cliente precisa conferir o dono do veículo.
    private void requireOwner(Long clientId, Requester requester) {
        if (requester.client()) {
            requester.requireAccessTo(clientRepository.getById(clientId));
        }
    }
}
