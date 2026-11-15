package com.workbench.workshop.application.vehicle;

import com.workbench.workshop.application.Requester;
import com.workbench.workshop.application.client.port.out.ClientRepository;
import com.workbench.workshop.application.vehicle.port.out.VehicleRepository;
import com.workbench.workshop.domain.vehicle.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Veículos de um cliente; um token de cliente só lista os próprios. */
@Service
@RequiredArgsConstructor
public class ListVehiclesByClientUseCase {

    private final VehicleRepository vehicleRepository;
    private final ClientRepository clientRepository;

    @Transactional(readOnly = true)
    public List<Vehicle> execute(Long clientId, Requester requester) {
        requester.requireAccessTo(clientRepository.getById(clientId));
        return vehicleRepository.findAllByClientId(clientId);
    }
}
