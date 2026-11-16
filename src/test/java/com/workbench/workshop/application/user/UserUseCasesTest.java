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
    private SignUpUseCase signUp;
    private AuthenticateUserUseCase authenticateUser;
    private LoginAttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        encoder = new BCryptPasswordEncoder();
        limiter = new LoginAttemptLimiter();
        signUp = new SignUpUseCase(repository, encoder);
        authenticateUser = new AuthenticateUserUseCase(repository, encoder, limiter);
        when(repository.save(any())).thenAnswer(i -> {
            User u = i.getArgument(0);
            return User.reconstitute(1L, u.getEmail(), u.getPasswordHash(), u.getRoles(),
                    u.isActive(), null, null);
        });
    }

    @Test
    void signUpHashesPasswordAndPersists() {
        when(repository.existsByEmail("staff@x.com")).thenReturn(false);

        var result = signUp.execute("Staff@X.com", "segredo123", Set.of("ADMIN"));

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
                () -> signUp.execute("dup@x.com", "segredo123", Set.of("TECHNICIAN")));
    }

    @Test
    void signUpRejectsShortPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> signUp.execute("x@x.com", "123", Set.of("ADMIN")));
    }

    @Test
    void signUpDefaultsToTechnicianWhenNoRoles() {
        when(repository.existsByEmail(anyString())).thenReturn(false);
        var result = signUp.execute("t@x.com", "segredo123", null);
        assertEquals(Set.of("TECHNICIAN"), result.roles());
    }

    @Test
    void authenticateReturnsRolesWhenCredentialsMatch() {
        User stored = User.reconstitute(1L, "a@x.com", encoder.encode("segredo123"),
                Set.of(Role.ADMIN), true, null, null);
        when(repository.findByEmail("a@x.com")).thenReturn(Optional.of(stored));

        var result = authenticateUser.execute("A@X.com", "segredo123");

        assertEquals("a@x.com", result.email());
        assertTrue(result.roles().contains("ADMIN"));
    }

    @Test
    void authenticateFailsOnWrongPassword() {
        User stored = User.reconstitute(1L, "a@x.com", encoder.encode("segredo123"),
                Set.of(Role.ADMIN), true, null, null);
        when(repository.findByEmail("a@x.com")).thenReturn(Optional.of(stored));
        assertThrows(BadCredentialsException.class,
                () -> authenticateUser.execute("a@x.com", "errada"));
    }

    @Test
    void authenticateFailsWhenUserMissing() {
        when(repository.findByEmail(anyString())).thenReturn(Optional.empty());
        assertThrows(BadCredentialsException.class,
                () -> authenticateUser.execute("ghost@x.com", "x"));
    }

    @Test
    void lockedEmailIsRefusedEvenWithRightPassword() {
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES; i++) {
            limiter.recordFailure("a@x.com");
        }
        // Bloqueado: nem consulta o banco, mesma resposta de credencial inválida.
        assertThrows(BadCredentialsException.class, () -> authenticateUser.execute("a@x.com", "segredo123"));
        verify(repository, never()).findByEmail("a@x.com");
    }
}
