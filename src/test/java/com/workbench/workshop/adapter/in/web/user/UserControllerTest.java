package com.workbench.workshop.adapter.in.web.user;

import com.workbench.workshop.application.user.UserUseCases;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserControllerTest {

    private UserUseCases useCases;
    private UserController controller;

    @BeforeEach
    void setUp() {
        useCases = mock(UserUseCases.class);
        controller = new UserController(useCases);
    }

    @Test
    void signUpReturns201WithBody() {
        when(useCases.signUp("a@x.com", "segredo123", Set.of("ADMIN")))
                .thenReturn(new UserUseCases.SignUpResult(1L, "a@x.com", Set.of("ADMIN")));

        var res = controller.signUp(new UserController.SignUpRequest("a@x.com", "segredo123", Set.of("ADMIN")));

        assertEquals(201, res.getStatusCode().value());
        assertEquals("a@x.com", res.getBody().email());
        assertTrue(res.getBody().roles().contains("ADMIN"));
    }

    @Test
    void validateReturns200WithRoles() {
        when(useCases.authenticate("a@x.com", "segredo123"))
                .thenReturn(new UserUseCases.AuthResult("a@x.com", Set.of("TECHNICIAN")));

        var res = controller.validate(new UserController.AuthValidateRequest("a@x.com", "segredo123"));

        assertEquals(200, res.getStatusCode().value());
        assertEquals("a@x.com", res.getBody().email());
        assertTrue(res.getBody().roles().contains("TECHNICIAN"));
    }

    @Test
    void validatePropagatesBadCredentials() {
        when(useCases.authenticate(any(), any()))
                .thenThrow(new UserUseCases.BadCredentialsException());
        assertThrows(UserUseCases.BadCredentialsException.class,
                () -> controller.validate(new UserController.AuthValidateRequest("a@x.com", "errada")));
    }
}
