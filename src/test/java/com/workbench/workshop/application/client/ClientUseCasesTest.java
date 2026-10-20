package com.workbench.workshop.application.client;

import com.workbench.workshop.application.client.port.out.ClientRepository;
import com.workbench.workshop.domain.client.Client;
import com.workbench.workshop.domain.client.ClientNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientUseCasesTest {

    private ClientRepository repository;
    private ClientUseCases useCases;

    private static final String CPF = "52998224725";

    @BeforeEach
    void setUp() {
        repository = mock(ClientRepository.class);
        useCases = new ClientUseCases(repository);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void createPersistsWhenDocumentIsUnique() {
        when(repository.existsByDocument(CPF)).thenReturn(false);

        Client c = useCases.create("João Silva", "529.982.247-25", "j@x.com", "1199");

        assertEquals("JOÃO SILVA", c.getName());
        assertEquals(CPF, c.getDocument().getValue());
        verify(repository).save(any(Client.class));
    }

    @Test
    void createRejectsDuplicateDocument() {
        when(repository.existsByDocument(CPF)).thenReturn(true);
        assertThrows(IllegalStateException.class,
                () -> useCases.create("João", "52998224725", null, null));
        verify(repository, never()).save(any());
    }

    @Test
    void createRejectsInvalidDocument() {
        assertThrows(IllegalArgumentException.class,
                () -> useCases.create("João", "123", null, null));
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(ClientNotFoundException.class, () -> useCases.findById(9L));
    }

    @Test
    void findByDocumentNormalizesAndReturns() {
        when(repository.findByDocument(CPF)).thenReturn(Optional.of(
                Client.reconstitute(1L, "JOÃO", CPF, null, null, true, null, null)));
        assertEquals(1L, useCases.findByDocument("529.982.247-25").getId());
    }

    @Test
    void updateChangesMutableFields() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                Client.reconstitute(1L, "OLD", CPF, "old@x.com", "1", true, null, null)));

        Client c = useCases.update(1L, "Novo Nome", "new@x.com", "2");

        assertEquals("Novo Nome", c.getName());
        assertEquals("new@x.com", c.getEmail());
    }

    @Test
    void deactivateSoftDeletes() {
        when(repository.findById(1L)).thenReturn(Optional.of(
                Client.reconstitute(1L, "X", CPF, null, null, true, null, null)));
        useCases.deactivate(1L);
        verify(repository).save(argThat(c -> !c.isActive()));
    }

    @Test
    void findAllDelegates() {
        when(repository.findAllActive()).thenReturn(List.of());
        assertNotNull(useCases.findAll());
    }
}
