package com.workbench.workshop.adapter.in.web.vehicle;

import com.workbench.workshop.domain.vehicle.Vehicle;
import com.workbench.workshop.domain.vehicle.VehicleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

/** DTOs de entrada/saída do veículo. */
public final class VehicleDtos {

    private VehicleDtos() {}

    public record CreateVehicleRequest(
            @NotNull @Positive Long clientId,
            @NotBlank String plate,
            @NotBlank String brand,
            @NotBlank String model,
            @Min(1886) int year,
            String color,
            @NotNull VehicleType type) {}

    public record UpdateVehicleRequest(
            @NotBlank String plate,
            @NotBlank String brand,
            @NotBlank String model,
            @Min(1886) int year,
            String color,
            @NotNull VehicleType type) {}

    public record VehicleResponse(Long id, Long clientId, String plate, String brand, String model,
                                  int year, String color, VehicleType type, boolean active,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        public static VehicleResponse from(Vehicle v) {
            return new VehicleResponse(v.getId(), v.getClientId(), v.getPlate().formatted(),
                    v.getBrand(), v.getModel(), v.getYear(), v.getColor(), v.getType(),
                    v.isActive(), v.getCreatedAt(), v.getUpdatedAt());
        }
    }
}
