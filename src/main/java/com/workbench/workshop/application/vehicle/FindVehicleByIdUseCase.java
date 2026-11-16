package com.workbench.workshop.application.vehicle;

import com.workbench.workshop.application.Requester;
import com.workbench.workshop.application.client.port.out.ClientRepository;
import com.workbench.workshop.application.vehicle.port.out.VehicleRepository;
import com.workbench.workshop.domain.vehicle.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Veículo pelo id; um token de cliente só lê os próprios veículos (403 caso contrário). */
@Service
@RequiredArgsConstructor
public class FindVehicleByIdUseCase {

    private final VehicleRepository vehicleRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public Vehicle execute(Long id, Requester requester) {
        Vehicle vehicle = vehicleRepository.getById(id);
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
