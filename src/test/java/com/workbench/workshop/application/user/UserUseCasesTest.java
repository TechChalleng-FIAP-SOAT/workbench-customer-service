package com.workbench.workshop.application.user;

import com.workbench.workshop.application.user.port.out.UserRepository;
import com.workbench.workshop.domain.user.Role;
import com.workbench.workshop.domain.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserUseCasesTest {

    private UserRepository repository;
    private PasswordEncoder encoder;
    private UserUseCases useCases;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        encoder = new BCryptPasswordEncoder();
        useCases = new UserUseCases(repository, encoder);
        when(repository.save(any())).thenAnswer(i -> {
            User u = i.getArgument(0);
            return User.reconstitute(1L, u.getEmail(), u.getPasswordHash(), u.getRoles(),
                    u.isActive(), null, null);
        });
    }

    @Test
    void signUpHashesPasswordAndPersists() {
        when(repository.existsByEmail("staff@x.com")).thenReturn(false);

        var result = useCases.signUp("Staff@X.com", "segredo123", Set.of("ADMIN"));

        assertEquals("staff@x.com", result.email());
        assertTrue(result.roles().contains("ADMIN"));
        // a senha persistida deve ser um hash BCrypt, nunca o texto puro
        verify(repository).save(argThat(u -> !"segredo123".equals(u.getPasswordHash())
                && encoder.matches("segredo123", u.getPasswordHash())));
    }

    @Test
    void signUpRejectsDuplicateEmail() {
        when(repository.existsByEmail("dup@x.com")).thenReturn(true);
        assertThrows(IllegalArgumentException.class,
                () -> useCases.signUp("dup@x.com", "segredo123", Set.of("TECHNICIAN")));
    }

    @Test
    void signUpRejectsShortPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> useCases.signUp("x@x.com", "123", Set.of("ADMIN")));
    }

    @Test
    void signUpDefaultsToTechnicianWhenNoRoles() {
        when(repository.existsByEmail(anyString())).thenReturn(false);
        var result = useCases.signUp("t@x.com", "segredo123", null);
        assertEquals(Set.of("TECHNICIAN"), result.roles());
    }

    @Test
    void authenticateReturnsRolesWhenCredentialsMatch() {
        User stored = User.reconstitute(1L, "a@x.com", encoder.encode("segredo123"),
                Set.of(Role.ADMIN), true, null, null);
        when(repository.findByEmail("a@x.com")).thenReturn(Optional.of(stored));

        var result = useCases.authenticate("A@X.com", "segredo123");

        assertEquals("a@x.com", result.email());
        assertTrue(result.roles().contains("ADMIN"));
    }

    @Test
    void authenticateFailsOnWrongPassword() {
        User stored = User.reconstitute(1L, "a@x.com", encoder.encode("segredo123"),
                Set.of(Role.ADMIN), true, null, null);
        when(repository.findByEmail("a@x.com")).thenReturn(Optional.of(stored));
        assertThrows(UserUseCases.BadCredentialsException.class,
                () -> useCases.authenticate("a@x.com", "errada"));
    }

    @Test
    void authenticateFailsWhenUserMissing() {
        when(repository.findByEmail(anyString())).thenReturn(Optional.empty());
        assertThrows(UserUseCases.BadCredentialsException.class,
                () -> useCases.authenticate("ghost@x.com", "x"));
    }
}
