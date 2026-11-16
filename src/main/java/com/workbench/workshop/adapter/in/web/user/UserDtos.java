package com.workbench.workshop.adapter.in.web.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

/** DTOs de entrada/saída de usuários staff. */
public final class UserDtos {

    private UserDtos() {}

    public record SignUpRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            Set<String> roles) {}

    public record SignUpResponse(Long id, String email, Set<String> roles) {}

    public record AuthValidateRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record AuthValidateResponse(String email, Set<String> roles) {}
}
