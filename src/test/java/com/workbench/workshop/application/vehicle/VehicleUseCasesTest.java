package com.workbench.workshop.application.vehicle;

import com.workbench.workshop.application.Requester;
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
    private CreateVehicleUseCase createVehicle;
    private FindVehicleByIdUseCase findVehicleById;
    private FindVehicleByPlateUseCase findVehicleByPlate;
    private ListVehiclesByClientUseCase listVehiclesByClient;
    private UpdateVehicleUseCase updateVehicle;
    private DeactivateVehicleUseCase deactivateVehicle;

    private static final Requester STAFF = Requester.staff("admin@workbench.dev");

    @BeforeEach
    void setUp() {
        vehicleRepository = mock(VehicleRepository.class);
        clientRepository = mock(ClientRepository.class);
        createVehicle = new CreateVehicleUseCase(vehicleRepository, clientRepository);
        findVehicleById = new FindVehicleByIdUseCase(vehicleRepository, clientRepository);
        findVehicleByPlate = new FindVehicleByPlateUseCase(vehicleRepository, clientRepository);
        listVehiclesByClient = new ListVehiclesByClientUseCase(vehicleRepository, clientRepository);
        updateVehicle = new UpdateVehicleUseCase(vehicleRepository);
        deactivateVehicle = new DeactivateVehicleUseCase(vehicleRepository);
        when(clientRepository.getById(any())).thenCallRealMethod();
        when(vehicleRepository.getById(any())).thenCallRealMethod();
        when(vehicleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private void clientExists() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(
                Client.reconstitute(1L, "X", "52998224725", null, null, true, null, null)));
    }

    @Test
    void clientCannotSeeAnotherClientsVehicle() {
        clientExists(); // dono: CPF 52998224725
        when(vehicleRepository.findByPlate("ABC1234")).thenReturn(Optional.of(
                Vehicle.reconstitute(1L, 1L, "ABC1234", "VW", "Gol", 2020, "PRATA", VehicleType.CAR, true, null, null)));

        assertThrows(SecurityException.class,
                () -> findVehicleByPlate.execute("ABC1234", new Requester("11144477735", true)));
        assertEquals(1L, findVehicleByPlate.execute("ABC1234", new Requester("52998224725", true)).getId());
    }

    @Test
    void createRequiresExistingClient() {
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ClientNotFoundException.class,
                () -> createVehicle.execute(1L, "ABC1234", "VW", "Gol", 2020, "PRATA", VehicleType.CAR));
    }

    @Test
    void createPersistsWhenPlateUnique() {
        clientExists();
        when(vehicleRepository.existsByPlate("ABC1234")).thenReturn(false);

        Vehicle v = createVehicle.execute(1L, "abc-1234", "VW", "Gol", 2020, "prata", VehicleType.CAR);

        assertEquals("ABC1234", v.getPlate().getValue());
        assertEquals("VW", v.getBrand());
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void createRejectsDuplicatePlate() {
        clientExists();
        when(vehicleRepository.existsByPlate("ABC1234")).thenReturn(true);
        assertThrows(IllegalStateException.class,
                () -> createVehicle.execute(1L, "ABC1234", "VW", "Gol", 2020, "PRATA", VehicleType.CAR));
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(vehicleRepository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(VehicleNotFoundException.class, () -> findVehicleById.execute(9L, STAFF));
    }

    @Test
    void findByPlateNormalizes() {
        when(vehicleRepository.findByPlate("ABC1234")).thenReturn(Optional.of(
                Vehicle.reconstitute(1L, 1L, "ABC1234", "VW", "Gol", 2020, "PRATA", VehicleType.CAR, true, null, null)));
        assertEquals(1L, findVehicleByPlate.execute("abc-1234", STAFF).getId());
    }

    @Test
    void updateRejectsPlateTakenByAnother() {
        clientExists();
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(
                Vehicle.reconstitute(1L, 1L, "ABC1234", "VW", "Gol", 2020, "PRATA", VehicleType.CAR, true, null, null)));
        when(vehicleRepository.existsByPlate("XYZ9876")).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> updateVehicle.execute(1L, "XYZ9876", "VW", "Gol", 2021, "PRETO", VehicleType.CAR));
    }

    @Test
    void findByClientValidatesClientThenLists() {
        clientExists();
        when(vehicleRepository.findAllByClientId(1L)).thenReturn(List.of());
        assertNotNull(listVehiclesByClient.execute(1L, STAFF));
    }

    @Test
    void deactivateSoftDeletes() {
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(
                Vehicle.reconstitute(1L, 1L, "ABC1234", "VW", "Gol", 2020, "PRATA", VehicleType.CAR, true, null, null)));
        deactivateVehicle.execute(1L);
        verify(vehicleRepository).save(argThat(v -> !v.isActive()));
    }
}
