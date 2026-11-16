package com.workbench.workshop.infrastructure.config;

import com.workbench.workshop.application.user.port.out.UserRepository;
import com.workbench.workshop.domain.user.Role;
import com.workbench.workshop.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminPasswordInitializerTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private UserRepository repository;
    private AdminPasswordInitializer initializer;
    private User admin;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        initializer = new AdminPasswordInitializer(repository, encoder);
        admin = User.create(AdminPasswordInitializer.SEEDED_ADMIN,
                encoder.encode(AdminPasswordInitializer.SEEDED_PASSWORD), Set.of(Role.ADMIN));
        when(repository.findByEmail(AdminPasswordInitializer.SEEDED_ADMIN)).thenReturn(Optional.of(admin));
    }

    @Test
    void replacesDefaultPasswordWhenConfigured() {
        ReflectionTestUtils.setField(initializer, "initialPassword", "SenhaForte#2026");

        initializer.run(null);

        assertTrue(encoder.matches("SenhaForte#2026", admin.getPasswordHash()));
        verify(repository).save(admin);
    }

    @Test
    void keepsDefaultAndOnlyWarnsWhenNotConfigured() {
        ReflectionTestUtils.setField(initializer, "initialPassword", "");

        initializer.run(null);

        verify(repository, never()).save(any());
    }

    @Test
    void refusesToStartWithTheDefaultPasswordOutsideLocal() {
        ReflectionTestUtils.setField(initializer, "initialPasswordRequired", true);

        ReflectionTestUtils.setField(initializer, "initialPassword", "");
        assertThrows(IllegalStateException.class, () -> initializer.run(null));

        ReflectionTestUtils.setField(initializer, "initialPassword", "curta");
        assertThrows(IllegalStateException.class, () -> initializer.run(null));
        verify(repository, never()).save(any());
    }

    @Test
    void requiredDoesNotBlockAnAlreadyChangedPassword() {
        ReflectionTestUtils.setField(initializer, "initialPasswordRequired", true);
        ReflectionTestUtils.setField(initializer, "initialPassword", "");
        admin.changePassword(encoder.encode("JaTrocada#1"));

        assertDoesNotThrow(() -> initializer.run(null));
    }

    @Test
    void neverOverwritesAnAlreadyChangedPassword() {
        admin.changePassword(encoder.encode("JaTrocada#1"));
        ReflectionTestUtils.setField(initializer, "initialPassword", "SenhaForte#2026");

        initializer.run(null);

        assertFalse(encoder.matches("SenhaForte#2026", admin.getPasswordHash()));
        verify(repository, never()).save(any());
    }
}
