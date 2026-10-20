package com.workbench.workshop.infrastructure.persistence.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleJpaRepository extends JpaRepository<VehicleEntity, Long> {
    Optional<VehicleEntity> findByPlate(String plate);
    List<VehicleEntity> findByClient_IdAndActiveTrue(Long clientId);
    boolean existsByPlate(String plate);
}
