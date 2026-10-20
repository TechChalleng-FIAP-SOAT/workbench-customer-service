package com.workbench.workshop.adapter.out.persistence.vehicle;

import com.workbench.workshop.application.vehicle.port.out.VehicleRepository;
import com.workbench.workshop.domain.vehicle.Vehicle;
import com.workbench.workshop.infrastructure.persistence.client.ClientEntity;
import com.workbench.workshop.infrastructure.persistence.client.ClientJpaRepository;
import com.workbench.workshop.infrastructure.persistence.vehicle.VehicleEntity;
import com.workbench.workshop.infrastructure.persistence.vehicle.VehicleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VehiclePersistenceAdapter implements VehicleRepository {

    private final VehicleJpaRepository vehicleJpaRepository;
    private final ClientJpaRepository clientJpaRepository;

    @Override
    public Vehicle save(Vehicle vehicle) {
        ClientEntity owner = clientJpaRepository.findById(vehicle.getClientId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cliente inexistente para o veículo: " + vehicle.getClientId()));

        VehicleEntity entity = (vehicle.getId() != null)
                ? vehicleJpaRepository.findById(vehicle.getId()).orElseGet(VehicleEntity::new)
                : new VehicleEntity();
        entity.setClient(owner);
        entity.setPlate(vehicle.getPlate().getValue());
        entity.setBrand(vehicle.getBrand());
        entity.setModel(vehicle.getModel());
        entity.setYear(vehicle.getYear());
        entity.setColor(vehicle.getColor());
        entity.setType(vehicle.getType());
        entity.setActive(vehicle.isActive());
        return toDomain(vehicleJpaRepository.save(entity));
    }

    @Override
    public Optional<Vehicle> findById(Long id) {
        return vehicleJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Vehicle> findByPlate(String normalizedPlate) {
        return vehicleJpaRepository.findByPlate(normalizedPlate).map(this::toDomain);
    }

    @Override
    public List<Vehicle> findAllByClientId(Long clientId) {
        return vehicleJpaRepository.findByClient_IdAndActiveTrue(clientId).stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByPlate(String normalizedPlate) {
        return vehicleJpaRepository.existsByPlate(normalizedPlate);
    }

    private Vehicle toDomain(VehicleEntity e) {
        return Vehicle.reconstitute(e.getId(), e.getClient().getId(), e.getPlate(),
                e.getBrand(), e.getModel(), e.getYear(), e.getColor(), e.getType(),
                e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
