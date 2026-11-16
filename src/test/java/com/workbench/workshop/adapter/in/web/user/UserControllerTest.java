package com.workbench.workshop.adapter.in.web.user;

import com.workbench.workshop.application.user.AuthenticateUserUseCase;
import com.workbench.workshop.application.user.BadCredentialsException;
import com.workbench.workshop.application.user.SignUpUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserControllerTest {

    private SignUpUseCase signUp;
    private AuthenticateUserUseCase authenticateUser;
    private UserController controller;

    @BeforeEach
    void setUp() {
        signUp = mock(SignUpUseCase.class);
        authenticateUser = mock(AuthenticateUserUseCase.class);
        controller = new UserController(signUp, authenticateUser);
    }

    @Test
    void signUpReturns201WithBody() {
        when(signUp.execute("a@x.com", "segredo123", Set.of("ADMIN")))
                .thenReturn(new SignUpUseCase.Result(1L, "a@x.com", Set.of("ADMIN")));

        var res = controller.signUp(new UserDtos.SignUpRequest("a@x.com", "segredo123", Set.of("ADMIN")));

        assertEquals(201, res.getStatusCode().value());
        assertEquals("a@x.com", res.getBody().email());
        assertTrue(res.getBody().roles().contains("ADMIN"));
    }

    @Test
    void validateReturns200WithRoles() {
        when(authenticateUser.execute("a@x.com", "segredo123"))
                .thenReturn(new AuthenticateUserUseCase.Result("a@x.com", Set.of("TECHNICIAN")));

        var res = controller.validate(new UserDtos.AuthValidateRequest("a@x.com", "segredo123"));

        assertEquals(200, res.getStatusCode().value());
        assertEquals("a@x.com", res.getBody().email());
        assertTrue(res.getBody().roles().contains("TECHNICIAN"));
    }

    @Test
    void validatePropagatesBadCredentials() {
        when(authenticateUser.execute(any(), any()))
                .thenThrow(new BadCredentialsException());
        assertThrows(BadCredentialsException.class,
                () -> controller.validate(new UserDtos.AuthValidateRequest("a@x.com", "errada")));
    }
}
