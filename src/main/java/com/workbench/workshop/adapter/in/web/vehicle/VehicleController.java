package com.workbench.workshop.adapter.in.web.vehicle;

import com.workbench.workshop.adapter.in.web.vehicle.VehicleDtos.CreateVehicleRequest;
import com.workbench.workshop.adapter.in.web.vehicle.VehicleDtos.UpdateVehicleRequest;
import com.workbench.workshop.adapter.in.web.vehicle.VehicleDtos.VehicleResponse;
import com.workbench.workshop.adapter.in.web.RequesterResolver;
import com.workbench.workshop.application.vehicle.CreateVehicleUseCase;
import com.workbench.workshop.application.vehicle.DeactivateVehicleUseCase;
import com.workbench.workshop.application.vehicle.FindVehicleByIdUseCase;
import com.workbench.workshop.application.vehicle.FindVehicleByPlateUseCase;
import com.workbench.workshop.application.vehicle.ListVehiclesByClientUseCase;
import com.workbench.workshop.application.vehicle.UpdateVehicleUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Vehicles", description = "Cadastro de veículos")
public class VehicleController {

    private final CreateVehicleUseCase createVehicle;
    private final FindVehicleByIdUseCase findVehicleById;
    private final FindVehicleByPlateUseCase findVehicleByPlate;
    private final ListVehiclesByClientUseCase listVehiclesByClient;
    private final UpdateVehicleUseCase updateVehicle;
    private final DeactivateVehicleUseCase deactivateVehicle;

    @PostMapping
    @Operation(summary = "Criar veículo")
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody CreateVehicleRequest request) {
        var vehicle = createVehicle.execute(request.clientId(), request.plate(), request.brand(),
                request.model(), request.year(), request.color(), request.type());
        log.info("Vehicle created: id={}", vehicle.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(VehicleResponse.from(vehicle));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar veículo por ID")
    public ResponseEntity<VehicleResponse> findById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(VehicleResponse.from(findVehicleById.execute(id, RequesterResolver.from(authentication))));
    }

    @GetMapping("/plate/{plate}")
    @Operation(summary = "Buscar veículo por placa")
    public ResponseEntity<VehicleResponse> findByPlate(@PathVariable String plate, Authentication authentication) {
        return ResponseEntity.ok(VehicleResponse.from(findVehicleByPlate.execute(plate, RequesterResolver.from(authentication))));
    }

    @GetMapping("/client/{clientId}")
    @Operation(summary = "Listar veículos de um cliente")
    public ResponseEntity<List<VehicleResponse>> findByClient(@PathVariable Long clientId, Authentication authentication) {
        return ResponseEntity.ok(listVehiclesByClient.execute(clientId, RequesterResolver.from(authentication)).stream().map(VehicleResponse::from).toList());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar veículo")
    public ResponseEntity<VehicleResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateVehicleRequest request) {
        var vehicle = updateVehicle.execute(id, request.plate(), request.brand(), request.model(),
                request.year(), request.color(), request.type());
        log.info("Vehicle updated: id={}", id);
        return ResponseEntity.ok(VehicleResponse.from(vehicle));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar veículo")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        deactivateVehicle.execute(id);
        log.info("Vehicle deactivated: id={}", id);
        return ResponseEntity.noContent().build();
    }
}
