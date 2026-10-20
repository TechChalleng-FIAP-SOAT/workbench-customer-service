package com.workbench.workshop.adapter.in.web.vehicle;

import com.workbench.workshop.adapter.in.web.vehicle.VehicleDtos.CreateVehicleRequest;
import com.workbench.workshop.adapter.in.web.vehicle.VehicleDtos.UpdateVehicleRequest;
import com.workbench.workshop.adapter.in.web.vehicle.VehicleDtos.VehicleResponse;
import com.workbench.workshop.application.vehicle.VehicleUseCases;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Vehicles", description = "Cadastro de veículos")
public class VehicleController {

    private final VehicleUseCases useCases;

    @PostMapping
    @Operation(summary = "Criar veículo")
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody CreateVehicleRequest request) {
        var vehicle = useCases.create(request.clientId(), request.plate(), request.brand(),
                request.model(), request.year(), request.color(), request.type());
        log.info("Vehicle created: id={}", vehicle.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(VehicleResponse.from(vehicle));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar veículo por ID")
    public ResponseEntity<VehicleResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(VehicleResponse.from(useCases.findById(id)));
    }

    @GetMapping("/plate/{plate}")
    @Operation(summary = "Buscar veículo por placa")
    public ResponseEntity<VehicleResponse> findByPlate(@PathVariable String plate) {
        return ResponseEntity.ok(VehicleResponse.from(useCases.findByPlate(plate)));
    }

    @GetMapping("/client/{clientId}")
    @Operation(summary = "Listar veículos de um cliente")
    public ResponseEntity<List<VehicleResponse>> findByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(useCases.findByClient(clientId).stream().map(VehicleResponse::from).toList());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar veículo")
    public ResponseEntity<VehicleResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody UpdateVehicleRequest request) {
        var vehicle = useCases.update(id, request.plate(), request.brand(), request.model(),
                request.year(), request.color(), request.type());
        log.info("Vehicle updated: id={}", id);
        return ResponseEntity.ok(VehicleResponse.from(vehicle));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar veículo")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        useCases.deactivate(id);
        log.info("Vehicle deactivated: id={}", id);
        return ResponseEntity.noContent().build();
    }
}
