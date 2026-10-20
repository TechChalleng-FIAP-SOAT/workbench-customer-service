package com.workbench.workshop.application.vehicle;

import com.workbench.workshop.application.client.port.out.ClientRepository;
import com.workbench.workshop.application.vehicle.port.out.VehicleRepository;
import com.workbench.workshop.domain.client.Client;
import com.workbench.workshop.domain.client.ClientNotFoundException;
import com.workbench.workshop.domain.vehicle.Vehicle;
import com.workbench.workshop.domain.vehicle.VehicleNotFoundException;
import com.workbench.workshop.domain.vehicle.VehicleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VehicleUseCasesTest {

    private VehicleRepository vehicleRepository;
    private ClientRepository clientRepository;
    private VehicleUseCases useCases;

    @BeforeEach
    void setUp() {
        vehicleRepository = mock(VehicleRepository.class);
        clientRepository = mock(ClientRepository.class);
        useCases = new VehicleUseCases(vehicleRepository, clientRepository);
        when(vehicleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private void clientExists() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(
                Client.reconstitute(1L, "X", "52998224725", null, null, true, null, null)));
    }

    @Test
    void createRequiresExistingClient() {
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ClientNotFoundException.class,
                () -> useCases.create(1L, "ABC1234", "VW", "Gol", 2020, "PRATA", VehicleType.CAR));
    }

    @Test
    void createPersistsWhenPlateUnique() {
        clientExists();
        when(vehicleRepository.existsByPlate("ABC1234")).thenReturn(false);

        Vehicle v = useCases.create(1L, "abc-1234", "VW", "Gol", 2020, "prata", VehicleType.CAR);

        assertEquals("ABC1234", v.getPlate().getValue());
        assertEquals("VW", v.getBrand());
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void createRejectsDuplicatePlate() {
        clientExists();
        when(vehicleRepository.existsByPlate("ABC1234")).thenReturn(true);
        assertThrows(IllegalStateException.class,
                () -> useCases.create(1L, "ABC1234", "VW", "Gol", 2020, "PRATA", VehicleType.CAR));
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(vehicleRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(VehicleNotFoundException.class, () -> useCases.findById(9L));
    }

    @Test
    void findByPlateNormalizes() {
        when(vehicleRepository.findByPlate("ABC1234")).thenReturn(Optional.of(
                Vehicle.reconstitute(1L, 1L, "ABC1234", "VW", "Gol", 2020, "PRATA", VehicleType.CAR, true, null, null)));
        assertEquals(1L, useCases.findByPlate("abc-1234").getId());
    }

    @Test
    void updateRejectsPlateTakenByAnother() {
        clientExists();
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(
                Vehicle.reconstitute(1L, 1L, "ABC1234", "VW", "Gol", 2020, "PRATA", VehicleType.CAR, true, null, null)));
        when(vehicleRepository.existsByPlate("XYZ9876")).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> useCases.update(1L, "XYZ9876", "VW", "Gol", 2021, "PRETO", VehicleType.CAR));
    }

    @Test
    void findByClientValidatesClientThenLists() {
        clientExists();
        when(vehicleRepository.findAllByClientId(1L)).thenReturn(List.of());
        assertNotNull(useCases.findByClient(1L));
    }

    @Test
    void deactivateSoftDeletes() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(
                Vehicle.reconstitute(1L, 1L, "ABC1234", "VW", "Gol", 2020, "PRATA", VehicleType.CAR, true, null, null)));
        useCases.deactivate(1L);
        verify(vehicleRepository).save(argThat(v -> !v.isActive()));
    }
}
